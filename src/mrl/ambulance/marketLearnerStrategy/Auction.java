package mrl.ambulance.marketLearnerStrategy;

import mrl.ambulance.AmbulanceStrategy;
import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.ambulance.structures.Bid;
import mrl.ambulance.structures.ValuableVictim;
import mrl.common.MRLConstants;
import mrl.common.comparator.ConstantComparators;
import mrl.communication2013.helper.AmbulanceMessageHelper;
import mrl.helper.HumanHelper;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 27, 2010
 * Time: 7:28:31 PM
 */
public class Auction implements IAuction {


    protected MrlAmbulanceTeamWorld world;
    protected AmbulanceUtilities ambulanceUtilities;
    protected AmbulanceMessageHelper ambulanceMessageHelper;

    protected int auctionPeriod = 3;
    protected int previuseAuctionTime = 0;
    protected int numberOfBids = 2;
    protected int lastBidTime = 0;


    ////// public metods
    ////////////////////////

    public Auction(MrlAmbulanceTeamWorld world, AmbulanceMessageHelper ambulanceMessageHelper, AmbulanceUtilities ambulanceUtilities) {
        this.world = world;
        this.ambulanceUtilities = ambulanceUtilities;
        this.ambulanceMessageHelper = ambulanceMessageHelper;

    }


    public List<StandardEntity> computeCAOPForHumans(Set<StandardEntity> humans, ArrayList<StandardEntity> myBadHumans) {

        int timeToArrive = -1;
        int ttr, currentDamage, currentHp, timeToDeath, buriedness;
        double cAOP = 0;
        double agentEffectCoef = 1;
        double ambulanceCoef = 0, fireBrigadeCoef = 0, policeCoef = 0;
        List<StandardEntity> toRemoveHumans = new ArrayList<StandardEntity>();


        if (humans.isEmpty()) {
            return null;
        }

        int numberOfAvailableATs = ambulanceUtilities.computeNumberOfHealthyATs();
        if (numberOfAvailableATs == 0)
            return null;


        if (world.getBuriedAgents().size() > 0) {
            ambulanceCoef = computeAmbulanceAgentCoef(humans.size());
            fireBrigadeCoef = computeFireBrigadeAgentCoef();
            policeCoef = computePoliceAgentCoef();

        }

        HumanHelper hH = world.getHelper(HumanHelper.class);
        for (StandardEntity standardEntity : humans) {
            Human human = (Human) standardEntity;

//            ambulanceUtilities.computeTimeToNearestAvailableRefuge()
            ttr = hH.getTimeToRefuge(human.getID());
            currentDamage = hH.getCurrentDamage(human.getID());
            currentHp = hH.getCurrentHP(human.getID());

            if (currentDamage <= 0) {
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.err.println("0000000000000000000000000000 currentDamage <= 0   myID:" + world.getSelf().getID() + " HID:" + human.getID() + " Current damage=" + currentDamage);

                currentDamage = 1;
                hH.setCurrentDamage(human.getID(), currentDamage);
            }

            timeToDeath = currentHp / currentDamage;

            // will dead, and nobody can not do anything
            if (!world.getRefuges().isEmpty() && (human.getBuriedness() / numberOfAvailableATs + ttr > timeToDeath)) {
                if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
                    System.out.println("BBBBBBBBBAAAAAADDDDDD CCIIVVIIILLLIIIAAAANNN" + human);
                    System.out.println(world.getTime() + " " + world.getSelf().getID() +
                            " id:" + human.getID() +
                            " cAOP:" + hH.getCAOP(human.getID()) +
                            " HP:" + human.getHP() +
                            " currentHP:" + hH.getCurrentHP(human.getID()) +
                            " DMG:" + human.getDamage() +
                            " currentDMG:" + hH.getCurrentDamage(human.getID()) +
                            " brd:" + human.getBuriedness() +
                            " currentBRD:" + hH.getCurrentBuriedness(human.getID()) +
                            " TTA:" + hH.getTimeToArrive(human.getID()) +
                            " NORA:" + hH.getNumberOfATsRescuing(human.getID()))
                    ;

                }
//                myBadHumans.add(human);
                toRemoveHumans.add(human);
                hH.setCAOP(human.getID(), 0);
                continue;
            }


            //Approximating Time To Arrive to civilian
            timeToArrive = ambulanceUtilities.approximatingTTA(human);
            hH.setTimeToArrive(human.getID(), timeToArrive);
            buriedness = human.getBuriedness();

            if (human instanceof Civilian) {
                agentEffectCoef = 1;
            } else {
                agentEffectCoef = computeAgentEffectCoefInCAOP(human);
            }

            if (buriedness <= 0) {
                //for victims who has no buriedness or very low
                cAOP = computeCAOPBasedOnFire(human, cAOP);
                hH.setCAOP(human.getID(), cAOP);
                continue;
            }


            if (timeToArrive == 0) {
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println("timeToArrivetimeToArrivetimeToArrivetimeToArrivetimeToArrivetimeToArrivetimeToArrive");
                timeToArrive = 1;
            }
            if (currentHp <= 0) {
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println("currentHpcurrentHpcurrentHpcurrentHpcurrentHpcurrentHpcurrentHpcurrentHpcurrentHpcurrentHp");
                currentHp = 100;
            }
            if (buriedness == 0) {
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println("human.getBuriedness()human.getBuriedness()human.getBuriedness()human.getBuriedness()");
                buriedness = 1;
            }
            if (currentDamage <= 0) {
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println("human.getCurrentDamage()human.getCurrentDamage()human.getCurrentDamage()human.getCurrentDamage()");
                currentDamage = 1;
            }
            //Formula of Computing cAOP=6/CTi *( damage/(buriedness*TTA))  if EmergencyLevel is in class one, increase cAOP
            cAOP = /*agentEffectCoef **/ ((double) currentDamage * 10000 / (double) (currentHp * buriedness * timeToArrive));

            if (human instanceof AmbulanceTeam) {
                cAOP += ambulanceCoef;
            } else if (human instanceof FireBrigade) {
                cAOP += fireBrigadeCoef;
            } else if (human instanceof PoliceForce) {
                cAOP += policeCoef;
            }

            cAOP = computeCAOPBasedOnFire(human, cAOP);


            hH.setCAOP(human.getID(), cAOP);

//            HumanHelper hH = world.getHelper(HumanHelper.class);
//            if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
//                System.out.println(world.getTime() + " " + world.getSelf().getID() +
//                        " id:" + human.getID() +
//                        " cAOP:" + hH.getCAOP(human.getID()) +
//                        " El:" + hH.getEmergencyLevel(human.getID()) +
//                        " hp:" + hH.getCurrentHP(human.getID()) +
//                        " dmg:" + hH.getCurrentDamage(human.getID()) +
//                        " brd:" + human.getBuriedness() +
//                        " TTA:" + timeToArrive +
//                        " LastBrd:" + hH.getPreviousBuriedness(human.getID()));
//            }


        }
//        humans.removeAll(toRemoveHumans);
        humans.removeAll(myBadHumans);

        // sort Civilians based on their cAOP value
        List<StandardEntity> victims = sortHumans(humans);


        return victims;

    }

    private double computePoliceAgentCoef() {
        double closePathCoef;

        Pair<Integer, Integer> numberOfCloseAndVisitedPaths = computeNumberOfCloseAndVisitedPaths();
        if (numberOfCloseAndVisitedPaths.second() > 0) {
            closePathCoef = 50f * numberOfCloseAndVisitedPaths.first() / numberOfCloseAndVisitedPaths.second();
            return closePathCoef;
        } else {
            return 0;
        }


    }

    private Pair<Integer, Integer> computeNumberOfCloseAndVisitedPaths() {
        int numberOfVisitedPaths = 0;
        int numberOfClosePaths = 0;
        for (Path path : world.getPaths()) {
            if (path.isItOpened() != null) {
                numberOfVisitedPaths++;
                if (path.isItOpened() == false) {
                    numberOfClosePaths++;
                }
            }
        }

        return new Pair<Integer, Integer>(numberOfClosePaths, numberOfVisitedPaths);
    }

    private double computeFireBrigadeAgentCoef() {

        if (world.getBuildings().size() > 0)
            return 50f * world.getBurningBuildings().size() / world.getBuildings().size();
        else
            return 0;
    }

    /**
     * compute a coefficient to add to the importance of a buried Ambulance Agent
     *
     * @param numberOfGoodHumans how many selectable humans exist to rescue
     * @return the computed coef
     */
    private double computeAmbulanceAgentCoef(int numberOfGoodHumans) {

        if (world.getCivilians().size() > 0) {
            return 50f * numberOfGoodHumans / world.getCivilians().size();
        } else {
            return 0;
        }
    }

    private double computeCAOPBasedOnFire(Human human, double cAOP) {


        for (StandardEntity standardEntity : world.getObjectsInRange(human.getPosition(world), 4 * world.getViewDistance())) {
            if (standardEntity instanceof Building) {
                if (((Building) standardEntity).isFierynessDefined() && ((Building) standardEntity).getFieryness() > 0) {
                    if (cAOP == 0) {
                        return 20;
                    } else {
                        return cAOP * 3;// a big value for CAOP
                    }
                }
            }
        }


        return cAOP;  //To change body of created methods use File | Settings | File Templates.
    }


    @Override
    public void startAuction(Set<StandardEntity> goodHumans) {

        //////////// Market Centralized Strategy
        staticAuctioning(goodHumans);
//
        //////////// Market Centralized Leader Initiator Strategy
//        } else if (strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED_LEADER_INITIATOR)) {
//            leaderInitiatorAuctioning(goodHumans);
//        }


    }

    private void leaderInitiatorAuctioning(ArrayList<StandardEntity> goodHumans) {


        //get m most valuable victims to bid
//        List<StandardEntity> victimsToSell = getVictimsToSell(goodHumans);
//
//        ambulanceMessageHelper.sendVictimsToSell(victimsToSell);

    }

    private List<StandardEntity> getVictimsToSell(ArrayList<StandardEntity> goodHumans) {

        return null;
    }

    private void staticAuctioning(Set<StandardEntity> goodHumans) {

        if (world.getTime() > 6 && world.getAmbulanceLeaderID() == null) {

//            System.out.println(world.getTime() + " " + world.getSelf().getID() + " I am Here >>>>>>>>> ");
            ambulanceUtilities.chooseLeader();
            if (MRLConstants.DEBUG_AMBULANCE_TEAM && world.getAmbulanceLeaderID().equals(world.getSelf().getID())) {
                System.out.println("......" + world.getTime() + " " + world.getSelf().getID() + " I AM LEADER................");
            }

        }


        if (world.getTime() % auctionPeriod != 0)
            return;


        this.previuseAuctionTime = world.getTime();

        //clear previouse bids
        world.getBids().clear();
        world.getBadeHumans().clear();

        world.getVictimBidsMap().clear();

//        //clear previouse assigned tasks
//        world.getTaskAssignment().clear();

        //get m most valuable victims to bid
        List<StandardEntity> humansToBid;
        if (world.getRefuges().isEmpty()) {
            humansToBid = getBestHumansToBid_refugeless(goodHumans);
        } else {
            humansToBid = getBestHumansToBid(goodHumans);
        }
        if (MRLConstants.DEBUG_AMBULANCE_TEAM)
            System.out.println(world.getSelf().getID() + " Humans to BID : " + humansToBid);


        if (!humansToBid.isEmpty()) {
            lastBidTime = world.getTime();
        }

        //create bidMessage from selected victims
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        ArrayList<AmbulanceTeamBid> bids = new ArrayList<AmbulanceTeamBid>();
        for (StandardEntity standardEntity : humansToBid) {
            AmbulanceTeamBid bid = new AmbulanceTeamBid();
            bid.setBidderID(world.getSelf().getID());
            bid.setHumanID(standardEntity.getID());
            bid.setCivilian(standardEntity instanceof Civilian);
            bid.setBidValue((int) (10 * humanHelper.getCAOP(standardEntity.getID())));
            if (bid.getBidValue() > 255) {
                bid.setBidValue(255);
            }
            bids.add(bid);
            if (!world.getBadeHumans().contains(bid.getHumanID())) {
                // for simlpicity of computation we reserve it
                world.getBadeHumans().add(bid.getHumanID());
            }

        }
        world.getBids().put(world.getSelf().getID(), bids);


        // send bids
        //ambulanceMessageHelper.sendBidMessage(bids);
    }

    /**
     * Gets best humans when there is no refuge, in this state, just humans who will be alive should be rescued; The
     * mechanism is as below:
     * 1)finds which victims will be alive, means TTD>=250
     * 2)sort victims based on biggest TTD AND then
     * 3)sort victims based on nearest ones to fires AND then
     * 4)sort victims based on lowest BRD
     *
     * @param goodHumans victims to find betters to bid from
     * @return sorted victims based on ttd,fire and brd
     */
    private List<StandardEntity> getBestHumansToBid_refugeless(Set<StandardEntity> goodHumans) {

        Pair<Integer, Integer> ttd;
        double distanceToFire;
        double distanceToMe;

        List<ValuableVictim> valuableVictims = new ArrayList<ValuableVictim>();
        List<StandardEntity> valuables = new ArrayList<StandardEntity>();

        ValuableVictim valuableVictim;
        Human human;
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        System.out.println(world.getTime() + " " + world.getSelf().getID() + "---------------------------------");

        for (StandardEntity victim : goodHumans) {
            ttd = ambulanceUtilities.computeTTD(victim);
//            System.out.println(world.getTime()+" self: "+world.getSelf().getID()+" id: "+victim.getID()+" ttd:"+ttd);
            if (ttd.second() >= 250 || ttd.first() < 0) {
                distanceToFire = ambulanceUtilities.computeDistanceToFire(victim.getID());
                human = (Human) victim;
                distanceToMe = ambulanceUtilities.approximatingTTA(human);
                if (distanceToFire == Integer.MAX_VALUE) {
                    humanHelper.setCAOP(victim.getID(), (double) 100 / (distanceToMe + human.getBuriedness()));
                } else {
                    distanceToFire = distanceToFire / MRLConstants.MEAN_VELOCITY_OF_MOVING;
                    humanHelper.setCAOP(victim.getID(), (double) 100 / (distanceToMe + Math.pow(distanceToFire, 2) + human.getBuriedness()));
                }
                valuableVictim = new ValuableVictim(victim.getID(), humanHelper.getCAOP(victim.getID()));
                valuableVictims.add(valuableVictim);
            }
        }

        Collections.sort(valuableVictims, ConstantComparators.VALUABLE_VICTIM_COMPARATOR);

//        for(ValuableVictim v:valuableVictims){
//            System.out.println(world.getTime()+" self: "+world.getSelf().getID()+" id: "+v.getVictimID()+ " CAOP " + humanHelper.getCAOP(v.getVictimID()));
//        }

        if (!valuableVictims.isEmpty()) {
            for (int i = 0; i < Math.min(numberOfBids, valuableVictims.size()); i++) {
                valuables.add(world.getEntity(valuableVictims.get(i).getVictimID()));
            }
        }


        return valuables;

    }


    private ArrayList<StandardEntity> getBestHumansToBid(Set<StandardEntity> goodHumans) {


//        ArrayList<StandardEntity> humansToBid = new ArrayList<StandardEntity>();
//        int count = 0;
//        int i = 0;
//        int ttf;
//        while (count < numberOfBids && i < goodHumans.size()) {
//
//            ttf = ambulanceUtilities.timeToFreeForVictim((Human) goodHumans.get(i));
//            if (ttf > 5) {
//                humansToBid.add(goodHumans.get(i));
//                count++;
//            }
//            i++;
//        }
//
//        return humansToBid;
        throw new UnsupportedOperationException();
    }


    /**
     * compute Agent Effect Coefficient to use in CAOP
     * this function calculates NumberOfBRDOfThisType and totalOfThisType to compute n=NumberOfBRDOfThisType/totalOfThisType
     * and compute q as totalOfThisType /totalNumberOfAgents and at last return v=n/q as the effect coefficient
     *
     * @param human the human to find its effect in coefficient
     */
    private double computeAgentEffectCoefInCAOP(Human human) {
        double n, q, v;
        int numberOfBuriedOfThisType = 0;
        int totalNumberOfThisType = 0;
        int humanType = getInstanceType(human);
        int tempType;

        for (EntityID h : world.getBuriedAgents()) {
            tempType = getInstanceType((Human) world.getEntity(h));
            if (tempType == humanType) {
                numberOfBuriedOfThisType++;
            }
        }

        totalNumberOfThisType = getTotalNumberOfThisType(human);

        n = (double) numberOfBuriedOfThisType / totalNumberOfThisType;

        q = (double) totalNumberOfThisType / world.getAgents().size();

        v = n / q;

        return v;
    }

    private int getTotalNumberOfThisType(Human human) {
        if (human instanceof AmbulanceTeam)
            return world.getAmbulanceTeams().size();
        else if (human instanceof FireBrigade) {
            return world.getFireBrigades().size();
        } else { // instanceof PoliceForces
            return world.getPoliceForces().size();
        }

    }


    /**
     * find Instance Type of human
     *
     * @param human human to find instance
     * @return type of human; 1 is AT, 2 is FB and 3 is PF
     */
    private int getInstanceType(Human human) {
        if (human instanceof AmbulanceTeam) {
            return 1;
        } else if (human instanceof FireBrigade) {
            return 2;
        } else {
            return 3;
        }

    }

    /**
     * Computes the Benefit of accepting each civilian to rescue as a Bid
     * <p/>
     * Benefit=  NOCA * CAOP
     *
     * @param bids            list of civilians that suggested by all ATs with me
     * @param myGoodCivilians
     */
    public void computeBenefits(ArrayList<Bid> bids, ArrayList<Civilian> myGoodCivilians) {

//        System.out.println(world.getSelf().getID()+" BBBBBBB>>>>>>>> bids Before Remove "+ bids.size());
//        System.out.println(world.getSelf().getID()+"MYGOOOOOOOOOD:>>>>>>>>> "+myGoodCivilians.size());
//        System.out.println(world.getSelf().getID()+"AAAAAAA>>>>>>>> bids After Remove "+ bids.size());


//        ambulanceUtilities.updateBidCiviliansHP(bids);

//        computeCAOPForBidCivilians(bids);

        int numberofBids;

        for (Bid bid : bids) {

            Civilian civilian = bid.getCivilian(world);
            numberofBids = ambulanceUtilities.computeNumberOfATsThatSelect(bids, civilian);
            world.getHelper(HumanHelper.class).setBenefit(civilian.getID(), (1 + numberofBids / (double) world.getAmbulanceTeams().size()) * world.getHelper(HumanHelper.class).getCAOP(civilian.getID()));
//            civilian.setBenefit((  numberofBids/ (double)(world.getAmbulanceTeams().size()-numberofBids+1)) * civilian.getCAOP());
        }


    }

    public boolean didNewBidsCome() {
        return world.getBids().size() > 0;
    }


    ////// private metods
    ////////////////////////

    private int computeEmergencyLevel(ArrayList<Civilian> shouldRemoveCivilians, Civilian civ, int numberOfAvailableARs) {
//        if (!(civ.getDamageProperty().isDefined() &&
//                civ.getBuriednessProperty().isDefined() &&
//                civ.getHPProperty().isDefined()
//        )) {
//            return -1;
//        }

        int currentHP = world.getHelper(HumanHelper.class).getCurrentHP(civ.getID());
        int currentDamage = world.getHelper(HumanHelper.class).getCurrentDamage(civ.getID());
//        if (currentDamage == 0) {
//            currentDamage = 10;
//        }

        int timeToRefuge = world.getHelper(HumanHelper.class).getTimeToRefuge(civ.getID());


        if (currentHP <= 0)// maybe dead
        {
//            shouldRemoveCivilians.add(civ);
            return 6;
        }
        double healthToDamage = currentHP / currentDamage;
        int rescueTimeForAvailableATs = (int) Math.ceil(civ.getBuriedness() / numberOfAvailableARs);
        int rescueTimeForAllATs = (int) Math.ceil(civ.getBuriedness() / world.getAmbulanceTeams().size());

//        int nOACAT = ambulanceUtilities.computeNumberOfATsRescuingThis(civ);
        int nOACAT = world.getHelper(HumanHelper.class).getNumberOfATsRescuing(civ.getID());
        int rescueTimeForAboveCivilianATs;
        if (nOACAT != 0)
            rescueTimeForAboveCivilianATs = civ.getBuriedness() / nOACAT;
        else
            rescueTimeForAboveCivilianATs = 1000; // an impossible big time
//            rescueTimeForAboveCivilianATs = civ.getBuriedness() / 2; // an impossible big time


        if (civ.getBuriedness() == 0) {    //todo
            return 6;
        }


        if (/*(currentDamage == 0 || civ.getBuriedness() == 0) ||*/  // will be alive  or ==> will dead
                (healthToDamage < timeToRefuge * 3 / 4) ||
                        (healthToDamage + rescueTimeForAllATs <= timeToRefuge)) {
//            shouldRemoveCivilians.add(civ);
            return 6;
        }

        if ((healthToDamage == timeToRefuge + rescueTimeForAvailableATs) ||
                (healthToDamage == timeToRefuge + rescueTimeForAboveCivilianATs)) {
            return 5;
        }

        int temp = timeToRefuge + rescueTimeForAboveCivilianATs;
        if (healthToDamage > temp && healthToDamage < 2 * temp) {

            boolean condition = (currentDamage >= 150); // todo  && (TTF < rescueTimeForAboveCivilianATs);  seems equal
            if (nOACAT <= 1 && condition)
                return 1;
            else if (nOACAT == 2 && condition)
                return 2;
            else
                return 3;

        } else if (healthToDamage == temp) {
            //boolean condition = (civ.getDamage() > 500); // todo  && (TTF < rescueTimeForAboveCivilianATs);  seems equal
            if (nOACAT <= 2)
                return 3;
            else
                return 4;
        } else {
            return 4;
        }

//        if (civ.getEmergencyLevel() != -1)
//            return;


    }


    /**
     * Sort Civilians Based On CAOP( Civilian Amount Of Profitability)
     * CAOP = Damage / ( Buriedness * TTA )
     * TTA is Time To Arrive to a civilian by this AT
     *
     * @param humans civilian list to sort
     */
    private List<StandardEntity> sortHumans(Set<StandardEntity> humans) {

        List<StandardEntity> victims = new ArrayList<StandardEntity>(humans);
        Collections.sort(victims, Human_CAOPComparator);
        return victims;
    }

    public Comparator Human_CAOPComparator = new Comparator() {
        public int compare(Object o1, Object o2) {
            StandardEntity h1 = (StandardEntity) o1;
            StandardEntity h2 = (StandardEntity) o2;

            double caop1 = world.getHelper(HumanHelper.class).getCAOP(h1.getID());
            double caop2 = world.getHelper(HumanHelper.class).getCAOP(h2.getID());

            if (caop1 < caop2) //decrease
                return 1;
            if (caop1 == caop2)
                return 0;

            return -1;
        }
    };

    /**
     * minimum needed AmbulanceTeam to rescue the human
     *
     * @param human the human to calculate its need
     * @return number of needed agents
     */
    public int computeMinimumNeededAgent(Human human) {

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        int minimumNeed = 0;
        //time to death
        int ttd;

        //Optimistic
//        ttd = humanHelper.getCurrentHP(human.getID()) / (humanHelper.getCurrentDamage(human.getID()) + 1);

        //realistic  without fire consideration
        ttd = ambulanceUtilities.computeTTD(human).first();

        //realistic  without fire consideration
//        ttd= TODO:-------------------------


        int ttr;
        //Time To Refuge
        if (human.isPositionDefined()) {
            ttr = ambulanceUtilities.computeTimeToNearestAvailableRefuge(human.getPosition(world));
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

    public EntityID getMyTask() {
        return world.getTaskAssignment().get(world.getSelf().getID());
    }

    public int getTimeToNextAuction() {
        int timeToNextAuction = this.auctionPeriod - (world.getTime() - this.previuseAuctionTime);
        if (timeToNextAuction < 0) // if the leader slept
            return 0;
        else
            return timeToNextAuction;
    }

    public void leaderOperations(Set<StandardEntity> myGoodHumans, List<StandardEntity> healthyATs, AmbulanceStrategy strategy) {
        if ((world.getTime() - 1) % auctionPeriod == 0) {

            //clear previouse assigned tasks
            world.getTaskAssignment().clear();
            List<VictimAllocation> victimAllocations = computeNeededAT(myGoodHumans, healthyATs);

            findProperATforEachVictim(victimAllocations, healthyATs);

            if (strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED)) {
                //ambulanceMessageHelper.sendTasks();
            }

        } else if ((world.getTime() - 2) % auctionPeriod == 0) {

            if (strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED)) {
                //resend tasks to prevent message drops
                //ambulanceMessageHelper.sendTasks();
            }
        }

    }

    private void findProperATforEachVictim(List<VictimAllocation> victimAllocations, List<StandardEntity> healthyATs) {
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

            for (StandardEntity entity : healthyATs) {

                tta = ambulanceUtilities.approximatingTTM(entity, world.getEntity(victimAlloc.getVictimID()));
                ambulanceDistanceToVictim.add(new Pair<EntityID, Integer>(entity.getID(), tta));

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

    private List<VictimAllocation> computeNeededAT(Set<StandardEntity> myGoodHumans, List<StandardEntity> healthyATs) {

        Human human;
        int minNeed, maxNeed, i = 0;


        List<VictimAllocation> victimAllocations = new ArrayList<VictimAllocation>();


        allocateFaultyBidsToItsBidder(myGoodHumans, healthyATs);

        int numberOfAvailableAT = healthyATs.size();

//        addPlatoonAgentsToBadeHumans();

        for (EntityID entityID : world.getBadeHumans()) {

            human = (Human) world.getEntity(entityID);
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

    private void addPlatoonAgentsToBadeHumans() {
        for (EntityID agentID : world.getBuriedAgents()) {
            world.getBadeHumans().add(agentID);
        }

        //To change body of created methods use File | Settings | File Templates.
    }

    private void allocateFaultyBidsToItsBidder(Set<StandardEntity> myGoodHumans, List<StandardEntity> healthyATs) {
        List<EntityID> shouldAllocateToItsBidder = new ArrayList<EntityID>();
        for (EntityID entityID : world.getBadeHumans()) {
            if (!myGoodHumans.contains(world.getEntity(entityID)) || hasZeroBidValue(entityID)) {
                shouldAllocateToItsBidder.add(entityID);
            }
        }
        world.getBadeHumans().removeAll(shouldAllocateToItsBidder);
        ArrayList<Pair<EntityID, Integer>> bidPairs;
        int k = 0, count = 0;
        for (EntityID entityID : shouldAllocateToItsBidder) {
            bidPairs = world.getVictimBidsMap().get(entityID);
            if (bidPairs == null || bidPairs.isEmpty()) {
                continue;
            }

            Collections.sort(bidPairs, ConstantComparators.BID_VALUE_COMPARATOR);
            k = 0;
            count = 1;// maximum number of allocation for faulty bids
            while (count > 0 && k < healthyATs.size() && k < bidPairs.size()) {
                if (!world.getTaskAssignment().containsKey(bidPairs.get(k).first())) {
                    world.getTaskAssignment().put(bidPairs.get(k).first(), entityID);
                    healthyATs.remove(bidPairs.get(k).first());
                    count--;
                }
                k++;
            }


        }
    }

    private boolean hasZeroBidValue(EntityID entityID) {

        for (Pair<EntityID, Integer> bidPairs : world.getVictimBidsMap().get(entityID)) {
            if (bidPairs.second() == 0) {
                return true;
            }

        }
        return false;
    }

    /**
     * max number of needed AT for a victim is the number of agents who can rescue a damaged victim by lost of a minimum hp
     * currentDamage<=75 ===> lostHP=750
     * currentDamage>75 && currentDamage<=125    ===> lostHP=1000
     * currentDamage>125 ===> lostHP=1250
     *
     * @param human the victim to calculate its need to ATs
     * @return Maximum Needed Agent
     */
    protected int computeMaximumNeededAgent(Human human) {

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        int currentDamage = humanHelper.getCurrentDamage(human.getID());
        int maxCycleToRescue = 0;
        int maxNeededAgents = 0;
        byte i = 0;

        if (currentDamage <= 0) {
            currentDamage = 1;
        }

        if (currentDamage <= 75) { // max expected lost of hp is 750
            maxCycleToRescue = (int) Math.ceil(750f / currentDamage);
            i = 1;

        } else if (currentDamage > 75 && currentDamage <= 125) {  // max expected lost of hp is 1000
            maxCycleToRescue = (int) Math.ceil(1000f / currentDamage);
            i = 2;

        } else {// currentDamage >125  --- // max expected lost of hp is 1250
            maxCycleToRescue = (int) Math.ceil(1250f / currentDamage);
            i = 3;
        }

        maxNeededAgents = (int) Math.ceil((float) human.getBuriedness() / maxCycleToRescue);
        if (maxNeededAgents == 0) {
            maxNeededAgents++;
        }

//        if (i > 1) {
//            maxNeededAgents++;
//        }
        return maxNeededAgents;


    }

    protected int computeMaximumNeededAgent(EntityID humanID) {

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        Human human = (Human) world.getEntity(humanID);
        int currentDamage = humanHelper.getCurrentDamage(humanID);
        int maxCycleToRescue = 0;
        int maxNeededAgents = 0;
        byte i = 0;

        if (currentDamage <= 0) {
            currentDamage = 1;
        }

        if (currentDamage <= 75) { // max expected lost of hp is 750
            maxCycleToRescue = (int) Math.ceil(750f / currentDamage);
            i = 1;

        } else if (currentDamage > 75 && currentDamage <= 125) {  // max expected lost of hp is 1000
            maxCycleToRescue = (int) Math.ceil(1000f / currentDamage);
            i = 2;

        } else {// currentDamage >125  --- // max expected lost of hp is 1250
            maxCycleToRescue = (int) Math.ceil(1250f / currentDamage);
            i = 3;
        }

        maxNeededAgents = (int) Math.ceil((float) human.getBuriedness() / maxCycleToRescue);
        if (maxNeededAgents == 0) {
            maxNeededAgents++;
        }

//        if (i > 1) {
//            maxNeededAgents++;
//        }
        return maxNeededAgents;


    }

    public int getLastBidTime() {
        return lastBidTime;
    }

    public void clearPreviouseTasks() {
        if ((world.getTime() - 1) % auctionPeriod == 0) {

            //clear previouse assigned tasks
            world.getTaskAssignment().clear();
        }
    }

    public boolean shouldIPayAttentionToMyAssiggnedTask() {
//        if ((world.getTime() - 2) % auctionPeriod == 0 || (world.getTime() - 1) % auctionPeriod == 0) {
//            return true;
//        }
//        return false;

//        if(world.getBadeHumans().size()>0 && )
        return false;
    }

    /**
     * is there any one with bigger Id than me to go to check the predicted bad task
     *
     * @return true if I has the biggest ID in world.getTaskAssignment
     */
    public boolean haveIBiggestIDInAssignedTask() {
        for (EntityID entityID : world.getTaskAssignment().keySet()) {
            if (entityID.getValue() > world.getSelf().getID().getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * It will compute the cost of selecting a victim to rescue.
     * To simplify the solution, at first we consider ttf(timeToFreeForVictim) and ttm(timeToMove).
     *
     * @return cost
     */
    @Override
    public int computeCost(Human myCurrentTarget, EntityID victimID) {

        Human victim = (Human) world.getEntity(victimID);
        int cost = 0;
        cost += ambulanceUtilities.timeToFreeAT(myCurrentTarget) + ambulanceUtilities.approximatingTTA(victim);

        return cost;
    }

    @Override
    public void bidding(Human myTarget) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isBidTime() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRecieveBidsTime() {
        System.err.println("NOT Currect Function call");
        return false;
    }

    @Override
    public void taskAllocation() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean isAuctionTime() {
        if (world.getTime() % auctionPeriod != 0) {
            return false;
        } else {
            return true;
        }

    }


}
