package mrl.ambulance;

import mrl.MrlPersonalData;
import mrl.ambulance.federatedCoordinationStrategy.FederatedCoordinationStrategy;
import mrl.ambulance.marketLearnerStrategy.*;
import mrl.ambulance.structures.CivilianValue;
import mrl.ambulance.structures.RescuedCivilian;
import mrl.ambulance.structures.ShouldGetReward;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.helper.AmbulanceMessageHelper;
import mrl.helper.HumanHelper;
import mrl.helper.RoadHelper;
import mrl.partitioning.Partition;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.platoon.genericsearch.*;
import mrl.platoon.search.SearchHelper;
import rescuecore2.messages.Command;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * MRL ambulance team agent.
 */
public class MrlAmbulanceTeam extends MrlPlatoonAgent<AmbulanceTeam> implements MRLConstants {
    public static final String AMBULANCE_TEAM_COUNT_EXTENSION = ".atc";
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlAmbulanceTeam.class);

    private AmbulanceStrategy strategy = AmbulanceStrategy.PARTITION_BASED_COORDINATION;
    private LIA_StaticAuctioning lia_staticAuctioning;
    private LIA_StaticAuctioning_LA lia_staticAuctioning_la;
    private GroupBasedCoordination groupBasedCoordination;
    private PartitionBasedCoordination partitionBasedCoordination;

    //    private boolean useLearner = false;
//    private Civilian currentRescueCivilian = null;
//    private ArrayList<Civilian> mustLoadCivilians = new ArrayList<Civilian>();


    // if any civilian was in this list, then don't compute its TimeToRefuge again


    //LocalRescueLeader job : it will load and carry the rescued civilian to the nearest Refuge and Computing number of
    //ATs performed rescue action on the civilian
//    private int startRescueTime = -1; // the time of starting rescue action
    private int totalATsInThisRescue = 0;
    private int totalRescueTimeInThisRescue = 0;

    private double earnedReward = 0;

    RewardHelper rewardHelper = new RewardHelper();
    String actLog = "___________________________________________________\n";
    private Random ran_biased = new Random(2011);


    protected MrlAmbulanceTeamWorld world;
    AmbulanceUtilities ambulanceUtilities;
    public AmbulanceConditionChecker ambulanceConditionChecker;
    AmbulanceMessageHelper ambulanceMessageHelper;
    RoadHelper roadHelper;
    Auction auction;
    Learner learner;
    private int coef = 1;
    //    private boolean firstCycles = true;  //means no bids came yet frome the first cycle
//    private Civilian lastBestCivilanToSelect = null;
//    private double switchPanishment = 0.5;
//    private boolean needRescue = true;
    AmbulanceTeamState state_;
    private EntityID civToRefugeID;
    //    private HashSet<StandardEntity> rescuedCivilians = new HashSet<StandardEntity>();
    public Human myTarget;
    private Human tempTarget;
    private boolean needToExecute = true;
    private boolean shouldSetReward = false;
    private Area releaseTargetRoad;
    private int hpPrecision;
    private int damagePrecision;
    private ChangeSet changed;
    private boolean isTempTarget = true;
    private boolean wasUnreachableTarget = false;
    private FederatedCoordinationStrategy federatedCoordinationStrategy;
    private VictimClassifier victimClassifier;

    Partition myPartition;

    SearchHelper searchHelper;

    @Override
    public String toString() {
        return "MRL ambulance team ID: " + this.getID().getValue();
    }

    @Override
    protected void postConnect() {
        long startTime = System.currentTimeMillis();
        super.postConnect();

        this.world = (MrlAmbulanceTeamWorld) super.world;

        hpPrecision = config.getIntValue(HP_PRECISION);
        damagePrecision = config.getIntValue(DAMAGE_PRECISION);

        ambulanceUtilities = new AmbulanceUtilities(world);
        ambulanceConditionChecker = new AmbulanceConditionChecker(world, me());
        ambulanceMessageHelper = new AmbulanceMessageHelper(world, world.getPlatoonAgent(), this.getMessageManager());
        auction = new Auction(world, ambulanceMessageHelper, ambulanceUtilities);
        learner = new Learner(world);

//        possibleBuildingSearchDecisionMaker = new PossibleBuildingSearchDecisionMaker(world);
//        possibleBuildingSearchManager = new PossibleBuildingSearchManager(world, this, possibleBuildingSearchDecisionMaker, senseSearchStrategy);
        stupidSearchDecisionMaker = new StupidSearchDecisionMaker(world);
        stupidSearchManager = new StupidSearchManager(world, this, stupidSearchDecisionMaker, senseSearchStrategy);
        manualSearchDecisionMaker = new ManualSearchDecisionMaker(world);
//        manualSearchManager = new ManualSearchManager(world, this, manualSearchDecisionMaker, senseSearchStrategy);
        defaultSearchManager = new DefaultSearchManager(world, this, stupidSearchDecisionMaker, senseSearchStrategy);
        civilianSearchBBDecisionMaker = new CivilianSearchBBDecisionMaker(world);
        civilianSearchStrategy = new CivilianSearchStrategy(this, world);
        civilianSearchManager = new CivilianSearchManager(world, this, civilianSearchBBDecisionMaker, civilianSearchStrategy);

        simpleSearchDecisionMaker = new SimpleSearchDecisionMaker(world);
        simpleSearchManager = new SimpleSearchManager(world, this, simpleSearchDecisionMaker, civilianSearchStrategy);

        victimClassifier = new VictimClassifier(world);


        federatedCoordinationStrategy = new FederatedCoordinationStrategy(world, this, ambulanceUtilities, ambulanceConditionChecker, pathPlanner, victimClassifier);
        roadHelper = world.getHelper(RoadHelper.class);

        if (groupBasedCoordination == null) {
            groupBasedCoordination = new GroupBasedCoordination(world, ambulanceUtilities, ambulanceConditionChecker, victimClassifier);
        }

        if (partitionBasedCoordination == null) {
            partitionBasedCoordination = new PartitionBasedCoordination(world, ambulanceUtilities, ambulanceConditionChecker, victimClassifier);
        }

        if (world.isMapHuge()) {
            coef = 4;
        }


        searchHelper = new SearchHelper(world, world.getPlatoonAgent());
        storeNumberOfAgents(AMBULANCE_TEAM_COUNT_EXTENSION, world.getAmbulanceTeamList().size());

        long endTime = System.currentTimeMillis();
        System.out.println(this + "  connected ---->   total(" + (endTime - startTime) + "ms)");
    }


    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        super.think(time, changed, heard);
        this.changed = changed;
    }

    @Override
    public void act() throws CommandException, TimeOutException {

        // I'm buried and can't do anything
        if (me().getBuriedness() > 0 || stuckChecker.amITotallyStuck()) {
            //todo send buriedAgentMessage
            return;
        }

        if (myPartition == null) {
            myPartition = world.getPartitionManager().findHumanPartition(world.getSelfHuman());
        }

        victimClassifier.updateGoodHumanList(world.getCivilians(), getUnReachablePositions(), needToExecute);
        isThinkTimeOver("updateGoodHumanList");

        ambulanceUtilities.updateReadyAmbulances();
        ambulanceUtilities.updateEveryCycleInfos(victimClassifier.getMyGoodHumans());
        isThinkTimeOver("updateEveryCycleInfos");

        world.getShouldCheckBuildings().clear();
        ambulanceUtilities.updateCiviliansHP(victimClassifier.getMyGoodHumans(), hpPrecision, damagePrecision);
        world.getShouldCheckBuildings().addAll(ambulanceUtilities.findShouldCheckBuildings(victimClassifier.getMyGoodHumans()));
        needToExecute = false;
        isThinkTimeOver("updateCiviliansHP");

        List<StandardEntity> healthyATs = new ArrayList<StandardEntity>(ambulanceUtilities.getReadyAmbulances());
        strategyManager(healthyATs);//todo oooooooooooooooooooooooooooo

        //emphasizing on selecting a good target to do a work instead of searching
        for (int i = 0; i < 4; i++) {

            chooseStrategy(healthyATs);
            isThinkTimeOver("chooseStrategy");
            world.getFirstTimeSeenVictims().clear();

            preActs();
            isThinkTimeOver("preActs");

            if (DEBUG_AMBULANCE_TEAM) {
                System.out.println(world.getTime() + " " + world.getSelf().getID() + " MyTarget==>> " + myTarget);
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////

            postActs();
            isThinkTimeOver("post Acts");
        }


        rewardHelper.moveToSearch++;
        searching(world.isCommunicationLess());
        isThinkTimeOver("searching");

        moveToCenterPosition();
        isThinkTimeOver("moveToCenterPosition");

    }

    private void postActs() throws CommandException {

        if (myTarget == null) {

            rewardHelper.moveToSearch++;
            // Nothing to do
            Logger.info(" searching .....");
            MrlPersonalData.VIEWER_DATA.addAmbulanceTarget(me().getID(), null);
            searching(true);
        } else {

            if (myTarget instanceof Civilian) {
                // Civilian targets
                if (location().getID().equals(myTarget.getPosition())) {
                    // Targets in the same place might need rescuing or loading
                    if (ambulanceConditionChecker.loadCondition(myTarget)) {
                        // Load
                        if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                            System.out.println(world.getTime() + " " + world.getSelf().getID() + " Loading....... " + myTarget);
                        Logger.info("Loading " + myTarget);
                        civToRefugeID = myTarget.getID();
                        needToExecute = true;
                        ambulanceMessageHelper.sendLoaderMessage(civToRefugeID);
                        sendLoadAct(world.getTime(), myTarget.getID());
//                    return;
                    } else if (ambulanceConditionChecker.rescueCondition(myTarget)) {

//                    if (ambulanceUtilities.computeNumberOfVisibleATs() >= myTarget.getBuriedness()) {
                        if (ambulanceUtilities.timeToFreeForVictim(myTarget) <= 3) {
                            if (ambulanceConditionChecker.isLoader(myTarget) || world.getLoaders().contains(new Pair<EntityID, EntityID>(me().getID(), myTarget.getID()))) {
                                civToRefugeID = myTarget.getID();
                                ambulanceMessageHelper.sendLoaderMessage(civToRefugeID);
                                if (DEBUG_AMBULANCE_TEAM) {
                                    System.out.println(world.getTime() + " " + world.getSelf().getID() + "  NNNNNNNLLLLLLLL " + civToRefugeID);
                                }
                            }

                            //loadReward
                            earnedReward += RewardUtill.loadReward;
                            shouldSetReward = true;

//                        learner.setEarnedReward(earnedReward);
                        }
                        //rescueReward
                        earnedReward += RewardUtill.rescueReward;

                        if (shouldSetReward && strategy == AmbulanceStrategy.LEARNING) {
                            setRewardForTarget();

                        }
                        rewardHelper.rescueAct++;

                        // Rescue
                        Logger.info("Rescuing " + myTarget);
                        needToExecute = true;
                        sendRescueAct(world.getTime(), myTarget.getID());
//                    return;
                    } else {

                        rewardHelper.moveToSearch++;
                        if (myTarget != null && strategy == AmbulanceStrategy.LEARNING) {
                            setRewardForTarget();
                        }
                        victimClassifier.getSelectedHumans().add(myTarget);
                        victimClassifier.getMyGoodHumans().remove(myTarget);
//                    world.getLoaders().clear();
                        if (myTarget != null /*&& world.getTaskAssignment().get(me().getID()) != null && world.getTaskAssignment().get(me().getID()).equals(myTarget.getID())*/) {
//                        world.getTaskAssignment().remove(me().getID());
                            Map<EntityID, Task> tasks = world.getTaskLists().get(world.getSelf().getID());
                            if (tasks != null && !tasks.isEmpty()) {
                                if (tasks.get(myTarget.getID()) != null) {
                                    tasks.get(myTarget.getID()).setDone(true);
                                }
                            }


                        }
//                    myTarget = null;
                        // /TODO: select target again for maximum 3 times
//                    act();
//                    return;
                    }
                } else {

                    rewardHelper.moveToTarget++;

                    // moveReward
                    earnedReward += RewardUtill.movePunishment;

                    // Try to move to the target
                    StandardEntity se = myTarget.getPosition(world);
                    needToExecute = true;
                    if (se instanceof Area) {
                        move((Area) myTarget.getPosition(world), IN_TARGET, false);
                        //ambulanceMessageHelper.sendTargetToGoMessage(se.getID());
                        victimClassifier.addToUnreachableHumans(victimClassifier.getUnReachableHumans(), myTarget);
                        victimClassifier.getMyGoodHumans().remove(myTarget);
                        needToExecute = false;
                        myTarget = null;
//                    act();       //TODO: select target again for maximum 3 times
//                    return;

                    }
                }
            } else {
                // Agent Targets
                if (location().getID().equals(myTarget.getPosition())) {
                    // Targets in the same place might need rescuing or loading
                    if (ambulanceConditionChecker.rescueCondition(myTarget)) {

                        // Rescue
                        Logger.info("Rescuing " + myTarget);
                        needToExecute = true;
                        sendRescueAct(world.getTime(), myTarget.getID());
                    } else {
                        victimClassifier.getSelectedHumans().add(myTarget);
                        victimClassifier.getMyGoodHumans().remove(myTarget);
                        myTarget = null;
                        // /TODO: select target again for maximum 3 times

//                    act();
//                    return;
                    }
                } else {

                    // moveReward
                    earnedReward += RewardUtill.movePunishment;

                    // Try to move to the target
                    StandardEntity se = myTarget.getPosition(world);
                    needToExecute = true;
                    if (se instanceof Area) {
                        move((Area) myTarget.getPosition(world), IN_TARGET, false);
                        victimClassifier.addToUnreachableHumans(victimClassifier.getUnReachableHumans(), myTarget);
                        victimClassifier.getMyGoodHumans().remove(myTarget);
                        needToExecute = false;
                        myTarget = null;
                        // /TODO: select target again for maximum 3 times

//                    act();
//                    return;
                    }
//                return;

                }
            }
        }
    }

    private void strategyManager(List<StandardEntity> healthyATs) {
//        if (healthyATs.size() <= 5 || world.isCommunicationLess() || world.isCommunicationLimited()) {
//            strategy = AmbulanceStrategy.GROUP_BASED;
//        }
    }

    /**
     * consist of on board operations , move to refuge and so on
     *
     * @throws mrl.common.CommandException
     */
    private void preActs() throws CommandException {
        // Am I transporting a civilian to a refuge?
        Human tempTarget = ambulanceConditionChecker.someoneOnBoard();
        if (tempTarget != null || ambulanceConditionChecker.needToGoRefuge()) {

            if (tempTarget == null) {
                civToRefugeID = null;
            } else {
                civToRefugeID = tempTarget.getID();
            }
            if (civToRefugeID == null && location() instanceof Refuge) // last unloadOperation was not successful so do it again
            {
                sendUnloadAct(world.getTime());
            }


            // Am I at a refuge?
            if (location() instanceof Refuge && !ambulanceConditionChecker.needToGoRefuge()) {
                // Unload!
                ambulanceUtilities.setLastTraveledDistance(0);
                victimClassifier.getSelectedHumans().add(world.getEntity(civToRefugeID));
                unloadOperations(world.getTime());
            } else {

                Human civToRefuge = (Human) world.getEntity(civToRefugeID);
                if (civToRefugeID != null && civToRefuge != null && civToRefuge.getHP() == 0) {    // release dead civilian
                    civToRefugeID = null;
                    myTarget = null;
                    sendUnloadAct(world.getTime());
                }

                //Moving to Refuge
                ambulanceMessageHelper.sendTransportingCivilian(civToRefugeID);

                if (civToRefugeID != null && ambulanceConditionChecker.isReleaseCondition(civToRefugeID, world.getSelfPosition())) {

                    if (world.getRefuges().isEmpty()) {
                        ambulanceMessageHelper.sendRescuedCivilianMessage(civToRefugeID);
                    }
                    civToRefugeID = null;
                    myTarget = null;
                    sendUnloadAct(world.getTime());
                }

                // Move to a refuge
                if (!world.getRefuges().isEmpty()) {
                    pathPlanner.moveToRefuge();
                } else {
                    moveToNearestSimpleRoad();
                }

                moveToNearestSimpleRoad();

//                System.out.println("Problem on going to refuge, so try again");
//                if (tryCount >= 0) {
//                    tryCount--;
//                    act();
                return;
//                } else {
//                    return;
//                    do nothing
//                }
            }

        }

        if (civToRefugeID != null) {
            Human civToRefuge = (Human) world.getEntity(civToRefugeID);
            if (civToRefuge != null && ambulanceConditionChecker.loadCondition_messageBased(civToRefuge) /*ambulanceConditionChecker.loadCondition(civToRefuge)*/) {

                // Load
                if (MRLConstants.DEBUG_AMBULANCE_TEAM)
                    System.out.println(world.getTime() + " " + world.getSelf().getID() + " Loading....... " + civToRefugeID);
                needToExecute = true;
                world.getLoaders().clear();
                ambulanceMessageHelper.sendLoaderMessage(civToRefugeID);
                sendLoadAct(world.getTime(), civToRefugeID);

            }
        } else {
            civToRefugeID = null;
        }

        if (!strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED) && !strategy.equals(AmbulanceStrategy.MARKET_DISTRIBUTED)) {
            world.getLoaders().clear();
        }

    }


    /**
     * choose one strategy from RANDOM,GREEDY,CAOP,MARKET and LEARNING
     *
     * @param healthyATs list of healthy ambulanceTeams
     * @throws CommandException
     */
    private void chooseStrategy(List<StandardEntity> healthyATs) throws CommandException {
        //////////// CAOP Strategy
        if (strategy.equals(AmbulanceStrategy.CAOP)) {
            actingStrategy_CAOP();

            //////////// Market Centralized Leader Initiator Strategy
        } else if (strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED_LEADER_INITIATOR)) {
            actingStrategy_Market_Centralized_Leader_Initiator(healthyATs);

            //////////// Market Centralized Strategy  participant Initiator (default auctioning)
        } else if (strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED)) {
            actingStrategy_Market_Centralized(healthyATs);

            //////////// Market Distributed Strategy
        } else if (strategy.equals(AmbulanceStrategy.MARKET_DISTRIBUTED)) {
            actingStrategy_Market_Distributed(healthyATs);

            //////////// Random Biased
        } else if (strategy.equals(AmbulanceStrategy.RANDOM)) {
            actingStrategy_Random();

            //////////// Random Biased Strategy
        } else if (strategy.equals(AmbulanceStrategy.RANDOM_Biased)) {
            actingStrategy_Random_Biased();

            //////////// Greedy Strategy
        } else if (strategy.equals(AmbulanceStrategy.GREEDY)) {
            actingStrategy_Greedy();

            //////////// Learning Strategy
        } else if (strategy.equals(AmbulanceStrategy.MARKET_CENTRALIZED_LEADER_INITIATOR_WITH_LA)) {
            actingStrategy_Market_Centralized_Leader_Initiator_With_LA();

        } else if (strategy.equals(AmbulanceStrategy.FEDERATED_COORDINATION)) {
            actingStrategy_Federated_Coordination();
        } else if (strategy.equals(AmbulanceStrategy.GROUP_BASED)) {
            actingStrategy_GroupBased();
        } else if (strategy.equals(AmbulanceStrategy.PARTITION_BASED_COORDINATION)) {
            actingStrategy_Partitionbased();
        }

    }

    private void actingStrategy_Partitionbased() {
        myTarget = partitionBasedCoordination.getNextTarget(victimClassifier.getMyGoodHumans());
    }

    private void actingStrategy_GroupBased() {

        myTarget = groupBasedCoordination.getNextTarget(victimClassifier.getMyGoodHumans());

    }

    private void actingStrategy_Federated_Coordination() throws CommandException {
        federatedCoordinationStrategy.execute();
    }

    private void actingStrategy_Market_Centralized_Leader_Initiator(List<StandardEntity> healthyATs) {
//         if(LeaderInitiatorAuctionStrategy.EVERY_N_CYCLE)

        if (lia_staticAuctioning == null) {
            //lia_staticAuctioning = new LIA_StaticAuctioning(world, ambulanceMessageHelper, ambulanceUtilities);
        }

        auction.computeCAOPForHumans(victimClassifier.getMyGoodHumans(), victimClassifier.getMyBadHumans());
        if (myTarget != null && !victimClassifier.getMyGoodHumans().contains(myTarget)) {

            Map<EntityID, Task> tasks = world.getTaskLists().get(world.getSelf().getID());
            if (tasks != null && !tasks.isEmpty()) {
                if (tasks.get(myTarget.getID()) != null) {
                    tasks.get(myTarget.getID()).setDone(true);
                }
            }
            myTarget = null;
        }
        lia_staticAuctioning.startAuction(victimClassifier.getMyGoodHumans());
        lia_staticAuctioning.clearPreviouseTasks();
        lia_staticAuctioning.bidding(myTarget);
        lia_staticAuctioning.taskAllocation();
        selectTaskByConsideringFirstTimeVictims(lia_staticAuctioning);

    }

    private void actingStrategy_Market_Centralized_Leader_Initiator_With_LA() throws CommandException {

        ambulanceUtilities.chooseLeader();

        if (world.getTime() < 20) {
            return;
        }


        if (lia_staticAuctioning_la == null) {
//            lia_staticAuctioning_la = new LIA_StaticAuctioning_LA(world, ambulanceMessageHelper, ambulanceUtilities);
        }

        lia_staticAuctioning_la.readProbsFromFile();

        lia_staticAuctioning_la.updateLA();

        auction.computeCAOPForHumans(victimClassifier.getMyGoodHumans(), victimClassifier.getMyBadHumans());
        if (myTarget != null && !victimClassifier.getMyGoodHumans().contains(myTarget)) {

            Map<EntityID, Task> tasks = world.getTaskLists().get(world.getSelf().getID());
            if (tasks != null && !tasks.isEmpty()) {
                if (tasks.get(myTarget.getID()) != null) {
                    tasks.get(myTarget.getID()).setDone(true);
                }
            }
            myTarget = null;
        }
        lia_staticAuctioning_la.startAuction(victimClassifier.getMyGoodHumans());


        lia_staticAuctioning_la.clearPreviouseTasks();
        lia_staticAuctioning_la.bidding(myTarget);
        lia_staticAuctioning_la.taskAllocation();

        lia_staticAuctioning_la.updateLA(lia_staticAuctioning_la.getShouldCheckVictims());

        if (world.getTime() == 300) {
            lia_staticAuctioning_la.printProbsToFile();
        }

//        lia_staticAuctioning_la.getShouldCheckVictims()

        selectTaskByConsideringFirstTimeVictims(lia_staticAuctioning_la);

//            myTarget = learner.learningOperations(myGoodHumans, myTarget);
    }

    private void actingStrategy_Greedy() {
        if (!victimClassifier.getMyGoodHumans().contains(myTarget))
            myTarget = null;

        myTarget = (Human) selectTargetGreedily(victimClassifier.getMyGoodHumans(), myTarget);
    }

    private void actingStrategy_Random_Biased() {
        if (!victimClassifier.getMyGoodHumans().contains(myTarget))
            myTarget = null;

        myTarget = (Human) selectRandomTarget(victimClassifier.getMyGoodHumans(), myTarget, true);
    }

    private void actingStrategy_Random() {
        if (!victimClassifier.getMyGoodHumans().contains(myTarget))
            myTarget = null;

        myTarget = (Human) selectRandomTarget(victimClassifier.getMyGoodHumans(), myTarget, false);
    }

    private void actingStrategy_Market_Distributed(List<StandardEntity> healthyATs) {
        auction.computeCAOPForHumans(victimClassifier.getMyGoodHumans(), victimClassifier.getMyBadHumans());
        //if (!myGoodHumans.contains(myTarget))

        auction.startAuction(victimClassifier.getMyGoodHumans());
        auction.clearPreviouseTasks();

        if (myTarget != null && victimClassifier.getMyGoodHumans().contains(myTarget)) {
            return;
        }
        myTarget = null;

        auction.leaderOperations(victimClassifier.getMyGoodHumans(), healthyATs, strategy);


        //if I am loader, I should do the previouse job
        if (myTarget != null && world.isloader(myTarget.getID())) {
            return;
        }

//            System.out.println(world.getTime() + " " + world.getSelf().getID() + " ________________________________________________");
//            System.out.println("Tasks" + world.getTaskAssignment());

        if (auction.getMyTask() != null) {
            // get task which recieved from leader
            myTarget = (Human) world.getEntity(auction.getMyTask());
            if (!victimClassifier.getMyGoodHumans().contains(myTarget) /*&& !selectedHumans.contains(myTarget) || (!world.getLoaders().isEmpty() && !world.getLoaders().contains(new Pair<EntityID, EntityID>(me().getID(),myTarget.getID())))*/) {
                world.getLoaders().clear();
                myTarget = null;
            }
        }
        if (myTarget == null) {
            // get task based on CAOP
//                myTarget = (Human) selectMostValuableVictim(myGoodHumans);
        }
    }

    private void actingStrategy_Market_Centralized(List<StandardEntity> healthyATs) {


        victimClassifier.postponeBlockVictimTasks(victimClassifier.getMyGoodHumans());

        if (!world.getRefuges().isEmpty()) {// if isn't refugeless
            auction.computeCAOPForHumans(victimClassifier.getMyGoodHumans(), victimClassifier.getMyBadHumans());
        }
        if (world.maxID < 65535) {

            auction.startAuction(victimClassifier.getMyGoodHumans());
//        auction.clearPreviouseTasks();
            if (world.getAmbulanceLeaderID() != null && world.getAmbulanceLeaderID().equals(world.getSelf().getID())) {
                auction.leaderOperations(victimClassifier.getMyGoodHumans(), healthyATs, strategy);
            }
        }
        selectTaskByConsideringFirstTimeVictims(auction);

//        if (myTarget == null) {
//            get task based on CAOP
//                myTarget = (Human) selectTarget(myGoodHumans,myTarget);
//        }
        if (myTarget == null) {
            // get task based on CAOP
            myTarget = (Human) selectMostValuableVictim(victimClassifier.getMyGoodHumans());
//            myTarget = (Human) selectTarget(myGoodHumans, myTarget);
        }


//        System.out.println(world.getTime() + " TASK " + world.getSelf().getID() + " myTask: " + world.getTaskAssignment().get(me().getID()));
//        System.out.println(world.getTime() + " TARGET " + world.getSelf().getID() + " myTarget: " + myTarget);

    }


    private void selectTaskByConsideringFirstTimeVictims(IAuction iAuction) {

        if (myTarget != null && victimClassifier.getMyGoodHumans().contains(myTarget)) {
            if (isTempTarget && iAuction.getMyTask() == null /*&& !auction.shouldIPayAttentionToMyAssiggnedTask()*/) {
                world.getFirstTimeSeenVictims().clear();
                if (ambulanceUtilities.isAlivable(myTarget)) {
                    return;
                }
            } else if (!isTempTarget /*&& myTarget.getID().equals(iAuction.getMyTask())*/) {

                //iAuction.getMyTask() != null
                //todo
                world.getFirstTimeSeenVictims().clear();
                if (ambulanceUtilities.isAlivable(myTarget)) {
                    return;
                }
            } else {
                world.getFirstTimeSeenVictims().clear();
            }

        }

        myTarget = null;

        // don't waste time to wait to recieve a task from leader, start to rescue the
        // first time seen victim by myself until reciving an order to rescue a victim or search
        Map<EntityID, Task> myTasks = world.getTaskLists().get(world.getSelf().getID());
        if (!world.getFirstTimeSeenVictims().isEmpty() && (myTasks == null || myTasks.isEmpty())) {

            if (selectTargetFromFirstTimeSeenVictims(victimClassifier.getMyGoodHumans())) {
                if (ambulanceUtilities.isAlivable(myTarget)) {
                    return;
                }
            } else {
                world.getFirstTimeSeenVictims().clear();
            }
        }

//            if I am loader, I should do the previouse job
//            if (myTarget != null && world.isloader(myTarget.getID())) {
//                return;
//            }

//        System.out.println(world.getTime() + " " + world.getSelf().getID() + " ________________________________________________");

        if (iAuction.getMyTask() != null) {
            // get task which recieved from leader
            myTarget = (Human) world.getEntity(iAuction.getMyTask());
            tempTarget = myTarget;
            if (!victimClassifier.getMyGoodHumans().contains(myTarget)) {
                myTarget = null;
            }

            /*if (!myGoodHumans.contains(myTarget) *//*&& !selectedHumans.contains(myTarget) || (!world.getLoaders().isEmpty() && !world.getLoaders().contains(new Pair<EntityID, EntityID>(me().getID(),myTarget.getID())))*//*) {
                world.getLoaders().clear();
                myTarget = null;
                if (iAuction.haveIBiggestIDInAssignedTask()) {
                    myTarget = tempTarget;
                } else {
                    tempTarget = null;
                }

            }*/

        }
        if (myTarget == null) {
            // get task based on CAOP
//                myTarget = (Human) selectMostValuableVictim(myGoodHumans);
        }


        isTempTarget = false;
        if (myTarget != null) {
            if (ambulanceUtilities.isAlivable(myTarget)) {
                return;
            }
        }

        // don't waste time to wait to recieve a task from leader, start to rescue the
        // first time seen victim by myself until reciving an order to rescue a victim or search
        if (!world.getFirstTimeSeenVictims().isEmpty()) {

            selectTargetFromFirstTimeSeenVictims(victimClassifier.getMyGoodHumans());
        }
        world.getFirstTimeSeenVictims().clear();

    }

    private Human getBetterBetween(StandardEntity myTarget, StandardEntity myTask) {
        Human myHumanTask = (Human) myTask;
        if (!victimClassifier.getMyGoodHumans().contains(myHumanTask)) {
            return (Human) myTarget;
        }

        if (isOnFire(myHumanTask) && ambulanceUtilities.approximatingTTA(myHumanTask) > 5) {
            return (Human) myTarget;
        }
        if (isOnFire((Human) myTarget) && ambulanceUtilities.approximatingTTA((Human) myTarget) > 5) {
            return myHumanTask;

        }


        if (myTarget.getID().equals(myTask.getID())) {
            return myHumanTask;
        } else {
            int timeToArrive;
            int nOAR;
            int minForHuman;//minimum Number Of Needed Agent For human
            int minForMyTarget;//minimum Number Of Needed Agent For MyTarget
            HumanHelper hH = world.getHelper(HumanHelper.class);

            //Approximating Time To Arrive to human
            timeToArrive = ambulanceUtilities.approximatingTTA(myHumanTask);
            nOAR = hH.getNumberOfATsRescuing(myHumanTask.getID());
            if (nOAR != 0 && myHumanTask.getBuriedness() / nOAR > timeToArrive + 3) {
                return myHumanTask;
            } else if (nOAR == 0) {

                minForHuman = auction.computeMinimumNeededAgent(myHumanTask);
                minForMyTarget = auction.computeMinimumNeededAgent((Human) myTarget);

//                    System.out.println(">>>> minimumNeededAgent"+world.getTime()+" "+world.getSelf().getID()+" "+ minForMyTarget+" "+minForHuman);
                if (minForHuman > minForMyTarget || hH.getCAOP(myHumanTask.getID()) >= 30) {
                    return myHumanTask;
                } else {
                    return (Human) myTarget;
                }

            } else {
                return (Human) myTarget;
            }
        }
    }

    private void actingStrategy_CAOP() {
        auction.computeCAOPForHumans(victimClassifier.getMyGoodHumans(), victimClassifier.getMyBadHumans());
        if (!victimClassifier.getMyGoodHumans().contains(myTarget))
            myTarget = null;

        myTarget = (Human) selectTarget(victimClassifier.getMyGoodHumans(), myTarget);
    }

    /**
     * select a Target From First Time Seen Victims
     *
     * @param myGoodHumans
     * @return true if there exist a good target from first Time Seen victims
     */
    private boolean selectTargetFromFirstTimeSeenVictims(Set<StandardEntity> myGoodHumans) {
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        double maxCAOP = -1;
        double tempCAOP = 0;
        StandardEntity bestLocalVictim = null;
        for (StandardEntity standardEntity : world.getFirstTimeSeenVictims()) {
            tempCAOP = humanHelper.getCAOP(standardEntity.getID());
            if (myGoodHumans.contains(standardEntity) && tempCAOP > maxCAOP) {
                maxCAOP = tempCAOP;
                bestLocalVictim = standardEntity;
            }

        }
        myTarget = (Human) bestLocalVictim;
        tempTarget = myTarget;
        if (myTarget != null) {
            isTempTarget = true;
            return true;
        }

        return false;
    }


    private void moveToCenterPosition() throws CommandException {
        Area area = (Area) world.getSelfPosition();
        moveToPoint(area.getID(), area.getX(), area.getY());
    }

    private StandardEntity selectMostValuableVictim(Set<StandardEntity> myGoodHumans) {
        for (StandardEntity standardEntity : myGoodHumans) {
            if (isGoodToSelect(standardEntity)) {
                if (ambulanceUtilities.isAlivable((Human) standardEntity)) {
                    return standardEntity;
                }

            }
        }
        return null;
    }

    /**
     * it shows weather it is good to move to this target based on arrive time and next auction
     * time
     *
     * @param standardEntity the victim to check
     * @return good or bad
     */
    private boolean isGoodToSelect(StandardEntity standardEntity) {

        int timeToArrive = world.getHelper(HumanHelper.class).getTimeToArrive(standardEntity.getID());
        if (timeToArrive <= coef * 15 /*Math.min(5, auction.getTimeToNextAuction())*/ && !anyAssignment(standardEntity)) {
            return true;
        } else {
            return false;
        }

    }

    /**
     * is any ambulanceTeam assigned to it or not?
     *
     * @param standardEntity the victim to check its assgnment
     * @return has any assignment or not?
     */
    private boolean anyAssignment(StandardEntity standardEntity) {
        return world.getTaskAssignment().values().contains(standardEntity.getID());
    }

    private void moveToNearestSimpleRoad
            () throws CommandException {


        Area selfPosition = (Area) world.getSelfPosition();
        Area target = null;
        Area meanTaret = null;
        Area neighbour;
        Area innerNeighbour;
        RoadHelper roadHelper = world.getHelper(RoadHelper.class);

        if (this.releaseTargetRoad != null) {
            if (!world.getSelfPosition().getID().equals(this.releaseTargetRoad.getID())) {
                target = this.releaseTargetRoad;
                move(this.releaseTargetRoad, IN_TARGET, false);
            } else {
                this.releaseTargetRoad = null;
            }
        }


        for (EntityID neighbourId : selfPosition.getNeighbours()) {
            neighbour = (Area) world.getEntity(neighbourId);
            if (neighbour instanceof Road) {
                meanTaret = neighbour;
                if (roadHelper.isPassable(neighbourId)) {
                    for (EntityID id : neighbour.getNeighbours()) {
                        innerNeighbour = (Area) world.getEntity(id);
                        if (innerNeighbour instanceof Road) {
                            if (roadHelper.isPassable(id)) {
                                if (RoadHelper.getConnectedBuildings(world, (Road) innerNeighbour).size() == 0) {
                                    target = innerNeighbour;
                                    break;
                                } else {
                                    meanTaret = innerNeighbour;
                                }
                            }
                        }
                    }
                    if (target != null) {
                        break;
                    }
                }
            }
        }

        if (target != null) {
            this.releaseTargetRoad = target;
            move(target, IN_TARGET, false);
        } else if (meanTaret != null) {
            this.releaseTargetRoad = meanTaret;
            move(meanTaret, IN_TARGET, false);
        }
    }


    private void setRewardForTarget
            () {
        ShouldGetReward shouldGetReward;
        shouldGetReward = learner.getShouldGetRewardList().get(myTarget.getID());
        shouldGetReward.setComputedReward(earnedReward);
        learner.getShouldGetRewardList().put(myTarget.getID(), shouldGetReward);
        if (DEBUG_AMBULANCE_TEAM) {
            System.out.println(world.getSelf().getID() + "RRRRRRRRRRRRRRRR >>> " + earnedReward);
        }
        earnedReward = 0;
        shouldSetReward = false;
    }

    @Override
    public void processMessage(MessageEntity messageEntity) {
        try {
            ambulanceMessageHelper.processMessage(messageEntity);
        } catch (Exception ex) {
            world.printData(ex.getMessage());
        }
    }

    private StandardEntity selectTargetGreedily(Set<StandardEntity> myGoodHumans, StandardEntity
            myTarget) {
        if (myTarget != null) {
            return myTarget;
        }
        if (myGoodHumans.isEmpty()) {
            return null;
        }

        ArrayList<CivilianValue> civilianTTAs;
        civilianTTAs = sortHumansBasedOnTTA(myGoodHumans);

        if (civilianTTAs.isEmpty()) {
            return null;
        }

        return world.getEntity(civilianTTAs.get(0).getId());


    }

    private ArrayList<CivilianValue> sortHumansBasedOnTTA(Set<StandardEntity> myGoodHumans) {
        int tta;
        Pair<Integer, Integer> ttd;
        ArrayList<CivilianValue> civilianTTAs = new ArrayList<CivilianValue>();
        CivilianValue civilianTTA;

        for (StandardEntity standardEntity : myGoodHumans) {
            Human human = (Human) standardEntity;
//            ttd = ambulanceUtilities.computeTTD(human);
            if (ambulanceUtilities.isAlivable((Human) standardEntity)) {
                // it will be alive
                tta = ambulanceUtilities.approximatingTTA(human);
                civilianTTA = new CivilianValue(human.getID(), tta);
                civilianTTAs.add(civilianTTA);
            }

//            if (ttd.second() >= 250 || ttd.first() < 0) {
//                it will be alive
//                tta = ambulanceUtilities.approximatingTTA(human);
//                civilianTTA = new CivilianValue(human.getID(), tta);
//                civilianTTAs.add(civilianTTA);
//            }
        }

        Collections.sort(civilianTTAs);

        return civilianTTAs;
    }

    private StandardEntity selectRandomTarget(Set<StandardEntity> myGoodCivilians, StandardEntity myTarget, boolean biased) {

        if (myTarget != null) {
            return myTarget;
        }
        if (myGoodCivilians.isEmpty()) {
            return null;
        }

        int randNum;
        if (biased) {
            randNum = ran_biased.nextInt(myGoodCivilians.size());
        } else {
            randNum = random.nextInt(myGoodCivilians.size());
        }

        int i = 0;
        for (StandardEntity entity : myGoodCivilians) {
            if (i == randNum) {
                return entity;
            }
            i++;
        }

        return null;
    }

    private StandardEntity selectTarget(Set<StandardEntity> myGoodHumans, Human myTarget) {

        int timeToArrive;
        int nOAR;
        int minForHuman;//minimum Number Of Needed Agent For human
        int minForMyTarget;//minimum Number Of Needed Agent For MyTarget
        HumanHelper hH = world.getHelper(HumanHelper.class);
        Human humanToSelect;
        boolean isOnFire;
        for (StandardEntity standardEntity : myGoodHumans) {
            Human human = (Human) standardEntity;
            timeToArrive = ambulanceUtilities.approximatingTTA(human);
            isOnFire = isOnFire(human);
            if (myTarget == null) {
                //Approximating Time To Arrive to human
                nOAR = hH.getNumberOfATsRescuing(human.getID());
                if (!anyAssignment(human) && (nOAR == 0 || human.getBuriedness() / nOAR > timeToArrive + 3)) {
                    if (!isOnFire) {
                        return human;
                    } else if (timeToArrive <= 5) {
                        return human;
                    }

                }/* else if (anyAssignment(human) && !canSeeAnyAssignedAT(human)) {
                    if (!isOnFire) {
                        return human;
                    } else if (timeToArrive <= 5) {
                        return human;
                    }

                }*/

            } else if (myTarget.getID().equals(human.getID())) {

                if (!isOnFire) {
                    return human;
                } else if (timeToArrive <= 5) {
                    return human;
                }

            } else {

                //Approximating Time To Arrive to human
                nOAR = hH.getNumberOfATsRescuing(human.getID());
//            workTimeOnNextTarget = (human.getBuriedness() - nOAR * timeToArrive) / (nOAR + 1);
//
//            nOAR = hH.getNumberOfATsRescuing(myTarget.getID());
//            if (nOAR == 0) {
//                nOAR = 1;

//            }

                if (nOAR != 0 && human.getBuriedness() / nOAR > timeToArrive + 3) {
                    if (!isOnFire) {
                        return human;
                    } else if (timeToArrive <= 5) {
                        return human;
                    }

                } else if (nOAR == 0) {

                    minForHuman = auction.computeMinimumNeededAgent(human);
                    minForMyTarget = auction.computeMinimumNeededAgent(myTarget);

//                    System.out.println(">>>> minimumNeededAgent"+world.getTime()+" "+world.getSelf().getID()+" "+ minForMyTarget+" "+minForHuman);
                    if (minForHuman > minForMyTarget || hH.getCAOP(human.getID()) >= 30) {
                        if (!isOnFire) {
                            return human;
                        } else if (timeToArrive <= 5) {
                            return human;
                        }

                    }

                }
            }
        }

        return myTarget;

    }

    private boolean isOnFire(Human human) {
        Building building = (Building) world.getEntity(human.getPosition());
        return building.isFierynessDefined() && building.getFieryness() != 0 && building.getFieryness() != 4;
    }

    private boolean canSeeAnyAssignedAT
            (Human
                     human) {
        for (EntityID agentID : world.getTaskAssignment().keySet()) {
            if (world.getTaskAssignment().get(agentID).equals(human.getID())) {
                if (ambulanceConditionChecker.isVisible(agentID)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void searching(boolean inPartition) throws CommandException {
        needToExecute = true;

        state_ = AmbulanceTeamState.Exploring;
        setAgentState(State.SEARCHING);
        civilianSearchBBDecisionMaker.setSearchInPartition(inPartition);
        civilianSearchManager.execute();
        simpleSearchDecisionMaker.setSearchInPartition(inPartition);
        simpleSearchManager.execute();
        searchHelper.breadthFirstSearch(inPartition);
    }

    private void unloadOperations
            (
                    int time) throws CommandException {
        Logger.info("Unloading");
        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println(world.getTime() + " " + me().getID() + "Unload Operations>>>>>>>>>>>>>> " + civToRefugeID);
        }
        world.getRescuedCivilians().add(civToRefugeID);

        //todo message this civ as rescued by rescuedCivilianPacket
//        sendRescuedCivilianMessage(currentRescueCivilian);
        ambulanceMessageHelper.sendRescuedCivilianMessage(civToRefugeID);

        Human human = (Human) world.getEntity(civToRefugeID);
        RescuedCivilian rescuedCivilian = new RescuedCivilian();
        rescuedCivilian.setAmbulanceID(world.getSelf().getID());
        rescuedCivilian.setCivilianId(civToRefugeID);
        if (human != null) {
            rescuedCivilian.setHP(human.getHP());
        } else {
            rescuedCivilian.setHP(-10000);
        }
        rescuedCivilian.setTotalATsInThisRescue(totalATsInThisRescue);
        rescuedCivilian.setTotalRescueTime(totalRescueTimeInThisRescue);
        world.getCurrentlyRescuedCivilians().add(rescuedCivilian);


        civToRefugeID = null;
        myTarget = null;
//        currentRescueCivilian = null;
//        shouldRescueCivilian = null;
        totalATsInThisRescue = 0;
        totalRescueTimeInThisRescue = 0;
//        amILocalRescueLeader = false;
//        isThereLocalRescueLeader = false;
//        startRescueTime = -1;
        state_ = AmbulanceTeamState.Unloading;
        sendUnloadAct(time);
    }


    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum
            () {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }
}