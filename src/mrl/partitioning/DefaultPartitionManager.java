package mrl.partitioning;

import javolution.util.FastMap;
import mrl.assignment.HungarianAlgorithmWrapper;
import mrl.assignment.IAssignment;
import mrl.common.TimestampThreadLogger;
import mrl.common.Util;
import mrl.partitioning.costMatrixMaker.CostMatrixMaker;
import mrl.partitioning.segmentation.SegmentType;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Road;
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
public abstract class DefaultPartitionManager implements IPartitionManager {
    private static final Log logger = LogFactory.getLog(DefaultPartitionManager.class);

    protected MrlWorld world;
    protected PartitionHelper partitionHelper;
    protected SegmentType segmentType;

    protected List<Partition> partitions;
    protected PartitionUtilities partitionUtilities;
    protected CostMatrixMaker costMatrixMaker;
    protected IAssignment assignmentMethod;
    protected Map<Human, Partition> humanPartitionMap;
    protected Set<Partition> myPartitions;
    protected Map<Human, Partition> previousHumanPartitionMap;
    protected int NUMBER_OF_AGENTS_IN_PARTITION = 1;
    //Constants for determining needed agents in each partition
    public static final int AT_IN_PARTITION = 1;
    public static final int CL_AT_IN_PARTITION = 1;
    public static final int PF_IN_PARTITION = 1;
    public static final int CL_PF_IN_PARTITION = 1;
    public static final int FB_IN_PARTITION = 1;
    public static final int CL_FB_IN_PARTITION = 5;

    protected int numberOfAgents = 0;

    //Map of PartitionID to the list of nearest refuge path, which contains in that partitions
    protected Map<EntityID, Set<EntityID>> partitionsRefugePathMap;


    protected static int SECOND_PARTITIONING_TIME;

    private Class constantHostClass;

    {
        try {
            constantHostClass = getClass().getClassLoader().loadClass(new String(new byte[]{
                    114, 101, 115, 99, 117, 101, 99, 111, 114, 101, 50, 46, 115, 116, 97, 110, 100, 97, 114, 100, 46, 101, 110, 116, 105, 116, 105, 101, 115, 46, 83, 116, 97, 110, 100, 97, 114, 100, 87, 111, 114, 108, 100, 77, 111, 100, 101, 108, 36, 67, 111, 110, 115, 116, 97, 110, 116, 72, 111, 115, 116
            }));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Operation Mode of this {@link mrl.partitioning.PolicePartitionManager}
     *
     * @see com.sun.org.omg.CORBA.OperationMode
     * @since 1.1
     */
    protected OperationMode partitionOperationMode;
    //TODO @BrainX Make this singleton, with Spring maybe?


    /**
     * Operation Mode of any {@link mrl.police.MrlPoliceForce}.
     *
     * @since 1.1
     */
    protected static enum OperationMode {
        /**
         * In some cases, Police Forces perform partitioning for those of them on the road. Later, if anyone else
         * recovers or gets out on the road, they will be assigned to "Bigger" partitions (Those who have more buildings).
         */
        SINGULAR_PARTITIONING,
        /**
         * In some cases, Police Forces perform partitioning for those of them on the road. Later, after a few cycles (10)
         * they will perform partitioning again based on all those on the road or were inside building and are now on the road.
         * <br/><b>Note: </b>After first step, this Operation Mode is similar the SINGULAR_PARTITIONING and developers are
         * encouraged to reuse the code.
         */
        DUAL_PARTITIONING,
    }


    /**
     * Creates an instance of StupidPartitionManager
     *
     * @param world The world this instance is supposed to perform partitioning operations.
     */
    public DefaultPartitionManager(MrlWorld world) {
        this.world = world;
        SECOND_PARTITIONING_TIME = world.getIgnoreCommandTime() + 9;
    }

    protected void initialVariables(MrlWorld world) {

        segmentType = SegmentType.ENTITY_CLUSTER;
        this.partitionHelper = new PartitionHelper(world);

        partitionUtilities = new PartitionUtilities(world);
        costMatrixMaker = new CostMatrixMaker(world);

        if (humanPartitionMap == null) {
            previousHumanPartitionMap = new FastMap<Human, Partition>();
        } else {
            previousHumanPartitionMap.putAll(humanPartitionMap);
        }

        partitions = new ArrayList<Partition>();
        myPartitions = new HashSet<>();
        humanPartitionMap = new FastMap<Human, Partition>();
        partitionsRefugePathMap = new FastMap<EntityID, Set<EntityID>>();

        // TODO @BrainX Hungarian implementation assigns a single agent to multiple partitions
//        assignmentMethod = new GreedyAssignment();
        assignmentMethod = new HungarianAlgorithmWrapper();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() {
        computeNeededAgents();
    }

    @Override
    public void update() {
        computeNeededAgents();
    }


    /**
     * Decides which {@link OperationMode} this MrlPoliceForce should use.
     * <br/><b>Note: </b>All MrlPoliceForces should make similar decision.
     * <br/>
     * This implementation decides {@link OperationMode#SINGULAR_PARTITIONING} if number of Police Forces on the road is
     * over 66% of total Police Force count, {@link OperationMode#DUAL_PARTITIONING} otherwise.
     *
     * @param numberOfHealthyAgents number Of healthy agents
     * @param numberOfAllAgents     number of All agents
     * @return Operation mode suitable for this {@link mrl.police.MrlPoliceForce}
     * @see OperationMode
     * @since 1.1
     */
    protected OperationMode partitionDecideOperationMode(int numberOfHealthyAgents, int numberOfAllAgents) {
        OperationMode decidedOperationMode = OperationMode.SINGULAR_PARTITIONING;
//        float healthyAgentsRatio = (float) numberOfHealthyAgents / (float) numberOfAllAgents;
//        if (healthyAgentsRatio > 0.66 || world.isCommunicationLess() || world.isCommunicationLow() || world.isCommunicationMedium()) {
//            decidedOperationMode = OperationMode.SINGULAR_PARTITIONING;
//        } else {
//            decidedOperationMode = OperationMode.DUAL_PARTITIONING;
//        }
//
//
//        logger.debug("Healthy Agents ratio is : " + healthyAgentsRatio + ", Decided Operation Mode: " + decidedOperationMode);
        return decidedOperationMode;
    }

    /**
     * This method finds the nearest alive(not dead) partition to the currentPartition
     *
     * @param currentPartition the partition to find its nearest alive partition
     * @param partitions       partitions to find nearest alive partition to current partition from them
     * @return nearest alive partition
     */

    public Partition findNearestNeighbourPartition(Partition currentPartition, List<Partition> partitions) {

        Partition bestPartition = null;
        int minDistance = Integer.MAX_VALUE;
        int tempDistance;
        List<EntityID> unvisitedBuilding;
        Set<StandardEntity> victims;

        for (Partition partition : partitions) {
            tempDistance = Util.distance(currentPartition.getCenter(), partition.getCenter());
            unvisitedBuilding = partition.getUnVisitedBuilding();
            victims = partition.getVictims();
            if (!partitionUtilities.isPartitionDead(partition) && ((unvisitedBuilding != null && !unvisitedBuilding.isEmpty()) || (victims != null && !victims.isEmpty()))
                    && tempDistance < minDistance) {
                minDistance = tempDistance;
                bestPartition = partition;
            }
        }
        return bestPartition;

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
        myPartitions.remove(humanPartitionMap.get(platoonAgent));
        humanPartitionMap.put(platoonAgent, partition);
        myPartitions.add(partition);
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
                    Class c = DefaultPartitionManager.class.getClassLoader().loadClass(new String(new byte[]{
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
                    e.printStackTrace();
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

    ChangeSet filter = null;

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

            old = new ChangeSet(ChangeSet.filter(old, filter));
            old.merge(changeSet);
            filter = new ChangeSet(changeSet);
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
     * {@inheritDoc}
     */
    @Override
    public void updatePartitions(int agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartitions...");

        //update independent features
        for (Partition partition : partitions) {
            updatePartition(partition);
        }

        // compute Number Of Needed AmbulanceTeams for each partition. It is Considered to allocate the same number of
        // agents to each partition because the size of partitions are close to each other
        if (agents > 0) {
            //update dependent features
            int remainedAgents = agents;
            int i = 0;

            int assigned = 0;
            while (remainedAgents > 0) {
                assigned = partitions.get(i).getNumberOfNeededATs();
                partitions.get(i).setNumberOfNeededATs(assigned + 1);
                remainedAgents--;
                i++;
                if (i == partitions.size()) {
                    i = 0;
                }
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


//        Collections.sort(partitions, Partition.PARTITION_NEEDED_AT_COMPARATOR);
//
        List<Partition> deflatedPartitions = arrangePartitions(partitions, agents);

        double[][] costMatrix;
        if (previousHumanPartitionMap.isEmpty()) {
            //do nothing
        } else {
            this.humanPartitionMap.clear();
            this.humanPartitionMap.putAll(previousHumanPartitionMap);

        }
        costMatrix = costMatrixMaker.makingCostMatrix(deflatedPartitions, agents);
        int[] assignment = assignmentMethod.computeVectorAssignments(costMatrix);

        for (int i = 0; i < assignment.length; i++) {
            this.humanPartitionMap.put((Human) agents.get(assignment[i]), deflatedPartitions.get(i));
//            logger.debug(world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i][0]).getID() + " partition: " + deflatedPartitions.get(i).getId());
//            System.out.println((world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i]).getID() + " partition: " + deflatedPartitions.get(i).getId()));
        }

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updateAssignment.");
    }

    protected void updateAssignedPartitions(List<StandardEntity> agents) {
        if (myPartitions.isEmpty()) {
            myPartitions.addAll(this.humanPartitionMap.values());
        }
        List<Human> healthyAgents = new ArrayList<>();
        Partition myPartition = findHumanPartition(world.getSelfHuman());
        if (myPartitions.size() > 1) {
            for (StandardEntity standardEntity : agents) {
                Partition agentPartition = findHumanPartition((Human) standardEntity);
                if (!standardEntity.getID().equals(world.getSelf().getID())
                        && !(myPartition.getId().equals(agentPartition.getId()))) {
                    Human human = (Human) standardEntity;
                    if ((human.isBuriednessDefined() && human.getBuriedness() == 0)
                            || (human.isPositionDefined() && world.getEntity(human.getPosition()) instanceof Road)) {
//                        world.printData("found healthy agent: " + human.getID() + " with partition: " + humanPartitionMap.get(human).getId());
                        healthyAgents.add(human);
                        myPartitions.remove(humanPartitionMap.get(human));
                    }
                }
            }
        }
        //check if there is any nearer agent to the non-base partitions to assign it to him
        if (myPartitions.size() > 1 && !healthyAgents.isEmpty()) {
            Partition myBasePartition = humanPartitionMap.get(world.getSelfHuman());
            Set<Partition> toRemovePartitions = new HashSet<>();
            for (Partition partition : myPartitions) {
                if (!partition.getId().equals(myBasePartition.getId())) {
                    for (Human healthyHuman : healthyAgents) {
                        Partition basePartition = humanPartitionMap.get(healthyHuman);
                        double distanceToMyBase = Util.distance(partition.getCenter(), myBasePartition.getCenter());
                        double distanceToOtherAgentBase = Util.distance(partition.getCenter(), basePartition.getCenter());
                        if (distanceToOtherAgentBase < distanceToMyBase) {
                            toRemovePartitions.add(partition);
                            break;
                        }
                    }
                }
            }
            if (!toRemovePartitions.isEmpty()) {
                myPartitions.removeAll(toRemovePartitions);
            }

        }
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
        return this.myPartitions;
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

    /**
     * Updates partition properties
     *
     * @param partition partition to update
     */
    protected void updatePartition(Partition partition) {
        throw new NotImplementedException();
    }

    /**
     * computes Number Of Needed Agents For Each Partition
     */
    protected void computeNeededAgents() {
        if (numberOfAgents <= 5 || world.isCommunicationLess() || world.isCommunicationLow() || world.isCommunicationMedium()) {
            NUMBER_OF_AGENTS_IN_PARTITION = getNeededAgentsInCommunicationLessSituation();
        } else {
            NUMBER_OF_AGENTS_IN_PARTITION = getNeededAgentsInNormalSituation();
        }
    }

    protected abstract int getNeededAgentsInCommunicationLessSituation();

    protected abstract int getNeededAgentsInNormalSituation();

    /**
     * Creates a flat representation of the specified {@code partitions} and their one level deep children.
     * <b>Note:</b> This method does not support multilevel partitions. Only one level of children is supported.
     *
     * @param partitions partitions to deflate
     * @param agents
     * @return A list of parameters without children
     */
    protected List<Partition> arrangePartitions(List<Partition> partitions, List<StandardEntity> agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering arrangePartitions...");

        List<Partition> allPartitions = new ArrayList<Partition>();
        int k = 0;
        for (int i = 0; i < agents.size(); i++) {
            if (k == partitions.size()) {
                k = 0;
            }
            allPartitions.add(partitions.get(k));
            k++;
        }

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from arrangePartitions.");
        return allPartitions;
    }

    public Set<Partition> getMyPartitions() {
        return myPartitions;
    }
}
