package mrl.partitioning;

import mrl.MrlPersonalData;
import mrl.ambulance.VictimClassifier;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.TimestampThreadLogger;
import mrl.world.MrlWorld;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/12/12
 * Time: 5:27 PM
 */
public class AmbulancePartitionManager extends DefaultPartitionManager {
    private static final Log logger = LogFactory.getLog(AmbulancePartitionManager.class);

    private AmbulanceUtilities utilities;
    private VictimClassifier victimClassifier;

    /**
     * Creates an instance of StupidPartitionManager
     *
     * @param world            The world this instance is supposed to perform partitioning operations.
     * @param utilities        reference to a {@link mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities}
     * @param victimClassifier
     */
    public AmbulancePartitionManager(MrlWorld world, AmbulanceUtilities utilities, VictimClassifier victimClassifier) {
        super(world);
        this.utilities = utilities;
        this.victimClassifier = victimClassifier;

        utilities.updateReadyAmbulances();
        numberOfAgents = world.getAmbulanceTeamList().size();
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
        utilities.updateReadyAmbulances();
//        List<StandardEntity> ambulanceTeams = new ArrayList<StandardEntity>(utilities.getReadyAmbulances());
        List<StandardEntity> ambulanceTeams = new ArrayList<StandardEntity>(world.getAmbulanceTeamList());

        int numberOfAmbulances = ambulanceTeams.size();

        if (ambulanceTeams.isEmpty()) {
            ambulanceTeams.add(world.getSelfHuman());
        }

        //finding number of needed Segments
        int numberOfSegments = (int) Math.ceil(numberOfAmbulances / (double) NUMBER_OF_AGENTS_IN_PARTITION);
        if (numberOfSegments == 0) {
            numberOfSegments = 1;
        }

        //segment world into specified number of partitions
        partitions = split(worldPartition, numberOfSegments);

        //fill values of each constructed partitions
        fillPartitions(ambulanceTeams.size());

        //handles assignment of each agent to a proper partition
        updateAssignment(ambulanceTeams);

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
                partitionOperationMode = partitionDecideOperationMode(utilities.getReadyAmbulances().size(), world.getAmbulanceTeams().size());

            }

            this.updatePartitions(utilities.getReadyAmbulances().size());

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
            List<StandardEntity> ambulanceTeams = new ArrayList<>();
            for (AmbulanceTeam policeForce : world.getAmbulanceTeamList()) {
                ambulanceTeams.add(policeForce);
            }
            updateAssignment(ambulanceTeams);
            MrlPersonalData.VIEWER_DATA.setPartitions(world.getSelfHuman().getID(), this.getPartitions(), humanPartition, findHumanPartitionsMap(world.getSelfHuman()));

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

        Map<EntityID, Set<StandardEntity>> partitionVictimMap = PartitionUtilities.findPartitionVictimMap(world, victimClassifier.getMyGoodHumans());


        //update independent features
        for (Partition partition : partitions) {
            if (!partition.isDead()) {
                partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
//                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));

                partition.setVictims(partitionVictimMap.get(partition.getId()));
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

        Map<EntityID, Set<StandardEntity>> partitionVictimMap = PartitionUtilities.findPartitionVictimMap(world, victimClassifier.getMyGoodHumans());


        //update independent features
        for (Partition partition : partitions) {
            if (!partition.isDead()) {
                partition.setBurningBuildings(partitionUtilities.getBurningBuildings(partition));
//                partition.setUnVisitedBuilding(partitionUtilities.getUnVisitedBuildings(partition));
                partition.getUnVisitedBuilding().removeAll(world.getVisitedBuildings());

                partition.setVictims(partitionVictimMap.get(partition.getId()));
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
        return CL_AT_IN_PARTITION;
    }

    @Override
    protected int getNeededAgentsInNormalSituation() {
        return AT_IN_PARTITION;
    }


}
