package mrl.partitioning;

import mrl.MrlPersonalData;
import mrl.common.TimestampThreadLogger;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/12/12
 * Time: 5:27 PM
 */
public class FireBrigadePartitionManager extends DefaultPartitionManager {
    private static final Log logger = LogFactory.getLog(FireBrigadePartitionManager.class);

    private FireBrigadeUtilities utilities;


    /**
     * Creates an instance of StupidPartitionManager
     *
     * @param world     The world this instance is supposed to perform partitioning operations.
     * @param utilities reference to a {@link FireBrigadeUtilities}
     */
    public FireBrigadePartitionManager(MrlWorld world, FireBrigadeUtilities utilities) {
        super(world);
        this.utilities = utilities;

        utilities.updateReadyFireBrigades();
        numberOfAgents = world.getFireBrigadeList().size();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialise() {
        super.initialise();
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering initialize...");

        initialVariables(world);

        Partition worldPartition = partitionHelper.makeWorldPartition();
        utilities.updateReadyFireBrigades();
//        List<StandardEntity> fireBrigades = new ArrayList<StandardEntity>(utilities.getReadyFireBrigades());
        List<StandardEntity> fireBrigades = new ArrayList<StandardEntity>(world.getFireBrigadeList());

        if (fireBrigades.isEmpty()) {
            fireBrigades.add(world.getSelfHuman());
        }
//        if (MRLConstants.FILL_VIEWER_DATA) {
//            System.out.println(world.getSelf().getID() + " Ready FireBrigades are " + fireBrigades.size());
//        }
        int numberOfFireBrigades = fireBrigades.size();
        //finding number of needed Segments
        int numberOfSegments = (int) Math.ceil(numberOfFireBrigades / (double) NUMBER_OF_AGENTS_IN_PARTITION);
        if (numberOfSegments == 0) {
            numberOfSegments = 1;
        }

        //segment world into specified number of partitions
        partitions = split(worldPartition, numberOfSegments);

        //filling values of each constructed partitions
        fillPartitions(fireBrigades.size());

        //handles assignment of each agent to a proper partition
        updateAssignment(fireBrigades);

        MrlPersonalData.VIEWER_DATA.setPartitions(world.getSelfHuman().getID(), this.getPartitions(), findHumanPartition(world.getSelfHuman()), findHumanPartitionsMap(world.getSelfHuman()));

        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from initialize.");
        TimestampThreadLogger.getCurrentThreadLogger().flush();
    }

    @Override
    public void update() {
        try {
            super.update();

            if (world.getTime() < SECOND_PARTITIONING_TIME) {
                // After updating ready Ambulance teams information, it's time to decide which OperationMode to choose:
                partitionOperationMode = partitionDecideOperationMode(utilities.getReadyFireBrigades().size(), world.getFireBrigades().size());

            }

            this.updatePartitions(utilities.getReadyFireBrigades().size());

            if (partitionOperationMode == OperationMode.DUAL_PARTITIONING
                    && world.getTime() == SECOND_PARTITIONING_TIME) {
                // It's about time each ambulanceTeams should re-calculate partitioning to include newly added healthy ambulances
                // forces. (Whether recently buried, or healthy but inside a building)

                this.initialise();
//                System.out.println(me().getID() + " my Partition is:" + partitionManager.findHumanPartitions(selfHuman).getId() + " NumberOfNeeded:" + partitionManager.findHumanPartitions(selfHuman).getNumberOfNeededPFs());

            }

            Partition humanPartition = this.findHumanPartition(world.getSelfHuman());
            if (world.getTime() == world.getIgnoreCommandTime()) {

                List<StandardEntity> unKnownAgents = new ArrayList<StandardEntity>(world.getAmbulanceTeams());
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

            } else if (humanPartition == null) {
                this.forceAssignAgent(world.getSelfHuman(),
                        this.findPartitionAtArea(world.getSelfPosition().getID()));
            }

            if (humanPartition == null) {
                this.forceAssignAgent(world.getSelfHuman(),
                        this.findPartitionAtArea(world.getSelfPosition().getID()));
            }


            humanPartition = this.findHumanPartition(world.getSelfHuman());

            if (humanPartition.isDead() || ((humanPartition.getUnVisitedBuilding() == null || humanPartition.getUnVisitedBuilding().isEmpty()) && (humanPartition.getVictims() == null || humanPartition.getVictims().isEmpty()))) {
                Partition partitionToGo = findNearestNeighbourPartition(humanPartition, this.getPartitions());
                if (partitionToGo == null) {
                    partitionToGo = humanPartition;
                }
                this.forceAssignAgent(world.getSelfHuman(), partitionToGo);
            }

            List<StandardEntity> fireBrigades = new ArrayList<>(world.getFireBrigadeList());
            updateAssignedPartitions(fireBrigades);

            MrlPersonalData.VIEWER_DATA.setPartitions(world.getSelfHuman().getID(), this.getPartitions(), findHumanPartition(world.getSelfHuman()), findHumanPartitionsMap(world.getSelfHuman()));

        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to perform Partitioning related operations.");
            logger.debug("Stack Trace:", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillPartitions(int agents) {
        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartitions...");


        //update independent features
        for (Partition partition : partitions) {
            if (!partition.isDead()) {
                partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
//                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
                partition.getUnVisitedBuilding().removeAll(world.getVisitedBuildings());
                partition.setDead(partitionUtilities.isPartitionDead(partition));
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
            if (!partition.isDead()) {
                partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
//                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
                partition.getUnVisitedBuilding().removeAll(world.getVisitedBuildings());
                partition.setDead(partitionUtilities.isPartitionDead(partition));
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
//        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Entering updatePartition...");

//        TimestampThreadLogger.getCurrentThreadLogger().log(world.getSelf().getID() + " Returning from updatePartition.");
    }

    @Override
    protected int getNeededAgentsInCommunicationLessSituation() {
        return CL_FB_IN_PARTITION;
    }

    @Override
    protected int getNeededAgentsInNormalSituation() {
        return FB_IN_PARTITION;
    }


}
