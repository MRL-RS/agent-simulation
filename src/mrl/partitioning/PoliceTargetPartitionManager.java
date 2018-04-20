package mrl.partitioning;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.assignment.HungarianAlgorithmWrapper;
import mrl.assignment.IAssignment;
import mrl.common.TimestampThreadLogger;
import mrl.partitioning.costMatrixMaker.CostMatrixMaker;
import mrl.partitioning.segmentation.EntityCluster;
import mrl.partitioning.segmentation.SegmentType;
import mrl.police.moa.PoliceForceUtilities;
import mrl.police.moa.Target;
import mrl.police.strategies.ITargetManager;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;


/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/9/12
 *         Time: 1:12 AM
 */
public class PoliceTargetPartitionManager implements IPartitionManager {

    private MrlWorld world;
    private PartitionHelper partitionHelper;
    private static final int NUMBER_OF_AGENTS_IN_PARTITION = 1;
    private List<Partition> partitions;
    private SegmentType segmentType;

    private IPartitionValueDetermination valueDetermination;
    private IPartitionNeededAgentsComputation neededAgentsComputation;
    private ITargetManager targetManager;
    private PoliceForceUtilities utilities;
    private CostMatrixMaker costMatrixMaker;
    private IAssignment assignmentMethod;
    private Map<Human, Partition> humanPartitionMap;
    private Map<Human, Set<Partition>> humanPartitionsMap;
    private List<Partition> unassignedPartitions;
    private Map<Human, Partition> previousHumanPartitionMap;
    private List<Partition> previousPartitions;
    private PartitionUtilities partitionUtilities;

    private boolean shouldExecute = true;


    /**
     * Creates an instance of PoliceTargetPartitionManager
     *
     * @param world         The world this instance is supposed to perform partitioning operations.
     * @param utilities
     * @param targetManager
     */
    public PoliceTargetPartitionManager(MrlWorld world, PoliceForceUtilities utilities, ITargetManager targetManager) {
        this.world = world;
        this.utilities = utilities;
        this.targetManager = targetManager;

        partitionUtilities = new PartitionUtilities(world);
        this.humanPartitionMap = new FastMap<Human, Partition>();
        this.humanPartitionsMap = new FastMap<Human, Set<Partition>>();
        this.unassignedPartitions = new ArrayList<Partition>();
//        this.neededAgentsComputation = new NeededAgentsComputation_OneToOne();
        this.neededAgentsComputation = new NeededAgentsComputation_ThresholdBased();
        this.valueDetermination = new LinearValueDetermination(world);
        this.costMatrixMaker = new CostMatrixMaker(world);
//        this.assignmentMethod = new GreedyAssignment();
        this.assignmentMethod = new HungarianAlgorithmWrapper();
    }

    @Override
    public void initialise() {

        this.partitionHelper = new PartitionHelper(world);
        this.partitions = new ArrayList<Partition>();
        this.segmentType = SegmentType.TARGET_CLUSTER;

        //A map of TargetPositionEntity to TargetEntity
        Map<StandardEntity, List<StandardEntity>> targetsMap = findTargetsInitialPositions();
        if (targetsMap == null || targetsMap.isEmpty()) {
            // do nothing
        } else {

            Partition worldTargetsPartition = new Partition(world, new EntityCluster(world, targetsMap.keySet()), targetsMap);

            List<StandardEntity> healthyPoliceForces = new ArrayList<StandardEntity>(utilities.getHealthyPoliceForces());
            if (healthyPoliceForces.isEmpty()) {
                healthyPoliceForces.add(world.getSelfHuman());
            }

            partitions = split(worldTargetsPartition, -1);

            updatePartitions(healthyPoliceForces.size());

            updateAssignment(healthyPoliceForces);

        }

    }

    /**
     * This method finds initial positions of police Force Targets. These targets consist of any FBs, any ATs, buried PFs,
     * and Refuges.
     * <p/>
     * <p> <b> note: </b> Positions might be repetitive </p>
     *
     * @return Map of StandardEntity Position of targets to list of StandardEntity of Targets
     */
    private Map<StandardEntity, List<StandardEntity>> findTargetsInitialPositions() {

        Map<StandardEntity, List<StandardEntity>> targetPositionsMap = new FastMap<StandardEntity, List<StandardEntity>>();
        List<StandardEntity> targetsInPosition;

        Map<EntityID, Target> targets = targetManager.getTargets(null);
        if (targets == null || targets.isEmpty()) {
            //do nothing
        } else {
            for (Target target : targets.values()) {

                targetsInPosition = targetPositionsMap.get(world.getEntity(target.getPositionID()));
                if (targetsInPosition == null) {
                    targetsInPosition = new ArrayList<StandardEntity>();
                }
                targetsInPosition.add(world.getEntity(target.getId()));
                targetPositionsMap.put(world.getEntity(target.getPositionID()), targetsInPosition);

            }

        }
        return targetPositionsMap;

    }

    @Override
    public void update() {

        List<StandardEntity> healthyPoliceForces = new ArrayList<StandardEntity>(utilities.getHealthyPoliceForces());

        updatePartitions(healthyPoliceForces.size());

        updateAssignment(healthyPoliceForces);

        MrlPersonalData.VIEWER_DATA.setPartitionsData(world.getSelfHuman().getID(), this.getPartitions(), humanPartitionsMap);
    }

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
        //To change body of implemented methods use File | Settings | File Templates.
    }

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

//        if (partitionsRefugePathMap == null || partitionsRefugePathMap.isEmpty()) {
//            partitionsRefugePathMap = new FastMap<EntityID, Set<EntityID>>();
//            partitionsRefugePathMap.putAll(targetManager.findPartitionsRefugePaths(partitions));
//            for (Partition partition : partitions) {
//                partition.setRefugePathsToClearInPartition(targetManager.findRefugePathsToClearInPartition(partition, partitionsRefugePathMap));
//            }
//        }


        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartitions.");
    }

    @Override
    public void updateAssignment(List<StandardEntity> agents) {

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updateAssignment...");

        Collections.sort(partitions, Partition.PARTITION_VALUE_COMPARATOR);

        List<Partition> deflatedPartitions = arrangePartitions(partitions, agents);

        double[][] costMatrix;

        if (humanPartitionMap != null && !humanPartitionMap.isEmpty()) {
            //updated assigned partitions tasks
            List<Partition> toRemovePartitions = new ArrayList<Partition>();
            Set<Partition> assignedPartitions;
            for (Human human : humanPartitionsMap.keySet()) {
                assignedPartitions = humanPartitionsMap.get(human);
                if (assignedPartitions != null) {
                    for (Partition partition : partitions) {
                        if (!deflatedPartitions.contains(partition)) {
                            toRemovePartitions.add(partition);
                            System.out.println(">>>> time:" + world.getTime() + " This Partition is no longer assigned to me. " + world.getSelf().getID() + " partition:" + partition.getId());
                        }
                    }
                    assignedPartitions.removeAll(toRemovePartitions);
                }

            }


        }
        if (shouldExecute) {
            shouldExecute = false;
            //do operations for partition assignments, specially for the first time

            costMatrix = costMatrixMaker.makingCostMatrix(deflatedPartitions, agents);

            int[] assignment = assignmentMethod.computeVectorAssignments(costMatrix);

            Set<Partition> assignedPartitions;
            humanPartitionsMap.clear();
            for (int i = 0; i < assignment.length; i++) {
                assignedPartitions = this.humanPartitionsMap.get((Human) agents.get(assignment[i]));
                if (assignedPartitions == null) {
                    assignedPartitions = new HashSet<>();
                }
                assignedPartitions.add(deflatedPartitions.get(i));
                this.humanPartitionsMap.put((Human) agents.get(assignment[i]), assignedPartitions);
//            logger.debug(world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i][0]).getID() + " partition: " + deflatedPartitions.get(i).getId());
//            System.out.println((world.getTime() + " self: " + world.getSelf().getID() + " agentID: " + agents.get(assignment[i][0]).getID() + " partition: " + deflatedPartitions.get(i).getId()));
            }

        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updateAssignment.");
    }


    /**
     * This method finds assigned partition of the agent.<br/>
     * <br/>
     * <b>Note: </b> In this type of Manager({@link PoliceTargetPartitionManager}), agents may have more than one partitions, so between assigned partitions </br>
     * the partition with highest value will be considered as agent's current partition.
     *
     * @param human Human for which this method finds the assigned partition
     * @return
     */
    @Override
    public Partition findHumanPartition(Human human) {

        Partition topPartition = null;
        double topValue = Double.MIN_VALUE;
        if (humanPartitionsMap.get(human) != null) {
            for (Partition partition : humanPartitionsMap.get(human)) {
                if (!partition.isDone() && partition.getValue() > topValue) {
                    topPartition = partition;
                    topValue = partition.getValue();
                }
            }
        }

        return topPartition;
    }

    @Override
    public Set<Partition> findHumanPartitionsMap(Human human) {
        return humanPartitionsMap.get(human);
    }

    @Override
    public List<Partition> getPartitions() {
        return partitions;
    }

    @Override
    public List<Partition> getUnassignedPartitions() {
        return unassignedPartitions;
    }

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

    @Override
    public Set<Map.Entry<Human, Partition>> getHumanPartitionEntrySet() {
        return humanPartitionMap == null ? null : humanPartitionMap.entrySet();
    }

    @Override
    public Set<Partition> getMyPartitions() {
        return null;
    }

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

    @Override
    public void forceAssignAgent(Human platoonAgent, Partition partition) {
        throw new NotImplementedException();
    }

    @Override
    public ChangeSet preprocessChanges(ChangeSet changeSet) {
        throw new NotImplementedException();
    }


    /**
     * Updates partition properties
     *
     * @param partition partition to update
     */
    private void updatePartition(Partition partition) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");

        partition.setBlockedAgents(partitionUtilities.getBlockedAgents(partition, true));
        partition.setBuriedAgents(partitionUtilities.getBuriedAgents(partition, true));
        partition.setValue(valueDetermination.computeValue(world, partition));

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
    }

    /**
     * This method finds some n more valuable partitions where n refers to number of agents
     *
     * @param partitions partitions to deflate
     * @param agents
     * @return A list of parameters without children
     */
    private List<Partition> arrangePartitions(List<Partition> partitions, List<StandardEntity> agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering arrangePartitions...");

        List<Partition> allPartitions = new ArrayList<Partition>();
        unassignedPartitions.clear();

        for (Partition partition : partitions) {

//            if (partition.getNumberOfNeededPFs() > 0) {
//                allPartitions.add(partition);
//            } else {
//                unassignedPartitions.add(partition);
//            }

            if (allPartitions.size() >= agents.size()) {
                break;
            } else {
                if (partition.getValue() >= 17) {
                    allPartitions.add(partition);
                }
            }
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from arrangePartitions.");
        return allPartitions;
    }


}
