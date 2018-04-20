package mrl.ambulance;

import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.helper.HumanHelper;
import mrl.partitioning.AmbulancePartitionManager;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Human;

/**
 * User: RescueSim
 * Date: 6/20/12
 * Time: 9:56 PM
 */
public class SimpleCoordination {

    private MrlWorld world;
    private AmbulanceUtilities ambulanceUtilities;
    private AmbulanceConditionChecker conditionChecker;
    private AmbulancePartitionManager ambulancePartitionManager;
    private Human myTarget;
    private Partition myPartition;
    HumanHelper humanHelper;

    public SimpleCoordination(MrlWorld world, AmbulanceUtilities ambulanceUtilities, AmbulanceConditionChecker conditionChecker, VictimClassifier victimClassifier) {
        this.world = world;
        this.ambulanceUtilities = ambulanceUtilities;
        this.conditionChecker = conditionChecker;

        ambulancePartitionManager = new AmbulancePartitionManager(world, ambulanceUtilities, victimClassifier);
        ambulancePartitionManager.initialise();
        world.setPartitionManager(ambulancePartitionManager);

        myPartition = ambulancePartitionManager.findHumanPartition(world.getSelfHuman());

        humanHelper = world.getHelper(HumanHelper.class);
    }

  /*  public Human getNextTarget(List<StandardEntity> goodHumans) {

        if (!goodHumans.contains(myTarget)) {
            myTarget = null;
        }

        myTarget = (Human) selectTarget(goodHumans, myTarget);
        return myTarget;
    }

    private StandardEntity selectTarget(List<StandardEntity> myGoodHumans, StandardEntity myTarget) {
        if (myTarget != null) {
            Human human = (Human) myTarget;
            if (conditionChecker.isPassable(world.getSelfPosition().getID(), human.getPosition())) {
                return myTarget;
            } else {
                myGoodHumans.remove(myTarget);
            }
        }
        if (myGoodHumans.isEmpty()) {
            return null;
        }


        ArrayList<CivilianValue> civilianTTAs;
        //look in partition
        civilianTTAs = sortHumansBasedOnTTD(myGoodHumans, true);

        //look in world
        if (civilianTTAs.isEmpty()) {
            civilianTTAs = sortHumansBasedOnTTA(myGoodHumans, false);
        }
        if (civilianTTAs.isEmpty()) {
            return null;
        }

        return world.getEntity(civilianTTAs.get(0).getId());


    }


    private ArrayList<CivilianValue> sortHumansBasedOnTTD(List<StandardEntity> myGoodHumans, boolean inPartition) {
        int ttd;
        ArrayList<CivilianValue> civilianTTDs = new ArrayList<CivilianValue>();
        CivilianValue civilianTTD;

        //computes agents Time To Death
        for (StandardEntity standardEntity : myGoodHumans) {
            Human human = (Human) standardEntity;
            if (conditionChecker.isPassable(world.getSelfPosition().getID(), human.getPosition())) {  // if this victim can be alive
                ttd = ambulanceUtilities.computeTTD(human).first();
                humanHelper.setTimeToDeath(human.getID(), ttd);
            }
        }

        //in an increasing manner
        Collections.sort(myGoodHumans, Human_TTDComparator);


        List<VictimAllocation> victimAllocations = computeNeededAT(myGoodHumans, healthyATs);

        findProperATforEachVictim(victimAllocations, healthyATs);


        return civilianTTDs;
    }


    public Comparator Human_TTDComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            StandardEntity h1 = (StandardEntity) o1;
            StandardEntity h2 = (StandardEntity) o2;

            double ttd1 = world.getHelper(HumanHelper.class).getCAOP(h1.getID());
            double ttd2 = world.getHelper(HumanHelper.class).getCAOP(h2.getID());

            if (ttd1 < ttd2) //decrease
                return 1;
            if (ttd1 == ttd2)
                return 0;

            return -1;
        }
    };


    private List<VictimAllocation> computeNeededAT(ArrayList<StandardEntity> myGoodHumans, List<EntityID> healthyATs) {

        Human human;
        int minNeed, maxNeed, i = 0;


        List<VictimAllocation> victimAllocations = new ArrayList<VictimAllocation>();

        int numberOfAvailableAT = healthyATs.size();

//        addPlatoonAgentsToBadeHumans();

        for (StandardEntity entity : myGoodHumans) {

            human = (Human) entity;
            minNeed = computeMinimumNeededAgent(human);
            maxNeed = computeMaximumNeededAgent(human);
            victimAllocations.add(new VictimAllocation(human.getID(), minNeed, maxNeed));
        }

        // sort victims based on biggest minNeedAgents to smallest minNeedAgents
        Collections.sort(victimAllocations);

        int numberOfAllocated = 0;

        if (victimAllocations.isEmpty()) {
            return victimAllocations;
        } else if (victimAllocations.size() == 1) {
            int numberOfMaxNeed = victimAllocations.get(0).getMaxNeededAgents();
            if (numberOfMaxNeed <= numberOfAvailableAT) {
                victimAllocations.get(0).setNumberOfAllocated(numberOfMaxNeed);
            } else {
                victimAllocations.get(0).setNumberOfAllocated(numberOfAvailableAT);
            }
            return victimAllocations;
        } else {

            while (numberOfAvailableAT > 0) {
                if (i == 0) {
                    if (victimAllocations.get(i).getMinNeededAgents() >= victimAllocations.get(i + 1).getMinNeededAgents()) {
                        if (victimAllocations.get(i).getMinNeededAgents() == 0) {
                            break;
                        } else {
                            minNeed = victimAllocations.get(i).getMinNeededAgents();
                            victimAllocations.get(i).setMinNeededAgents(minNeed - 1);
                            numberOfAllocated = victimAllocations.get(i).getNumberOfAllocated();
                            victimAllocations.get(i).setNumberOfAllocated(numberOfAllocated + 1);

                            numberOfAvailableAT--;
                            continue;
                        }
                    } else {
                        i++;
                        continue;
                    }
                } else {
                    if (victimAllocations.get(i).getMinNeededAgents() > victimAllocations.get(i - 1).getMinNeededAgents()) {
                        minNeed = victimAllocations.get(i).getMinNeededAgents();
                        victimAllocations.get(i).setMinNeededAgents(minNeed - 1);
                        numberOfAllocated = victimAllocations.get(i).getNumberOfAllocated();
                        victimAllocations.get(i).setNumberOfAllocated(numberOfAllocated + 1);

                        numberOfAvailableAT--;
                        continue;

                    } else if (i == victimAllocations.size() - 1) {
                        i = 0;
                        continue;
                    } else if (victimAllocations.get(i).getMinNeededAgents() == victimAllocations.get(i - 1).getMinNeededAgents()
                            && victimAllocations.get(i).getMinNeededAgents() < victimAllocations.get(i + 1).getMinNeededAgents()) {
                        i++;
                        continue;
                    } else {
                        i = 0;
                        continue;
                    }

                }
            }
        }


        i = 0;
        while (numberOfAvailableAT > 0 && i < victimAllocations.size()) {
            if (victimAllocations.get(i).getNumberOfAllocated() >= victimAllocations.get(i).getMaxNeededAgents()) {
                i++;
                continue;
            } else {
                numberOfAllocated = victimAllocations.get(i).getNumberOfAllocated();
                victimAllocations.get(i).setNumberOfAllocated(numberOfAllocated + 1);
                numberOfAvailableAT--;
                i++;          // Todo ooooooooooo  just add one more agent
                continue;
            }
        }


        return victimAllocations;
    }

    *//**
     * minimum needed AmbulanceTeam to rescue the human
     *
     * @param human the human to calculate its need
     * @return number of needed agents
     *//*
    public int computeMinimumNeededAgent(Human human) {

        int minimumNeed = 0;
        //time to death
        int ttd;

        //Optimistic
//        ttd = humanHelper.getCurrentHP(human.getID()) / (humanHelper.getCurrentDamage(human.getID()) + 1);

        //realistic  without fire consideration
        ttd = humanHelper.getTimeToDeath(human.getID());

        //realistic  without fire consideration
//        ttd= TODO:-------------------------


        int ttr;
        //Time To Refuge
        if (human.isPositionDefined()) {
            ttr = computeTimeToNearestAvailableRefuge(human.getPosition(world));
        } else {
            ttr = 1000;
        }
        if (ttd > ttr) {
            minimumNeed = (int) Math.ceil((float) human.getBuriedness() / (ttd - ttr));
            if (minimumNeed == 0) {
                minimumNeed = 1;
            }
        } else {
            minimumNeed = 0;
        }
        return minimumNeed;
    }

    private int computeTimeToNearestAvailableRefuge(StandardEntity position) {

        //Time To Refuge
        int ttr = 1000;
        int temp = 0;
        boolean isOpen = false;
        Building refuge;
        for (StandardEntity refugeStandardEntity : world.getRefuges()) {

            refuge = (Building) refugeStandardEntity;
            isOpen = false;

            if (conditionChecker.isPassable(position.getID(), refuge.getID())) {
                isOpen = true;
            }

            if (isOpen) {

                temp = (int)(ambulanceUtilities.computeDistance(position.getID(), refuge.getID())/MRLConstants.MEAN_VELOCITY_OF_MOVING);
                if (temp < ttr) {
                    ttr = temp;
                }
            }
        }

        return ttr;

    }


    private void findProperATforEachVictim(List<VictimAllocation> victimAllocations, List<EntityID> healthyATs) {
        int numberOfNeeded = 0, i, j;
        ArrayList<Pair<EntityID, Integer>> bidPairs;

        List<Pair<EntityID, Integer>> ambulanceDistanceToVictim = new ArrayList<Pair<EntityID, Integer>>();
        int tta = 0;
        List<EntityID> healthyAmbulances;

        if (healthyATs == null || healthyATs.isEmpty()) {
            return;
        }

        for (VictimAllocation victimAlloc : victimAllocations) {
            numberOfNeeded = victimAlloc.getNumberOfAllocated();
            ambulanceDistanceToVictim.clear();

            bidPairs = world.getVictimBidsMap().get(victimAlloc.getVictimID());
            if (bidPairs != null && !bidPairs.isEmpty()) {


                Collections.sort(bidPairs, ConstantComparators.BID_VALUE_COMPARATOR);

                j = 0;
                while (numberOfNeeded > 0 && j < bidPairs.size()) {
                    if (j < bidPairs.size()) {

                        if (bidPairs.get(j) == null) {
                            System.out.print("");
                        }
                        if (bidPairs.get(j).first() == null) {
                            System.out.print("");
                        }
                        if (!world.getTaskAssignment().containsKey(bidPairs.get(j).first())) {
                            world.getTaskAssignment().put(bidPairs.get(j).first(), victimAlloc.getVictimID());
                            numberOfNeeded--;
                            j++;
                        } else {
                            j++;
                        }
                    } else {
                        break;
                    }
                }
            }

            if (numberOfNeeded <= 0)
                continue;

            for (EntityID entityID : healthyATs) {

                tta = ambulanceUtilities.approximatingTTM(world.getEntity(entityID), world.getEntity(victimAlloc.getVictimID()));
                ambulanceDistanceToVictim.add(new Pair<EntityID, Integer>(entityID, tta));

            }

            Collections.sort(ambulanceDistanceToVictim, ConstantComparators.DISTANCE_VALUE_COMPARATOR);

            for (Pair<EntityID, Integer> pair : ambulanceDistanceToVictim) {
                if (numberOfNeeded > 0) {
                    if (world.getTaskAssignment().containsKey(pair.first()) == false) {
                        world.getTaskAssignment().put(pair.first(), victimAlloc.getVictimID());
                        numberOfNeeded--;
                    }
                } else {
                    break;
                }

            }

        }


    }


    private ArrayList<CivilianValue> sortHumansBasedOnTTA(List<StandardEntity> myGoodHumans, boolean inPartition) {
        int tta;
        Pair<Integer, Integer> ttd;
        ArrayList<CivilianValue> civilianTTAs = new ArrayList<CivilianValue>();
        CivilianValue civilianTTA;

        for (StandardEntity standardEntity : myGoodHumans) {
            Human human = (Human) standardEntity;
//            ttd = ambulanceUtilities.computeTTD(human);
            if (ambulanceUtilities.isAlivable((Human) standardEntity) && conditionChecker.isPassable(world.getSelfPosition().getID(), human.getPosition())) {  // if this victim can be alive
                // it will be alive
//                    tta = ambulanceUtilities.approximatingTTA(human);
                tta = ambulanceUtilities.computeDistance(world.getSelfPosition().getID(), human.getPosition());
                civilianTTA = new CivilianValue(human.getID(), tta);
                civilianTTAs.add(civilianTTA);

            }
        }

        Collections.sort(civilianTTAs);

        return civilianTTAs;
    }
*/

}
