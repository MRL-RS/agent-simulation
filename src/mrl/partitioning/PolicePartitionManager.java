package mrl.partitioning;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.assignment.HungarianAlgorithmWrapper;
import mrl.common.TimestampThreadLogger;
import mrl.partitioning.costMatrixMaker.CostMatrixMaker;
import mrl.partitioning.segmentation.SegmentType;
import mrl.police.moa.PoliceForceUtilities;
import mrl.world.MrlWorld;
import mrl.world.object.Route;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/12/12
 * Time: 5:27 PM
 */
public class PolicePartitionManager extends DefaultPartitionManager {
    private static final Log logger = LogFactory.getLog(PolicePartitionManager.class);

    private PoliceForceUtilities utilities;

    private List<Partition> previousPartitions;
    private IPartitionValueDetermination valueDetermination;

    private IPartitionNeededAgentsComputation neededAgentsComputation;
    //Map of PartitionID to the list of nearest refuge path, which contains in that partition
    private Map<EntityID, Set<EntityID>> partitionsRefugePathMap;

    private Map<EntityID, Partition> partitionMap;


    //TODO @BrainX Make this singleton, with Spring maybe?

    /**
     * Creates an instance of PolicePartitionManager
     *
     * @param world     The world this instance is supposed to perform partitioning operations.
     * @param utilities reference to a {@link PoliceForceUtilities}
     */
    public PolicePartitionManager(MrlWorld world, PoliceForceUtilities utilities) {
        super(world);
        this.utilities = utilities;
        numberOfAgents = world.getPoliceForceList().size();


    }

    @Override
    protected void initialVariables(MrlWorld world) {
        super.initialise();

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

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering initialize...");

        initialVariables(world);


        Partition worldPartition = partitionHelper.makeWorldPartition();

        int numberOfPolices = world.getPoliceForceList().size();


        partitions = split(worldPartition, numberOfPolices);
        if (partitions == null || partitions.isEmpty()) {
            partitions = new ArrayList<Partition>();
            partitions.add(worldPartition);
        }

        fillPartitions(numberOfPolices);

        partitionMap = new HashMap<>();
        for (Partition partition : partitions) {
            partitionMap.put(partition.getId(), partition);
        }


//        partitionHelper.createRendezvous(partitions, world);


        List<StandardEntity> policeEntities = new ArrayList<>();

        for (PoliceForce policeForce : world.getPoliceForceList()) {
            policeEntities.add(policeForce);
        }
        updateAssignment(policeEntities);


        // find and set neighbour partitions for each partition
        findPartitionsNeighbours();

        //Extracting Spider  Net Routes between partitions
        //ExtractingSpiderNetRoutes();


        MrlPersonalData.VIEWER_DATA.setPartitions(world.getSelfHuman().getID(), this.getPartitions(), findHumanPartition(world.getSelfHuman()), findHumanPartitionsMap(world.getSelfHuman()));

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from initialize.");
        TimestampThreadLogger.getCurrentThreadLogger().flush();
    }

    private void findPartitionsNeighbours() {
        for (Partition partition : partitions) {

            // find and set neighbour partitions for each partition
            Set<EntityID> neighbours = partitionHelper.findNeighbours(partition, partitions, world.getViewDistance() / 2);
            partition.setNeighbours(neighbours);
        }
    }

    /**
     * This method is to extract Spider Net Routs which are routes between partitions to let agents access other
     * partitions quickly
     */
    private void ExtractingSpiderNetRoutes() {

        for (Partition partition : partitions) {
            //Find Routes between each neighbour partition
            //To be sure about route uniqueness, find rout from partition with lower Id to higher one
            // Map<partitionId,Route>
            Map<EntityID, Route> neighbourRoutes = partition.getNeighbourRoutes();
            if (neighbourRoutes == null) {
                neighbourRoutes = new HashMap<>();
            }
            for (EntityID neighbourId : partition.getNeighbours()) {
                if (neighbourRoutes.get(neighbourId) == null) {
                    Route route;
                    Partition secondPartition = partitionMap.get(neighbourId);
                    if (secondPartition.getNeighbourRoutes() != null) {
                        if (secondPartition.getNeighbourRoutes().get(partition.getId()) != null) {
                            neighbourRoutes.put(neighbourId, secondPartition.getNeighbourRoutes().get(partition.getId()));
                            continue;
                        }
                    } else {
                        secondPartition.setNeighbourRoutes(new HashMap<EntityID, Route>());
                    }
                    if (partition.getId().getValue() < neighbourId.getValue()) {
                        route = new Route(partitionHelper.findRoute(partition, secondPartition));
                    } else {
                        route = new Route(partitionHelper.findRoute(secondPartition, partition));
                    }
                    neighbourRoutes.put(neighbourId, route);
                    secondPartition.getNeighbourRoutes().put(partition.getId(), route);
                }
            }
            partition.setNeighbourRoutes(neighbourRoutes);
        }


        //To show in viewer
        MrlPersonalData.VIEWER_DATA.setAllPartitionsMapData(world.getSelfHuman().getID(), partitionMap);


    }

    @Override
    public void update() {
        try {
            super.update();
            if (world.getTime() < SECOND_PARTITIONING_TIME) {
                // After updating Healthy Police force information, it's time to decide which OperationMode to choose:
                partitionOperationMode = partitionDecideOperationMode(utilities.getHealthyPoliceForces().size(), world.getPoliceForces().size());
            }

            this.updatePartitions(utilities.getHealthyPoliceForces().size());

            if (partitionOperationMode == OperationMode.DUAL_PARTITIONING
                    && world.getTime() == SECOND_PARTITIONING_TIME) {
                // It's about time each police force should re-calculate partitioning to include newly added healthy police
                // forces. (Whether recently buried, or healthy but inside a building)

                this.initialise();
//                System.out.println(me().getID() + " my Partition is:" + partitionManager.findHumanPartitions(selfHuman).getId() + " NumberOfNeeded:" + partitionManager.findHumanPartitions(selfHuman).getNumberOfNeededPFs());

            }

            if (world.getTime() == world.getIgnoreCommandTime()) {

                List<StandardEntity> unKnownAgents = new ArrayList<StandardEntity>(world.getPoliceForces());
                for (Map.Entry<Human, Partition> humanPartitionEntry : this.getHumanPartitionEntrySet()) {
                    unKnownAgents.remove(humanPartitionEntry.getKey());
                }

                Human unknownHuman;
                for (StandardEntity agent : unKnownAgents) {
                    // this agent is not assigned to any partition, I will assign it to the partition it is located at.
                    unknownHuman = (Human) agent;
                    this.forceAssignAgent(unknownHuman,
                            this.findPartitionAtArea(unknownHuman.getPosition()));

                }

            } else if (this.findHumanPartition(world.getSelfHuman()) == null) {
                this.forceAssignAgent(world.getSelfHuman(),
                        this.findPartitionAtArea(world.getSelfPosition().getID()));
            }


            if (this.findHumanPartition(world.getSelfHuman()).isDead()) {
                Partition currentPartition = this.findHumanPartition(world.getSelfHuman());
                Partition partitionToGo = findNearestNeighbourPartition(currentPartition, this.getPartitions());
                if (partitionToGo == null) {
                    partitionToGo = currentPartition;
                }
                this.forceAssignAgent(world.getSelfHuman(), partitionToGo);
            }

            List<StandardEntity> policeEntities = new ArrayList<>();
            for (PoliceForce policeForce : world.getPoliceForceList()) {
                policeEntities.add(policeForce);
            }
            updateAssignedPartitions(policeEntities);

            MrlPersonalData.VIEWER_DATA.setPartitions(world.getSelfHuman().getID(), this.getPartitions(), findHumanPartition(world.getSelfHuman()), findHumanPartitionsMap(world.getSelfHuman()));

        } catch (Exception e) {
            logger.error("Failed to perform Partitioning related operations.");
            logger.debug("Stack Trace:", e);
        }
    }

    @Override
    public Set<Partition> findHumanPartitionsMap(Human human) {
        return this.myPartitions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPartitions(int agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartitions...");

        //update independent features
        for (Partition partition : partitions) {
            TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");
            if (!partition.isDead()) {
                partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
//                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
                partition.getUnVisitedBuilding().removeAll(world.getVisitedBuildings());
                partition.setBlockedAgents(partitionUtilities.getBlockedAgents(partition, false));
                partition.setBuriedAgents(partitionUtilities.getBuriedAgents(partition, false));
//            partition.setValue(valueDetermination.computeValue(partition));
                partition.setDead(partitionUtilities.isPartitionDead(partition));
            }
            TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
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
    public void updatePartitions(int agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartitions...");

        //update independent features
        for (Partition partition : partitions) {
            TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");
            if (!partition.isDead()) {
                partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
                partition.setBlockedAgents(partitionUtilities.getBlockedAgents(partition, false));
                partition.setBuriedAgents(partitionUtilities.getBuriedAgents(partition, false));
//            partition.setValue(valueDetermination.computeValue(partition));
                partition.setDead(partitionUtilities.isPartitionDead(partition));
            }
            TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
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


        updateAssignedPartitions(agents);


        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updateAssignment.");
    }


    /**
     * Updates partition properties
     *
     * @param partition partition to update
     */
    @Override
    protected void updatePartition(Partition partition) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");
        if (!partition.isDead()) {
            partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
            partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
            partition.setBlockedAgents(partitionUtilities.getBlockedAgents(partition, false));
            partition.setBuriedAgents(partitionUtilities.getBuriedAgents(partition, false));
//            partition.setValue(valueDetermination.computeValue(partition));
            partition.setDead(partitionUtilities.isPartitionDead(partition));
        }
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
    }

    @Override
    protected int getNeededAgentsInCommunicationLessSituation() {
        return CL_PF_IN_PARTITION;
    }

    @Override
    protected int getNeededAgentsInNormalSituation() {
        return PF_IN_PARTITION;
    }


}
