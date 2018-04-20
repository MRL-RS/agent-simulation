package mrl.ambulance.marketLearnerStrategy;

import javolution.util.FastMap;
import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.ambulance.structures.ShouldGetReward;
import mrl.communication2013.helper.AmbulanceMessageHelper;
import mrl.helper.HumanHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: pooyad
 * Date: 6/9/11
 * Time: 6:40 PM
 */
public class LIA_StaticAuctioning_LA extends LeaderInitiatorAuction {


    private Map<EntityID, ShouldGetReward> shouldGetRewardList = new HashMap<EntityID, ShouldGetReward>();
    private AmbulanceLA ambulanceLA;
    private boolean isRead = false;


    public LIA_StaticAuctioning_LA(MrlAmbulanceTeamWorld world, AmbulanceMessageHelper ambulanceMessageHelper, AmbulanceUtilities ambulanceUtilities) {
        super(world, ambulanceMessageHelper, ambulanceUtilities);

        ambulanceLA = new AmbulanceLA(world);

    }


    /**
     * By this method, the leader start auction in a specific
     *
     * @param goodHumans
     */
    @Override
    public void startAuction(Set<StandardEntity> goodHumans) {
        ambulanceUtilities.chooseLeader();

        if (!amILeader()) {
            return;
        }
        if (!isAuctionTime()) {
            return;
        }

        List<VictimImportance> victimsToSell = getVictimsToSell(goodHumans);

        List<Pair<EntityID, Integer>> vics = new ArrayList<Pair<EntityID, Integer>>();
        for (VictimImportance vic : victimsToSell) {
            vics.add(new Pair<EntityID, Integer>(vic.getVictim().getID(), vic.getImportance()));
        }
        System.out.println("LLLLLLLLLLLLLGGGGGGG " + world.getTime() + " " + world.getSelf().getID() + " I am LEADER Goods To bid: " + vics);


//        ambulanceMessageHelper.sendVictimsToSell(victimsToSell);

    }

    /**
     * this method selects a victim by using Learning Automata action selection strategy
     *
     * @param goodHumans Victims to select bids from them.
     * @return
     */
    @Override
    protected List<VictimImportance> getVictimsToSell(Set<StandardEntity> goodHumans) {

        // List of <Importance(TTD)>
        List<VictimImportance> victimImportanceList = new ArrayList<VictimImportance>();

        // Map of <victim,Importance(TTD)>
        Map<EntityID, VictimImportance> victimImportanceMap = new FastMap<EntityID, VictimImportance>();

        victimsToSell.clear();

        int importance;

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        VictimImportance victimImportance;

        for (StandardEntity victim : goodHumans) {


            if (/*(world.getTime() - humanHelper.getLastTimeDamageChanged(victim.getID())) > 10
                    || */shouldCheckVictims.contains(victim.getID())
                    || world.getAgentAssignment().keySet().contains(victim.getID())) {
                continue;
            }

            importance = computeImportance(victim);
            //TODO:  if importance is less than a threshhold, we should consider victim as dead
//            if (importance <= m) {// we can't survive the victim
//                continue;
//            }
            victimImportance = new VictimImportance(victim, importance);
            victimImportanceList.add(victimImportance);
            victimImportanceMap.put(victim.getID(), victimImportance);
        }

        if (victimImportanceList.isEmpty()) {
            return new ArrayList<VictimImportance>();
        }

        //sort victims based on their importance, smaller one is more important one
        Collections.sort(victimImportanceList);

        ambulanceLA.clusterVictimsIntoStates(victimImportanceList);

        // select a victim by considering the LA action selection
        int stateType = ambulanceLA.actionSelection();

        int numberOfStateTransitionToSeeThisState = ambulanceLA.updateStateTransitions(stateType);

        if (stateType == -1) {
            // no valid state
            return new ArrayList<VictimImportance>();
        }

        //get proper victim in selected state, which is randomly
        VictimImportance vic = getProperVictim(victimImportanceMap, stateType);
        if (vic != null) {
            victimsToSell.add(vic);
            addToShouldGetRewardList(victimsToSell, stateType, numberOfStateTransitionToSeeThisState);
        }

        //vic is null so return empty
        return victimsToSell;
    }

    private VictimImportance getProperVictim(Map<EntityID, VictimImportance> victimImportanceMap, int stateType) {

        if (victimImportanceMap.isEmpty()) {
            return null;
        }
        List<EntityID> lst = ambulanceLA.getVictimClusters().get(stateType);
        if (lst == null || lst.isEmpty()) {
            return null;
        }
        Random rnd = new Random(System.currentTimeMillis());
        return victimImportanceMap.get(lst.get(rnd.nextInt(lst.size())));
    }

    public void updateLA() {
        if (world.getAmbulanceLeaderID() != null && world.getAmbulanceLeaderID().equals(world.getSelf().getID())) {
            ambulanceLA.updateLA(shouldGetRewardList, getFinishedTasks());
        }
    }

    public void updateLA(Set<EntityID> shouldCheck) {
        if (world.getAmbulanceLeaderID() != null && world.getAmbulanceLeaderID().equals(world.getSelf().getID())) {
            ambulanceLA.updateLA(shouldGetRewardList, shouldCheck);
        }
    }

    private List<EntityID> getFinishedTasks() {

        Human human;
        ShouldGetReward shouldGetReward;
        List<EntityID> finishedTaskes = new ArrayList<EntityID>();
        for (EntityID victimID : shouldGetRewardList.keySet()) {

            shouldGetReward = shouldGetRewardList.get(victimID);
            if (shouldGetReward.isGotReward()) {
                continue;
            }

            human = (Human) world.getEntity(victimID);

            if (human.getHP() == 0 || shouldGetReward.isWillDead()) {
                finishedTaskes.add(human.getID());
                //TODO: should we keep any value for reward or punishment here?
                shouldGetReward.setComputedReward(-0.5);

                continue;
            }
            if (human instanceof Civilian) {
                if (world.getEntity(human.getPosition()) instanceof Refuge) {
                    //saved victim
                    finishedTaskes.add(victimID);
                    shouldGetReward.setComputedReward(0.5);
                }
            } else {
                if (human.getBuriedness() == 0) {
                    //rescued buried agent
                    finishedTaskes.add(victimID);
                    shouldGetReward.setComputedReward(0.5);
                }

            }

        }

        return finishedTaskes;

    }


    protected void addToShouldGetRewardList(List<VictimImportance> victimsToSell, int stateType, int numberOfStateTransitionToSeeThisState) {

        EntityID victimID;
        for (VictimImportance victimImportance : victimsToSell) {

            victimID = victimImportance.getVictim().getID();
            if (shouldGetRewardList.containsKey(victimID)) {
                return;
            }
            ShouldGetReward shouldGetReward = new ShouldGetReward();

            shouldGetReward.setVictimID(victimID);
            shouldGetReward.setStateValue(stateType);
            shouldGetReward.setTotalStateTransitionsBeforeIt(numberOfStateTransitionToSeeThisState);
            shouldGetReward.setComputedReward(0);

            shouldGetRewardList.put(victimID, shouldGetReward);
        }

    }

    /**
     * Is it time to send bids? Often after one cycle of starting an auction, it is time to send bids.
     *
     * @return returns true if it is time to send bids otherwise returns false.
     */
    @Override
    public boolean isBidTime() {
        if ((world.getTime() - 1) % auctionPeriod == 0) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean isRecieveBidsTime() {
        System.err.println("Currect Function call :LA");

        if ((world.getTime() - 2) % auctionPeriod == 0 || (world.getTime() - 3) % auctionPeriod == 0) {
            return true;
        } else {
            return false;
        }

    }


    public void printProbsToFile() {
        if (world.getAmbulanceLeaderID() != null && world.getAmbulanceLeaderID().equals(world.getSelf().getID())) {
            ambulanceLA.pritProbsToFile();
        }
    }

    public void readProbsFromFile() {
        if (!isRead && world.getAmbulanceLeaderID() != null && world.getAmbulanceLeaderID().equals(world.getSelf().getID())) {
            ambulanceLA.readProbsFromFile();
            isRead = true;
        }
    }


}
