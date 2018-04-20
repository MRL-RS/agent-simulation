package mrl.ambulance.targetSelector;

import javolution.util.FastMap;
import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.ambulance.structures.CivilianValue;
import mrl.common.Util;
import mrl.common.comparator.ConstantComparators;
import mrl.partitioning.Partition;
import mrl.partitioning.PartitionUtilities;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/21/13
 *         Time: 2:05 PM
 */
public class LegacyPartitionBasedTargetSelector extends TargetSelector {
    private StandardEntity myTarget;
    private Pair<Integer, Integer> pairPosition = null;


    public LegacyPartitionBasedTargetSelector(MrlWorld world, AmbulanceConditionChecker conditionChecker, AmbulanceUtilities ambulanceUtilities, Partition myPartition) {
        super(world, conditionChecker, ambulanceUtilities);
    }


    /**
     * Finds best target between specified possible victims
     *
     * @param victims victims to search between them
     * @return best target to select
     */
    @Override
    public StandardEntity nextTarget(Set<StandardEntity> victims) {
        setWorkingPartition();
        if (!victims.contains(myTarget)) {
            myTarget = null;
        }

        if (myTarget != null) {
            Human human = (Human) myTarget;
            if (conditionChecker.isPassable(world.getSelfPosition().getID(), human.getPosition())) {
                return myTarget;
            } else {
                victims.remove(myTarget);
            }
        }
        if (victims.isEmpty()) {
            return null;
        }


        ArrayList<CivilianValue> civilianTTAs;
        //look in partitions
        Map<EntityID, Set<StandardEntity>> partitionVictimMap = PartitionUtilities.findPartitionVictimMap(world, victims);

        StandardEntity victimToRescue = findVictimToRescue(partitionVictimMap, victims);

//        System.out.println(world.getPlatoonAgent().getDebugString() + " victimToRescue: " + victimToRescue);

        myTarget = victimToRescue;

        return victimToRescue;
    }

    private StandardEntity findVictimToRescue(Map<EntityID, Set<StandardEntity>> partitionVictimMap, Set<StandardEntity> myGoodHumans) {

        Set<StandardEntity> victims;
        StandardEntity bestVictim = null;
        if (myBasePartition == null) {
            bestVictim = findBestPartitionToGo(partitionVictimMap, myGoodHumans);
        } else {
            victims = partitionVictimMap.get(myBasePartition.getId());
            if (victims == null || victims.isEmpty()) {
//                System.out.println(world.getPlatoonAgent().getDebugString() + " myPartitionVictims are empty ");
                bestVictim = findBestPartitionToGo(partitionVictimMap, myGoodHumans);
            } else {

                bestVictim = findBestVictim(victims);
            }
        }

        return bestVictim;
    }


    private StandardEntity findBestVictim(Set<StandardEntity> victims) {

        List<StandardEntity> tempVictims = new ArrayList<StandardEntity>(victims);

        StandardEntity victimEntityToRescue = null;
        if (victims == null || victims.isEmpty()) {
            victimEntityToRescue = null;
        } else {

            Collections.sort(tempVictims, ConstantComparators.VICTIM_BURIENDNESS_COMPARATOR);

            EntityID selfPositionID = world.getSelfPosition().getID();
            Human victim;

            for (StandardEntity victimEntity : tempVictims) {
                victim = (Human) victimEntity;
                if (conditionChecker.isPassable(selfPositionID, victim.getPosition())) {
                    victimEntityToRescue = victimEntity;
                    break;
                }
            }

        }
        return victimEntityToRescue;
    }

    private StandardEntity findBestPartitionToGo(Map<EntityID, Set<StandardEntity>> partitionVictimMap, Set<StandardEntity> myGoodHumans) {
        List<StandardEntity> workerAmbulances;
        Set<StandardEntity> victims;
        Partition partitionToGo = null;
        int maxFoundCivilians = myGoodHumans.size();
        int maxAvailableAgents = ambulanceUtilities.getReadyAmbulances().size();
        int numberOfWorkers;
        int numberOfVictims;
        double value;
        double normalRatio = (double) maxFoundCivilians / (double) (maxAvailableAgents + 1);


//        System.out.println(world.getPlatoonAgent().getDebugString() + " normalRatio: " + normalRatio);


        List<Partition> partitions = new ArrayList<Partition>(world.getPartitionManager().getPartitions());
        Map<EntityID, List<StandardEntity>> partitionAmbulanceMap = findPartitionAmbulanceMap();


        for (Partition partition : partitions) {
            workerAmbulances = partitionAmbulanceMap.get(partition.getId());
            victims = partitionVictimMap.get(partition.getId());

            if (workerAmbulances == null || workerAmbulances.isEmpty()) {
                numberOfWorkers = 0;
            } else {
                numberOfWorkers = workerAmbulances.size();
            }

            if (victims == null || victims.isEmpty()) {
                numberOfVictims = 0;
            } else {
                numberOfVictims = victims.size();
            }


            value = computePartitionValue(numberOfWorkers, numberOfVictims);
//            System.out.println(world.getPlatoonAgent().getDebugString() + "partition:" + partition.getId() + " partitionValue: " + value);

            partition.setValue(value);
        }


        Collections.sort(partitions, DISTANCE_TO_PARTITION_COMPARATOR);
        StandardEntity bestVictim = null;
        StandardEntity tempVictim = null;
        int minNeededAgents;

        for (Partition partition : partitions) {
            if (partition.getValue() > normalRatio) {

                victims = partitionVictimMap.get(partition.getId());
                while (!victims.isEmpty()) {
                    tempVictim = findBestVictim(victims);
                    if (tempVictim == null) {
                        break;
                    }
                    minNeededAgents = ambulanceUtilities.computeMinimumNeededAgent((Human) tempVictim);

                    if (shouldBeSelected(tempVictim, minNeededAgents)) {
//                        System.out.println(world.getPlatoonAgent().getDebugString()+" ID: "+tempVictim.getID()+" min:"+minNeededAgents);
                        bestVictim = tempVictim;
                        break;
                    }

                    victims.remove(tempVictim);
                }
                if (bestVictim != null) {
                    break;
                }
            }
        }

        return bestVictim;
    }


    private boolean shouldBeSelected(StandardEntity bestVictim, int minNeededAgents) {

        List<StandardEntity> agents = new ArrayList<StandardEntity>(ambulanceUtilities.getReadyAmbulances());
        pairPosition = bestVictim.getLocation(world);


        Collections.sort(agents, VICTIM_DISTANCE_COMPARATOR);


        if (agents.indexOf(world.getEntity(world.getSelf().getID())) < minNeededAgents) {
//            for (StandardEntity entity:agents){
////                System.out.println(" distance "+world.getPlatoonAgent().getDebugString()+" id:"+entity.getID());
//            }

            return true;
        } else {
            return false;
        }

    }

    private Map<EntityID, List<StandardEntity>> findPartitionAmbulanceMap() {

        Map<EntityID, List<StandardEntity>> partitionAmbulanceMap = new FastMap<EntityID, List<StandardEntity>>();

        Partition partition;
        Human human;
        List<StandardEntity> ambulanceList;
        for (StandardEntity entity : ambulanceUtilities.getReadyAmbulances()) {

            human = (Human) entity;
            partition = world.getPartitionManager().findPartitionAtArea(human.getPosition());
            if (partition == null) {
                // do nothing
            } else {
                ambulanceList = partitionAmbulanceMap.get(partition.getId());
                if (ambulanceList == null) {
                    ambulanceList = new ArrayList<StandardEntity>();
                }
                ambulanceList.add(entity);
                partitionAmbulanceMap.put(partition.getId(), ambulanceList);
            }

        }

        return partitionAmbulanceMap;

    }


    private double computePartitionValue(int workerAmbulances, int victims) {
        double ratio;
        if (workerAmbulances == 0) {
            ratio = 0;
        } else {
            ratio = (double) victims / (double) workerAmbulances;
        }
        return ratio;
    }


    public Comparator<StandardEntity> VICTIM_DISTANCE_COMPARATOR = new Comparator<StandardEntity>() {
        public int compare(StandardEntity r1, StandardEntity r2) {

            Human h1 = (Human) r1;
            Human h2 = (Human) r2;
            int firstDistance = Util.distance(h1.getLocation(world), pairPosition);
            int secondDistance = Util.distance(h2.getLocation(world), pairPosition);

            if (firstDistance > secondDistance)
                return 1;
            if (firstDistance == secondDistance)
                return 0;

            return -1;
        }
    };
}
