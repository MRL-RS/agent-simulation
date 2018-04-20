package mrl.ambulance.marketLearnerStrategy;

import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.ambulance.structures.ShouldGetReward;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: pooyad
 * Date: 6/9/11
 * Time: 7:34 PM
 */
public class AmbulanceLA {

    MrlAmbulanceTeamWorld world;

    String fileName = "data/ambulanceLA.txt";
    int numberOfStates = 8;

    // A map of stateType to list of victims in that state
    Map<Integer, ArrayList<EntityID>> victimClusters = new HashMap<Integer, ArrayList<EntityID>>();

    // possible actions to select a state
    List<Integer> possibleActions;

    /**
     * states of LA
     */
    private double[] stateProbabilities;

    /**
     * total reward earned since the last visit to state 'i'
     */
    private double[] totalEarnedReward;

    /**
     * is the number of state transitions that have been taken place since the last visit to 'i
     */
    private int[] stateTransitions;

    private double learningRate;
    private double sMin;
    private double sMax;  // 1/numberOfStates


    public AmbulanceLA(MrlAmbulanceTeamWorld world) {
        this.world = world;
        stateProbabilities = new double[numberOfStates];
        initializePobabilities(stateProbabilities);
        stateTransitions = new int[numberOfStates];
        totalEarnedReward = new double[numberOfStates];
        learningRate = 0.01;
        sMin = 0;
        sMax = (double) 1 / numberOfStates;
    }

    private void initializePobabilities(double[] stateProbabilities) {
        for (int i = 0; i < numberOfStates; i++) {
            stateProbabilities[i] = 1f / numberOfStates;
        }
    }

    public void updateLA(Map<EntityID, ShouldGetReward> shouldGetRewardList, List<EntityID> finishedTasks) {

        ShouldGetReward shouldGetReward;
        for (EntityID victimID : finishedTasks) {
            shouldGetReward = shouldGetRewardList.get(victimID);
            shouldGetReward.setGotReward(true);
            shouldGetReward.setComputedReward(computeReward(shouldGetReward));
            updateLAProbablities(shouldGetReward.getStateValue(), shouldGetReward.getComputedReward(), shouldGetReward.getTotalStateTransitionsBeforeIt());
        }


    }

    public void updateLA(Map<EntityID, ShouldGetReward> shouldGetRewardList, Set<EntityID> willDead) {

        ShouldGetReward shouldGetReward;
        for (EntityID victimID : willDead) {
            shouldGetReward = shouldGetRewardList.get(victimID);
            if (shouldGetReward.isGotReward()) {
                continue;
            }
            shouldGetReward.setGotReward(true);
            shouldGetReward.setComputedReward(computeReward(shouldGetReward));
            updateLAProbablities(shouldGetReward.getStateValue(), shouldGetReward.getComputedReward(), shouldGetReward.getTotalStateTransitionsBeforeIt());
        }


    }

    private double computeReward(ShouldGetReward shouldGetReward) {
        return shouldGetReward.getComputedReward();
    }

    /**
     * This function updates state probabilities considering earned reward
     *
     * @param stateValue     the selected state as action
     * @param computedReward earned reward of selecting the state stateType
     * @param tST            is total State Transitions Before visit statestateValue
     */
    private void updateLAProbablities(int stateValue, double computedReward, int tST) {
        double tERFS = 0;//totalEarnedRewardForState
//        tERFS = updateTotalEarnedReward(stateValue, computedReward);
        double feadBack;


        for (int i = 0; i < stateProbabilities.length; i++) {
            if (i == stateValue) {
//                feadBack = feadback(stateValue, tST, tERFS);
                stateProbabilities[i] += learningRate * computedReward * (1 - stateProbabilities[i]);
            } else {
//                feadBack = feadback(stateValue, stateTransitions[i], totalEarnedReward[i]);
                stateProbabilities[i] -= learningRate * computedReward * stateProbabilities[i];
            }
//            stateProbabilities[stateValue] += learningRate * feadBack * isActionTaken(i, stateValue) -
//                    learningRate * feadBack * stateProbabilities[stateValue];

        }
        System.out.println("-----------------------------------------------------------------------------------");
        System.out.println(world.getTime() + " " + stateValue + " " + "Probablities====>> " + stateProbabilities);

    }

    /**
     * Is action i eaqual with state value?
     *
     * @param i
     * @param stateValue
     * @return 1 of i is the taken action and otherwise 0
     */
    private double isActionTaken(int i, int stateValue) {
        if (i == stateValue) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Calculate normalized response of the system to the action selected in the last visit to the state 'i'
     *
     * @param stateValue the selected state
     * @param tST        total state transition to visit stateVale
     * @param tERFS      total earned reward until visititing stateVale
     * @return normalized feadback
     */
    private double feadback(int stateValue, int tST, double tERFS) {

        double s; // response of the system to the action selected in the last visit to the state 'i'
        double feadBack = 0;  //normalized feadback

        s = tERFS / tST;
        feadBack = (s - sMin) / (sMax - sMin);


        return feadBack;

    }

    public void clusterVictimsIntoStates(List<VictimImportance> victimImportanceList) {
        int victimCluster;
        victimClusters.clear();
        possibleActions = new ArrayList<Integer>();

        for (VictimImportance victimImportance : victimImportanceList) {
            victimCluster = verifyingStateType(victimImportance.getImportance());

            ArrayList<EntityID> clusteredVictims = victimClusters.get(victimCluster);
            if (clusteredVictims == null) {
                clusteredVictims = new ArrayList<EntityID>();
            }
            clusteredVictims.add(victimImportance.getVictim().getID());
            if (!possibleActions.contains(victimCluster)) {
                possibleActions.add(victimCluster);
            }
            victimClusters.put(victimCluster, clusteredVictims);
        }

    }

    // victim value currently is timeToDeath
    public int verifyingStateType(Integer victimValue) {


        if (victimValue < 50)
            return 0;
        else if (victimValue >= 50 && victimValue < 70)
            return 1;
        else if (victimValue >= 70 && victimValue < 100)
            return 2;
        else if (victimValue >= 100 && victimValue < 125)
            return 3;
        else if (victimValue >= 125 && victimValue < 150)
            return 4;
        else if (victimValue >= 150 && victimValue < 180)
            return 5;
        else if (victimValue >= 180 && victimValue < 200)
            return 6;
        else
            return 7;
    }

    public int actionSelection() {

        if (possibleActions.size() == 0) {
            return -1;
        } else if (possibleActions.size() == 1) {
            return possibleActions.get(0);
        } else { // there exist more than one posiible actions to select

            Collections.sort(possibleActions);
            Random random = new Random(System.currentTimeMillis() * world.getTime());

            int count = 10000;
            double rnd, prob;
            while (count >= 0) {
                rnd = random.nextDouble();
                prob = 0;
                for (int i = 0; i < possibleActions.size(); i++) {
                    prob += stateProbabilities[possibleActions.get(i)];
                    if (rnd <= prob) {
                        return possibleActions.get(i);
                    }
                }

                count--;
            }
/*
            Random random;
            int count = 10000;
            while (count >= 0) {
                random = new Random(System.currentTimeMillis() * world.getTime());

                double rnd = random.nextDouble();
                double prob = 0;
                for (int i = 0; i < stateProbabilities.length; i++) {
                    prob += stateProbabilities[i];
                    if (rnd <= prob) {
                        return i;
                    }
                }
                count--;
            }
*/
        }

        return -1;
    }

    public Map<Integer, ArrayList<EntityID>> getVictimClusters() {
        return victimClusters;
    }

    public List<Integer> getPossibleActions() {
        return possibleActions;
    }

    /**
     * If any state is visited, first keeps its total tarnsitions too meet it then increase others
     *
     * @param stateType the state that is met
     * @return number Of State Transitions to meet the selected state
     */
    public int updateStateTransitions(int stateType) {
        int numberOfStateTransitions = 0;
        if (stateType == -1) {
            return -1;
        }
        for (int i = 0; i < stateTransitions.length; i++) {
            if (i == stateType) {
                numberOfStateTransitions = stateTransitions[i];
                stateTransitions[i] = 0;
            } else {
                stateTransitions[i]++;
            }
        }
        return numberOfStateTransitions;
    }

    /**
     * If any state is visited, first keeps its total earned reward since the last visit then
     * increase others by earned reward
     *
     * @param stateType    the state that is met
     * @param earnedReward reward earned by doing selecting the state stateType
     */
    public double updateTotalEarnedReward(int stateType, double earnedReward) {
        double totalEarnedRewardForState = 0;
        if (stateType == -1) {
            return -1;
        }
        for (int i = 0; i < totalEarnedReward.length; i++) {
            if (i == stateType) {
                totalEarnedRewardForState = totalEarnedReward[i];
                totalEarnedReward[i] = 0;
            } else {
                totalEarnedReward[i] += earnedReward;
            }
        }
        return totalEarnedRewardForState;
    }

    public void pritProbsToFile() {
        LearnerIO learnerIO = new LearnerIO(fileName, false);
        String data = "";
        for (int i = 0; i < numberOfStates; i++) {
            data += "\t" + stateProbabilities[i];
        }

        learnerIO.printToFile_LA(data);

    }

    public void readProbsFromFile() {
        LearnerIO learnerIO = new LearnerIO(fileName, false);
        stateProbabilities = learnerIO.readFromFile_LA(numberOfStates);
        if (stateProbabilities[0] == 0) {
            for (int i = 0; i < numberOfStates; i++) {
                stateProbabilities[i] = (double) 1 / numberOfStates;
            }
        }

    }
}
