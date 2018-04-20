package mrl.ambulance.marketLearnerStrategy;

import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.ambulance.structures.Bid;
import mrl.ambulance.structures.RescuedCivilian;
import mrl.ambulance.structures.ShouldGetReward;
import mrl.helper.HumanHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 27, 2010
 * Time: 7:32:15 PM
 */
public class Learner {

    private MrlAmbulanceTeamWorld world;
    private double qTable[][];
    private double alpha = 0.3; // is learning rate
    private double gama = 0.2; // is discounted factor
    private int stateValue = -1;
    private int actionValue = -1;
    // it keeps this AT's best state value(valueFunction) in each time cycle like   HashMap<time,bestStateValue>
    private HashMap<Integer, Integer> stateValuesPerCycle = new HashMap<Integer, Integer>();
    AmbulanceUtilities ambulanceUtilities;
    private Map<EntityID, ShouldGetReward> shouldGetRewardList = new HashMap<EntityID, ShouldGetReward>();
    Map<Integer, ArrayList<EntityID>> civilianClusters = new HashMap<Integer, ArrayList<EntityID>>();
//    LearnerIO learnerIO;

    private double earnedReward = 0;

    ////////getters & setters

    public double getEarnedReward() {
        return earnedReward;
    }

    public void setEarnedReward(double earnedReward) {
        this.earnedReward = earnedReward;
    }

    public int getStateValue() {
        return stateValue;
    }

    public Map<EntityID, ShouldGetReward> getShouldGetRewardList() {
        return shouldGetRewardList;
    }


    public Learner(MrlAmbulanceTeamWorld world) {
        this.world = world;
        ambulanceUtilities = new AmbulanceUtilities(world);

        // 5 states and 5 action types
        // s1 : tta <=1
        // s2 : 2<= tta <=5
        // s3 : 6<= tta <=10
        // s4 : 11<= tta <15
        // s5 : 16<= tta <=20
        qTable = new double[5][5];
//        learnerIO = new LearnerIO("C:\\mytest\\" + world.getSelf().getID() + ".txt", true);


    }

    public void clusterCiviliansIntoStates(ArrayList<Civilian> civilians) {
        int civilianCluster;
        civilianClusters.clear();

        for (Civilian civilian : civilians) {
            civilianCluster = verifyingStateType(civilian);

            ArrayList<EntityID> clusteredCivilians = civilianClusters.get(civilianCluster);
            if (clusteredCivilians == null) {
                clusteredCivilians = new ArrayList<EntityID>();
            }
            clusteredCivilians.add(civilian.getID());
            civilianClusters.put(civilianCluster, clusteredCivilians);
        }

    }


    /**
     * use e-greedy to find the next best state
     *
     * @return most beneficial predicted state
     */
    public int getNextBestAction() {

        Random random = new Random();
        random.setSeed(22);

        if (this.stateValue == -1) {
            return random.nextInt(5);
        }


        int bestAction = findActionWithGraterValueOfThisState(qTable);

        // low Probability Actions means ones except bestAction
        ArrayList<Integer> lowProbabilityActions = new ArrayList<Integer>();
        for (int i = 0; i <= 4; i++)
            if (i != bestAction)
                lowProbabilityActions.add(i);

        double a = random.nextDouble();
        if (a <= 0.99) {
            // look up in bidCivilians list an find the civilian with this Emergency level and
            // if there is more than one, then select one with highest benefit. If there is no civilian with this
            // Emergency level then select one with highest benefit
            return bestAction;

        } else {
            return lowProbabilityActions.get(random.nextInt(4));
            //todo check random.nextInt(4) range
        }


    }


    private EntityID selectTarget() {
        EntityID civilianID = null;

        this.stateValue = this.actionValue;

        for (int i = 0; i <= 100; i++) {

            int bestAction = getNextBestAction();

            ArrayList<EntityID> clusteredCivilians = civilianClusters.get(bestAction);
            if (!(clusteredCivilians == null || clusteredCivilians.isEmpty())) {
                civilianID = clusteredCivilians.get(0);
                this.actionValue = bestAction;
                break;
            }
        }


        return civilianID;

    }


    public Civilian learningOperations(ArrayList<Civilian> myGoodCivilians, Civilian myTarget) {

        if (!world.getCurrentlyRescuedCivilians().isEmpty()) // check proper list in messaging
        {
            //todo:   for each rescued civilian Update related entry of Q table
            // use  ShouldGetRewardList , increase thir reward with saveReward = 5
            //
            ShouldGetReward shouldGetReward = null;
            for (RescuedCivilian rescuedCivilian : world.getCurrentlyRescuedCivilians()) {
                shouldGetReward = shouldGetRewardList.get(rescuedCivilian.getCivilianId());
                if (shouldGetReward != null) { // I was participated in this rescue, so I should get reward and update Q-table
                    shouldGetReward.setComputedReward(shouldGetReward.getComputedReward() + RewardUtill.saveReward);
                    //update previous table entry by this new state that verified above
                    updateQTableEntry(shouldGetReward.getStateValue(), shouldGetReward.getActionValue(), shouldGetReward.getComputedReward(), shouldGetReward.getNextStateBestValue());
                }

                // add to permanent list
                world.getRescuedCivilians().add(rescuedCivilian.getCivilianId());
            }
            // clear temp list
            world.getCurrentlyRescuedCivilians().clear();

        }

        // means target is dead or loaded
        if (myTarget != null && !world.getRescuedCivilians().contains(myTarget.getID()) && !myGoodCivilians.contains(myTarget)) { //todo: smeans should use RewardUtill.deadPunishment

            if (myTarget.getBuriedness() != 0) // target is dead
            {
                ShouldGetReward shouldGetReward = null;
                shouldGetReward = shouldGetRewardList.get(myTarget.getID());
                if (shouldGetReward != null) { // I was participated in this rescue, so I should get reward and update Q-table
                    shouldGetReward.setComputedReward(shouldGetReward.getComputedReward() + RewardUtill.deadPunishment);
                    //update previous table entry by this new state that verified above
                    updateQTableEntry(shouldGetReward.getStateValue(), shouldGetReward.getActionValue(), shouldGetReward.getComputedReward(), shouldGetReward.getNextStateBestValue());
                }

            }

            myTarget = null;

            //todo: should Update Q table with  RewardUtill.deadPunishment and
            // register in ShouldGetRewardList
        }


        if (myTarget != null)
            return myTarget;

        if (myGoodCivilians.isEmpty())
            return null;

        EntityID tempCivilian = null;
        clusterCiviliansIntoStates(myGoodCivilians);

        tempCivilian = selectTarget();

        if (tempCivilian == null) {
            System.out.println("(tempCivilian==null)   !!!!!!!!!!!");
        }

        addToShouldGetRewardList(tempCivilian);

        return (Civilian) world.getEntity(tempCivilian);

    }


//    public void updateQTable() {
//
//        for (RescuedCivilian rescuedCivilian : world.getCurrentlyRescuedCivilians()) {
//            for (ShouldGetReward shouldGetReward : shouldGetRewardList) {
//                if (rescuedCivilian.getHumanId().equals(shouldGetReward.getCivilianID().getID())) {
//
//                    int rescueTimeOfMe = computeNumberOfRescueTimesOfMeForThisCivilian(shouldGetRewardList, shouldGetReward);
////                    int totalRescueTime = rescuedCivilian.getTotalRescueTime();
////                    int totalATsInThisRescue = rescuedCivilian.getTotalATsInThisRescue();
//                    Pair<Integer, Integer> totalRescueTimeAndTotalATsInRescue =
//                            computeTotalRescueTimeAndTotalATInRescueFor(rescuedCivilian.getHumanId(), world.getTime());
//                    int totalRescueTime = totalRescueTimeAndTotalATsInRescue.first();
//                    int totalATsInThisRescue = totalRescueTimeAndTotalATsInRescue.second();
//
//                    int stNum = shouldGetReward.getStateValue();
//                    int emergencyLevel = world.getHelper(CivilianHelper.class).getEmergencyLevel(shouldGetReward.getCivilianID().getID());
//                    int selectTime = shouldGetReward.getSelectTime();
//                    int lastTravelDistance = shouldGetReward.getLastTraveledDistanceToArrive();
//
//                    double reward = RewardUtill.computeReward(world, rescuedCivilians, rescueTimeOfMe, totalRescueTime, totalATsInThisRescue, lastTravelDistance);
//
//
//                    //update previous table entry by this new state that verified above
//                    updateQTableEntry(stNum, emergencyLevel, reward);
//
//
//                }
//            }
//        }
//
//
//    }

    private Pair<Integer, Integer> computeTotalRescueTimeAndTotalATInRescueFor(EntityID civilianId, int unloadTime) {

        int firstTime = 1000;
        int num = 0;
        for (StandardEntity standardEntity : world.getAmbulanceTeams()) {
            AmbulanceTeam at = (AmbulanceTeam) standardEntity;
            Pair<Integer, Integer> startCurrentTimePair =
                    world.getAmbulanceCivilianMap().get(new Pair<EntityID, EntityID>(at.getID(), civilianId));
            if (startCurrentTimePair != null) {
                num++;
                if (startCurrentTimePair.first() < firstTime) {
                    firstTime = startCurrentTimePair.first();
                }
            }
        }

        if (num == 0)
            System.out.println("!!!!!!!! SO WHERE AM I? --- Means my message didn't arrive to me.");
        return new Pair<Integer, Integer>(unloadTime - firstTime, num);
    }

    /**
     * update Q-Table Entry by the below formula
     * Q(s,a) = Q(s,a) + alpha*(Reward' + gama*MaxQ(s') + Q(s,a))
     *
     * @param stNum         is next state number
     * @param action        is as next selected action
     * @param reward        reward af this action in this state
     * @param valueFunction the biggest value of next selected state
     */
    public void updateQTableEntry(int stNum, int action, double reward, double valueFunction) {


        // we don,t need the bellow instruction, because it recieved it from itself in the past cycles
        // double maxStateValue = findMaxValueOfThisState(qTable, stNum);

//  todo      double meanValueOfAllAgentsStates = computeMeanValueOfAllAgentsRecievedValues(selectTime + 1);
        if (stNum != -1 & action != -1) {
            double value = (1 - alpha) * qTable[stNum][action] + alpha * (reward + gama * valueFunction/* meanValueOfAllAgentsStates*/);
            qTable[stNum][action] = (value);
        }

//        learnerIO.writeTableToFile(qTable, stNum, action, reward, valueFunction, world.getTime());

    }


//    public int computeAndKeepValueFunction(int time, Civilian currentRescueCivilian, boolean firstCycles) {
    //Verify the type of new State in which I am Working, for Q-Table

//        int bestStateValue = findMaxValueOfThisAction(qTable, stateValue);
//
//
//         keep this stateValue for myself
//        stateValuesPerCycle.put(time, bestStateValue);
//
//        return bestStateValue;
//    }


    public void addToShouldGetRewardList(EntityID civilianID) {

//        ShouldGetReward shouldGetReward=shouldGetRewardList.get(shouldRescueCivilian.getID());
//        int totalRescueTimeOfMeForThisCivilian=shouldGetReward.getTimeInRescue();


        if (shouldGetRewardList.containsKey(civilianID))
            return;

        ShouldGetReward shouldGetReward = new ShouldGetReward();

        shouldGetReward.setVictimID(civilianID);
        shouldGetReward.setStateValue(this.stateValue);
        shouldGetReward.setActionValue(this.actionValue);
        shouldGetReward.setNextStateBestValue(findMaxValueOfThisAction(qTable, this.actionValue));
        shouldGetReward.setComputedReward(0);

        shouldGetRewardList.put(civilianID, shouldGetReward);


    }


    /**
     * Verify the type of human state relative to me, for Q-Table
     * <s1,s2,s3,s4,s5>=
     * // s1 : tta <=1
     * // s2 : 2<= tta <=5
     * // s3 : 6<= tta <=10
     * // s4 : 11<= tta <=15
     * // s5 : 16<= tta <=20
     *
     * @param civilian civilian to compute its state
     * @return state type number
     */
    public int verifyingStateType(Civilian civilian) {

        int ttr = ambulanceUtilities.approximatingTTA(civilian);

        if (ttr <= 1)
            return 1;
        else if (ttr <= 5)
            return 2;
        else if (ttr <= 10)
            return 3;
        else if (ttr <= 15)
            return 4;
        else return 5;
    }

    private double computeMeanValueOfAllAgentsRecievedValues(int selectTime) {

//        world.getValueFunctions().get(selectTime+1).add(maxStateValue);


        double sum = stateValuesPerCycle.get(selectTime);// get my state value of
        if (world.getValueFunctions().get(selectTime) == null)
            return sum;
        for (int value : world.getValueFunctions().get(selectTime)) {
            sum += value;
        }

        return sum / world.getValueFunctions().get(selectTime).size();
    }


//    public Civilian findOneWithStateOf(int bestAction) {
//        ArrayList<Bid> candidateBids = new ArrayList<Bid>();
//        for (Bid bid : world.getBids()) {
//            Civilian civilian = bid.getCivilian(world);
//            if (bestAction == world.getHelper(HumanHelper.class).getEmergencyLevel(civilian.getID()))
//                candidateBids.add(bid);
//        }
//
//        if (candidateBids.isEmpty()) {
//            return findMostBeneficialOne(world.getBids());
//
//        } else if (candidateBids.size() == 1) {
//            return candidateBids.get(0).getCivilian(world);
//        } else {
//            return findMostBeneficialOne(candidateBids);
//        }
//
//    }

    public Civilian findMostBeneficialOne(ArrayList<Bid> bidCivilians) {

        double bestBenefit = 0;
        Civilian bestCivilian = null;

        System.out.println("FFFFFFF biCivilian.Size() in findMostBeneficialOne:" + world.getBids().size());
        for (Bid bid : bidCivilians) {
            System.out.println("");
            Civilian civilian = bid.getCivilian(world);
            double civBenefit = world.getHelper(HumanHelper.class).getBenefit(civilian.getID());
            if (civBenefit > bestBenefit) {
                bestBenefit = civBenefit;
                bestCivilian = civilian;
            }
//            System.out.println(world.getTime()+" me:"+world.getSelfHuman().getID()+" civID:"+civilian.getID()+" bbbbbidCivilan Benefit="+civilian.getBenefit());
        }
        if (bestCivilian == null)
            System.out.println("NO BidCivilian");
        else
            System.out.println(world.getTime() + " me:" + world.getSelfHuman().getID() + " civID:" +
                    bestCivilian.getID() + " bbbbbBBBBBest Benefit=" + world.getHelper(HumanHelper.class).getBenefit(bestCivilian.getID()));

        return bestCivilian;

    }

    private int findActionWithGraterValueOfThisState(double[][] qTable) {

        double maxValue = 0;
        int bestAction = -1;
        for (int i = 0; i < 4; i++) {
            if (qTable[this.stateValue][i] >= maxValue) {
                maxValue = qTable[this.stateValue][i];
                bestAction = i;
            }
        }

        return bestAction;
    }

    private double findMaxValueOfThisAction(double[][] qTable, int action) {

        if (action == -1)
            return -1;


        double maxValue = 0;
        for (int i = 0; i < 4; i++) {
            if (qTable[action][i] > maxValue)
                maxValue = qTable[action][i];
        }

        return maxValue;
    }

//    private int computeNumberOfRescueTimesOfMeForThisCivilian(ArrayList<ShouldGetReward> shouldGetRewardList, ShouldGetReward shouldGetReward) {
//
//        int numberOfExistence = 0;
//        for (ShouldGetReward sGR : shouldGetRewardList) {
//            if (sGR.getCivilianID().getID().equals(shouldGetReward.getCivilianID().getID()))
//                numberOfExistence++;
//        }
//
//        return numberOfExistence;
//    }


}
