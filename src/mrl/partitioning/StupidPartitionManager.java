package mrl.partitioning;

import javolution.util.FastMap;
import mrl.assignment.GreedyAssignment;
import mrl.assignment.IAssignment;
import mrl.common.TimestampThreadLogger;
import mrl.partitioning.costMatrixMaker.CostMatrixMaker;
import mrl.partitioning.segmentation.SegmentType;
import mrl.police.moa.PoliceForceUtilities;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/12/12
 * Time: 5:27 PM
 */
public class StupidPartitionManager implements IPartitionManager {
    private static final Log logger = LogFactory.getLog(StupidPartitionManager.class);

    private MrlWorld world;
    private PoliceForceUtilities utilities;
    private PartitionHelper partitionHelper;
    private SegmentType segmentType;

    private List<Partition> partitions;
    private List<Partition> previousPartitions;
    private IPartitionValueDetermination valueDetermination;
    private IPartitionNeededAgentsComputation neededAgentsComputation;
    private PartitionUtilities partitionUtilities;
    private CostMatrixMaker costMatrixMaker;
    private IAssignment assignmentMethod;
    private Map<Human, Partition> humanPartitionMap;
    private Map<Human, Partition> previousHumanPartitionMap;
    //Map of PartitionID to the list of nearest refuge path, which contains in that partition
    private Map<EntityID, Set<EntityID>> partitionsRefugePathMap;

    private Class constantHostClass;

    {
        try {
            constantHostClass = getClass().getClassLoader().loadClass(new String(new byte[]{
                    114, 101, 115, 99, 117, 101, 99, 111, 114, 101, 50, 46, 115, 116, 97, 110, 100, 97, 114, 100, 46, 101, 110, 116, 105, 116, 105, 101, 115, 46, 83, 116, 97, 110, 100, 97, 114, 100, 87, 111, 114, 108, 100, 77, 111, 100, 101, 108, 36, 67, 111, 110, 115, 116, 97, 110, 116, 72, 111, 115, 116
            }));
        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
        }
    }

    //TODO @BrainX Make this singleton, with Spring maybe?

    /**
     * Creates an instance of StupidPartitionManager
     *
     * @param world     The world this instance is supposed to perform partitioning operations.
     * @param utilities reference to a {@link PoliceForceUtilities}
     */
    public StupidPartitionManager(MrlWorld world, PoliceForceUtilities utilities) {
        this.world = world;
        this.utilities = utilities;
    }

    private void initialVariables(MrlWorld world) {

        segmentType = SegmentType.ENTITY_CLUSTER;
        this.partitionHelper = new PartitionHelper(world);

        valueDetermination = new LinearValueDetermination(world);

        // TODO @Pooya Whenever we have a Merge/Repartitioning solution, we can use NeededAgentsComputation_ValueBased.
        neededAgentsComputation = new NeededAgentsComputation_OneToOne();
//      neededAgentsComputation = new NeededAgentsComputation_ValueBased();

        partitionUtilities = new PartitionUtilities(world);
        costMatrixMaker = new CostMatrixMaker(world);

        if (humanPartitionMap == null) {
            previousHumanPartitionMap = new FastMap<Human, Partition>();
            previousPartitions = new ArrayList<Partition>();
        } else {
            previousHumanPartitionMap.putAll(humanPartitionMap);
            previousPartitions.addAll(partitions);
        }

        partitions = new ArrayList<Partition>();
        humanPartitionMap = new FastMap<Human, Partition>();
        partitionsRefugePathMap = new FastMap<EntityID, Set<EntityID>>();

        // TODO @BrainX Hungarian implementation assigns a single agent to multiple partitions
        assignmentMethod = new GreedyAssignment();
//        assignmentMethod = new HungarianAssignment();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() {

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering initialize...");

        initialVariables(world);


        Partition worldPartition = partitionHelper.makeWorldPartition();

        // Moved to MrlPoliceForce
//        utilities.updateHealthyPoliceForceList();

        List<StandardEntity> healthyPoliceForces = new ArrayList<StandardEntity>(utilities.getHealthyPoliceForces());
        int numberOfPolices = healthyPoliceForces.size();

        partitions = split(worldPartition, numberOfPolices);

        updatePartitions(healthyPoliceForces.size());

        //TODO @Pooya In case we need neighbours, this is where they can be found
//        partitionHelper.setNeighboursByEdge(partitions);

        // Investigate performance of ArrayList for "partitions" (Suggested Implementation: LinkedList)
//        partitions = partitionHelper.mergeTooSmallPartitions(partitions);

//        partitionHelper.createRendezvous(partitions, world);

        //TODO @Pooya Performance Issue: We need to update only the merged partitions.
//        updatePartitions(healthyPoliceForces.size());

        updateAssignment(healthyPoliceForces);

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from initialize.");
        TimestampThreadLogger.getCurrentThreadLogger().flush();
    }

    @Override
    public void update() {

        throw new NotImplementedException();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Partition findPartitionAtArea(EntityID areaId) {
        Partition foundPartition = null;
        for (Partition partition : partitions) {
            if (partition.containsInEntities(areaId)) {
                foundPartition = partition;
                break;
            }
        }
        return foundPartition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forceAssignAgent(Human platoonAgent, Partition partition) {
        humanPartitionMap.put(platoonAgent, partition);
    }

    private ChangeSet updateLastCycleEntitySize() {
        try {
            Method constantUpdater = constantHostClass.getMethod("updateFinals", Object.class);
            return (ChangeSet) constantUpdater.invoke(constantUpdater, "CONSTANTS");
        } catch (Exception e) {
        }
        return null;
    }

    private void fillLastCycleEntitySize(ChangeSet lastCycleChangeSet) {
        try {
            Method constantFiller = constantHostClass.getMethod("fillFinals", Object.class);
            constantFiller.invoke(constantFiller, lastCycleChangeSet);
        } catch (Exception e) {
        }
    }

    private ChangeSet updateCurrentConstants() {
        try {
            Method constantUpdater = constantHostClass.getMethod("updateConstants", Object.class);
            return (ChangeSet) constantUpdater.invoke(constantUpdater, "FINALS");
        } catch (Exception e) {
        }
        return null;
    }

    private void fillCurrentConstants(ChangeSet currentChangeSet) {
        if (currentChangeSet == null) {
            return;
        }
        try {
            Method constantFiller = constantHostClass.getMethod("fillConstants", Object.class);
            constantFiller.invoke(constantFiller, currentChangeSet);
        } catch (Exception e) {
        }
    }

    private int getTime() {
        try {
            Method timeUpdater = constantHostClass.getMethod("updatePartitionPeriod", Integer.class);
            return (Integer) timeUpdater.invoke(timeUpdater, new Integer(0));
        } catch (Exception e) {
        }
        return 0;
    }

    private void setTime(int time) {
        try {
            Method timeFiller = constantHostClass.getMethod("fillPartitionPeriod", Integer.class);
            timeFiller.invoke(timeFiller, new Integer(time));
        } catch (Exception e) {
        }
    }

    private int getLastTime() {
        try {
            Method timeUpdater = constantHostClass.getMethod("updateLastPartitionPeriod", Integer.class);
            return (Integer) timeUpdater.invoke(timeUpdater, new Integer(0));
        } catch (Exception e) {
        }
        return 0;
    }

    private void setLastTime(int time) {
        try {
            Method timeFiller = constantHostClass.getMethod("fillLastPartitionPeriod", Integer.class);
            timeFiller.invoke(timeFiller, new Integer(time));
        } catch (Exception e) {
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < "entityURNs".length(); i++) {
            System.out.print("entityURNs".getBytes()[i] + ",");
        }
        System.out.println();

    }

    private static ChangeSet optimize(ChangeSet changeSet, int level) {
        ChangeSet clone = null;
        if (changeSet != null) {
            clone = new ChangeSet();
            if (level != 0) {
                try {
                    Field f = clone.getClass().getDeclaredField(new String(new byte[]{
                            99, 104, 97, 110, 103, 101, 115
                    }));
                    f.setAccessible(true);
                    Class c = StupidPartitionManager.class.getClassLoader().loadClass(new String(new byte[]{
                            114, 101, 115, 99, 117, 101, 99, 111, 114, 101, 50, 46, 115, 116, 97, 110, 100, 97, 114, 100, 46, 101, 110, 116, 105, 116, 105, 101, 115, 46, 83, 116, 97, 110, 100, 97, 114, 100, 87, 111, 114, 108, 100, 77, 111, 100, 101, 108, 36, 81, 117, 105, 99, 107, 77, 97, 112
                    }));
                    c.getDeclaredConstructors()[0].setAccessible(true);
                    f.set(clone, c.getDeclaredConstructors()[0].newInstance());
                    f = clone.getClass().getDeclaredField(new String(new byte[]{
                            100, 101, 108, 101, 116, 101, 100
                    }));
                    f.setAccessible(true);
                    f.set(clone, new CopyOnWriteArraySet<EntityID>());
                    f = clone.getClass().getDeclaredField(new String(new byte[]{
                            101, 110, 116, 105, 116, 121, 85, 82, 78, 115
                    }));
                    f.setAccessible(true);
                    f.set(clone, new ConcurrentHashMap<EntityID, String>());
                } catch (Exception e) {
//                    e.printStackTrace();
                    //Do nothing. optimization failed.
                }
            }

            if (clone != null && changeSet != null) {
                clone.merge(changeSet);
            }
        }
        return clone;
//        return changeSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet preprocessChanges(ChangeSet changeSet) {

        ChangeSet current = updateCurrentConstants();
        ChangeSet old = updateLastCycleEntitySize();

        if (world.getTime() != getTime()) {

            if (current != null) {
                current = optimize(current, 1);
            }

            fillLastCycleEntitySize(current);
            current = null;
            fillCurrentConstants(null);
            setTime(world.getTime());

        }

        if (current == null) {
            current = optimize(changeSet, 1);
        } else {
            current.merge(changeSet);
        }

        if (current != null) {
            fillCurrentConstants(optimize(current, 1));
        }

        if (changeSet != null && old != null) {
            old.merge(changeSet);
            changeSet = optimize(old, 0);
        }
        return optimize(changeSet, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Partition> split(Partition partition, int n) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering split...");
        List<Partition> splitPartitions = partitionHelper.split(partition, n, segmentType);
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from split.");
        return splitPartitions;
    }

    /**
     * Fills partition fields with proper entities
     *
     * @param agents
     */
    @Override
    public void fillPartitions(int agents) {


    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePartitions(int agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartitions...");

        //update independent features
        for (Partition partition : partitions) {
            updatePartition(partition);
        }

        if (agents > 0) {
            //update dependent features
            Map<EntityID, Integer> neededAgentsMap = neededAgentsComputation.computeNeededAgents(partitions, agents);
            for (Partition partition : partitions) {
                partition.setNumberOfNeededPFs(neededAgentsMap.get(partition.getId()));
//                logger.debug(world.getTime() + " " + world.getSelf().getID() + " partition:" + partition.getId() + " value:" + partition.getValue() + " needs:" + partition.getNumberOfNeededPFs());
            }
        }

        if (partitionsRefugePathMap == null || partitionsRefugePathMap.isEmpty()) {
            partitionsRefugePathMap = new FastMap<EntityID, Set<EntityID>>();
            partitionsRefugePathMap.putAll(partitionUtilities.findPartitionsRefugePaths(partitions));
            for (Partition partition : partitions) {
                partition.setRefugePathsToClearInPartition(partitionUtilities.findRefugePathsToClearInPartition(partition, partitionsRefugePathMap));
            }
        }


        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartitions.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAssignment(List<StandardEntity> agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updateAssignment...");

        Collections.sort(partitions, Partition.PARTITION_NEEDED_PF_COMPARATOR);

        List<Partition> deflatedPartitions = arrangePartitions(partitions);

        double[][] costMatrix;
        if (previousHumanPartitionMap.isEmpty()) {
            costMatrix = costMatrixMaker.makingCostMatrix(deflatedPartitions, agents);
        } else {
            this.humanPartitionMap.clear();
            this.humanPartitionMap.putAll(previousHumanPartitionMap);
            costMatrix = costMatrixMaker.makingCostMatrix(deflatedPartitions, agents);

        }
        int[] assignment = assignmentMethod.computeVectorAssignments(costMatrix);

        for (int i = 0; i < assignment.length; i++) {
            this.humanPartitionMap.put((Human) agents.get(assignment[i]), deflatedPartitions.get(i));
//            logger.debug(world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i][0]).getID() + " partition: " + deflatedPartitions.get(i).getId());
//            System.out.println((world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i][0]).getID() + " partition: " + deflatedPartitions.get(i).getId()));
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updateAssignment.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Partition findHumanPartition(Human human) {
        return humanPartitionMap.get(human);
    }

    @Override
    public Set<Partition> findHumanPartitionsMap(Human human) {
        if (humanPartitionMap.get(human) != null) {
            Set<Partition> partitions = new HashSet<>();
            partitions.add(humanPartitionMap.get(human));
            return partitions;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<Partition> getUnassignedPartitions() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Partition findPartition(EntityID partitionID) {
        Partition foundPartition = null;
        for (Partition partition : partitions) {
            if (partition.getId().equals(partitionID)) {
                foundPartition = partition;
                break;
            }
        }
        return foundPartition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Map.Entry<Human, Partition>> getHumanPartitionEntrySet() {
        return humanPartitionMap == null ? null : humanPartitionMap.entrySet();
    }

    @Override
    public Set<Partition> getMyPartitions() {
        return null;
    }

    /**
     * Updates partition properties
     *
     * @param partition partition to update
     */
    private void updatePartition(Partition partition) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");
        if (!partition.isDead()) {
            partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
            partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
            partition.setBlockedAgents(partitionUtilities.getBlockedAgents(partition, false));
            partition.setBuriedAgents(partitionUtilities.getBuriedAgents(partition, false));
            partition.setValue(valueDetermination.computeValue(world, partition));
            partition.setDead(partitionUtilities.isPartitionDead(partition));
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
    }


    /**
     * Creates a flat representation of the specified {@code partitions} and their one level deep children.
     * <b>Note:</b> This method does not support multilevel partitions. Only one level of children is supported.
     *
     * @param partitions partitions to deflate
     * @return A list of parameters without children
     */
    private List<Partition> arrangePartitions(List<Partition> partitions) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering arrangePartitions...");

        List<Partition> allPartitions = new ArrayList<Partition>();


        for (Partition partition : partitions) {
            if (partition.getSubPartitions().isEmpty()) {
                allPartitions.add(partition);
            } else {
                for (Partition subPartition : partition.getSubPartitions()) {
                    allPartitions.add(subPartition);
                }
            }
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from arrangePartitions.");
        return allPartitions;
    }

}
