package mrl.ambulance.marketLearnerStrategy;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.communication2013.helper.AmbulanceMessageHelper;
import mrl.helper.HumanHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static mrl.common.comparator.ConstantComparators.COST_VALUE_COMPARATOR;

/**
 * User: pooyad
 * Date: 5/3/11
 * Time: 3:58 PM
 */
public class LeaderInitiatorAuction extends Auction {


    private int numberOfBidsByLeader = 1;

    //List of Victims to sell by leader
    protected List<VictimImportance> victimsToSell = new ArrayList<VictimImportance>();
    protected Set<EntityID> shouldCheckVictims;


    public LeaderInitiatorAuction(MrlAmbulanceTeamWorld world, AmbulanceMessageHelper ambulanceMessageHelper, AmbulanceUtilities ambulanceUtilities) {
        super(world, ambulanceMessageHelper, ambulanceUtilities);
        shouldCheckVictims = new FastSet<EntityID>();
    }

    @Override
    public void startAuction(Set<StandardEntity> goodHumans) {

        if (!amILeader()) {
            return;
        }

        //get m most valuable victims to bid
        List<VictimImportance> victimsToSell = getVictimsToSell(goodHumans);

//        ambulanceMessageHelper.sendVictimsToSell(victimsToSell);

    }

    protected boolean amILeader() {
        return world.getAmbulanceLeaderID() != null && world.getAmbulanceLeaderID().equals(world.getSelf().getID());
    }

    @Override
    public boolean isBidTime() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Sort Victims Based on their Importance. At first we consider their TimeToDeath(TTD).
     *
     * @param goodHumans Victims to select bids from them.
     * @return Victims to send as leader bids.
     */
    protected List<VictimImportance> getVictimsToSell(Set<StandardEntity> goodHumans) {

        // list of <victim,Importance(TTD)>
        List<VictimImportance> victimImportanceList = new ArrayList<VictimImportance>();

        victimsToSell.clear();

        int importance;

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

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
            victimImportanceList.add(new VictimImportance(victim, importance));
        }

        if (victimImportanceList.isEmpty()) {
            return new ArrayList<VictimImportance>();
        }

        //sort victims based on their importance, smaller one is more important one
        Collections.sort(victimImportanceList);

        int min = Math.min(victimImportanceList.size(), numberOfBidsByLeader);

        for (int i = 0; i < min; i++) {
            victimsToSell.add(victimImportanceList.get(i));
        }


        return victimsToSell;
    }

    /**
     * This funcrion computes importance of a given victim. At first the importance is time to death
     * of the victim. it should be considered that after a period of not seeing a victim, this computation might
     * not be currect.
     *
     * @param victim The given victim to compute its importants.
     * @return returns the importance of the victim, it is almost the time to death of the victim. If the
     *         victim wouldn't die during the remained time to end of the simulation time, it will returns -1;
     */
    protected int computeImportance(StandardEntity victim) {
        Pair<Integer, Integer> ttd = ambulanceUtilities.computeTTD(victim);

        //TODO: if it is a platoon agent, we should change the importance value

        if (ttd.first() < 0) {
            return 10000;
        }

        return ttd.first();
    }

//    @Override
//    public boolean isRecieveBidsTime() {
//
//        return false;
//    }

    @Override
    public void taskAllocation() {

        if (!amILeader()) {
            return;
        }
        if (!isRecieveBidsTime()) {
            return;
        }

        int ttd, brd, ttr;
        Human victim;
        int delayCost = 4;
        int arrivedAgents;
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        int[][] costs; // [agents][agentID,Cost]

        for (int i = 0; i < victimsToSell.size(); i++) {
            victim = (Human) (victimsToSell.get(i).getVictim());
            ttr = humanHelper.getTimeToRefuge(victim.getID());
            ttd = victimsToSell.get(i).getImportance() - delayCost - ttr;
            brd = victim.getBuriedness();
            if (world.getVictimBidsMap().get(victim.getID()) != null) {
//                costs = fillCostList(world.getVictimBidsMap().get(victim.getID()));
                costs = fillCostList(world.getVictimBidsMap().get(victim.getID()));
                Arrays.sort(costs, COST_VALUE_COMPARATOR);
            } else {
                continue;
            }

            arrivedAgentList.clear();
            while (ttd > 0 && brd > 0) {

                //TODO: check if it can be done by less agents
                arrivedAgents = continueOneCycle(costs);

                if (willBeAlive(costs, arrivedAgentList, ttd, brd)) {
                    brd = 0;
                    break;
                }

                brd -= arrivedAgents;
                ttd--;
            }

            if (ttd > 0 && brd <= 0) {//it will survive
                allocateTask(victim.getID());
            } else { // probably it wouldn't be survived, so consider to check it if we have enough agents
                shouldCheckVictims.add(victim.getID());
            }


        }

        // keep tasks list for each agent
        keepTasks();

//        ambulanceMessageHelper.sendTasks();

    }

    /**
     * This function keeps current assigned tasks for each agents as their task queue
     */
    protected void keepTasks() {
        Map<EntityID, Task> taskList;
        Task task;
        for (EntityID agentID : world.getTaskAssignment().keySet()) {
            task = new Task(world.getTaskAssignment().get(agentID));
            taskList = world.getTaskLists().get(agentID);
            if (taskList == null) {
                taskList = new FastMap<EntityID, Task>();
            }
            taskList.put(task.getVictimID(), task);
            world.getTaskLists().put(agentID, taskList);

        }
    }

    /**
     * get agent bids for the victim
     *
     * @param pairs pairs of (agent, bid(cost))
     * @return array of costs
     */
    private int[][] fillCostList(ArrayList<Pair<EntityID, Integer>> pairs) {
        int[][] costs = new int[pairs.size()][2];
        for (int i = 0; i < pairs.size(); i++) {
            costs[i][0] = pairs.get(i).first().getValue();
            costs[i][1] = pairs.get(i).second();
        }

        return costs;
    }

    private List<Integer> arrivedAgentList = new ArrayList<Integer>();

    /**
     * simulate one cycle, if agents assiged to this victim
     *
     * @param costs remains the cost of arriving each AT to the victim
     * @return returns number of arrived AT at victim
     */
    private int continueOneCycle(int[][] costs) {

        int numberOfArrivedAgents = 0;
        for (int i = 0; i < costs.length; i++) {
            if (world.getTaskAssignment().containsKey(new EntityID(costs[i][0]))) {
                // this agent has been assigned to other victim and can not come here
                continue;
            }
            costs[i][1] -= 1;
            if (costs[i][1] <= 0) {
//                arrivedAgentList.add(costs[i][0]);
//                willBeAlive(costs, arrivedAgentList, ttd, brd);
                numberOfArrivedAgents++;
            }
        }

        return numberOfArrivedAgents;
    }

    private boolean willBeAlive(int[][] costs, List<Integer> arrivedAgentList, int ttd, int brd) {
        int tempTTD;
        int tempBRD;
        for (int i = 0; i < costs.length; i++) {
            if (costs[i][1] <= 0 && !arrivedAgentList.contains(costs[i][0]) && !world.getTaskAssignment().containsKey(new EntityID(costs[i][0]))) {
                tempTTD = ttd;
                tempBRD = brd;
                arrivedAgentList.add(costs[i][0]);
                while (tempTTD > 0 && tempBRD > 0) {

                    tempBRD -= arrivedAgentList.size();
                    tempTTD--;
                }

                if (tempTTD > 0 && tempBRD <= 0) {//it will survive
                    return true;
                } else { // probably it wouldn't be survived, so it needes more agents
                    return false;
                }

            }
        }
        return false;
    }

    /**
     * Allocate each agent(who can help) to the victim
     *
     * @param victimID id of the victim
     */
    private void allocateTask(EntityID victimID) {
//        int maxNeededAgents = computeMaximumNeededAgent(id);
////        Arrays.sort(costs, COST_VALUE_COMPARATOR);
//        for (int i = 0; i < costs.length && maxNeededAgents > 0; i++) {
//            if (costs[i][1] <= 0) {// it will arrive on time
//                maxNeededAgents--;
//                world.getTaskAssignment().put(new EntityID(costs[i][0]), id);
//            }
//        }

        List<EntityID> assignedAgents = new ArrayList<EntityID>();
        EntityID agentID;
        for (int i : arrivedAgentList) {
            agentID = new EntityID(i);
            world.getTaskAssignment().put(agentID, victimID);
            assignedAgents.add(agentID);
        }

        world.getAgentAssignment().put(victimID, assignedAgents);
    }

    @Override
    public void clearPreviouseTasks() {
        if ((world.getTime() - 2) % auctionPeriod == 0) {

            //clear previouse assigned tasks
            world.getTaskAssignment().clear();
        }
    }


    @Override
    public int computeCost(Human myCurrentTarget, EntityID victimID) {
        Human victim = (Human) world.getEntity(victimID);
        int cost = 0;
        int tempTTM, tempTTF;

        Map<EntityID, Task> taskMap = world.getTaskLists().get(world.getSelf().getID());
        List<Task> tasks = new ArrayList<Task>();

        if (taskMap != null) {
            //get task from map values
            for (Iterator it = taskMap.values().iterator(); it.hasNext(); ) {
                tasks.add((Task) it.next());
            }

            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i).isDone()) {//TODO: set task.setDone() when task in done
                    continue;
                }
                if (myCurrentTarget != null && myCurrentTarget.getID().equals(tasks.get(i).getVictimID())) {
                    cost += ambulanceUtilities.timeToFreeAT(myCurrentTarget);
                } else {
                    if (i != tasks.size() - 1)//if it is not last task
                    {
                        if (tasks.get(i).getNextTTM() == -1)// if it was not set
                        {
                            tempTTM = ambulanceUtilities.approximatingTTM
                                    (world.getEntity(tasks.get(i).getVictimID()), world.getEntity(tasks.get(i + 1).getVictimID()));
                            tasks.get(i).setNextTTM(tempTTM);
                        }
                        //cost of moving from a victim to another
                        cost += tasks.get(i).getNextTTM();
                    }
                    if (tasks.get(i).getTTF() == -1)// if ttf was not set
                    {
                        tempTTF = compute_TTF_For_Future_Task(tasks.get(i).getVictimID());
                        if (tempTTF == 1000) {
                            System.err.println("tempTTF == 1000");
                        }
                        tasks.get(i).setTTF(tempTTF);

                    }
                    cost += tasks.get(i).getTTF();
                }
            }
        }

        cost += ambulanceUtilities.approximatingTTA(victim);

        return cost;
    }

    private int compute_TTF_For_Future_Task(EntityID victimID) {
        Human victim = (Human) world.getEntity(victimID);
        List<EntityID> assignedAgents = world.getAgentAssignment().get(victim.getID());
        if (assignedAgents == null) {
            return 1000;
        }
        int ttf = (int) Math.ceil((float) victim.getBuriedness() / assignedAgents.size());
//        world.getHelper(HumanHelper.class).setTTF(victim.getID(),ttf);
        return ttf;
    }

    public EntityID getMyTask() {
//        return world.getTaskAssignment().get(world.getSelf().getID());
        Map<EntityID, Task> tasks = world.getTaskLists().get(world.getSelf().getID());
        if (tasks == null) {
            return null;
        }
        for (Task task : tasks.values()) {
            if (!task.isDone()) {
                return task.getVictimID();
            }
        }
        //no task exist
        return null;
    }


    public Set<EntityID> getShouldCheckVictims() {
        return shouldCheckVictims;
    }

    /**
     * Estimate cost of each leader bids and send it as a bid
     *
     * @param myCurrentTarget my currently rescuing target
     */
    @Override
    public void bidding(Human myCurrentTarget) {
        if (!isBidTime()) {
            return;
        }

        List<AmbulanceTeamBid> bids = new ArrayList<AmbulanceTeamBid>();

        Human human;
        for (EntityID victimID : world.getLeaderBids()) {

            human = (Human) world.getEntity(victimID);
            if (human == null || !human.isPositionDefined()) {
                continue; //TODO : what should we do here? now we don't bid for it
            }
            AmbulanceTeamBid bid = new AmbulanceTeamBid();
            bid.setBidderID(world.getSelf().getID());
            bid.setHumanID(victimID);
            bid.setBidValue(computeCost(myCurrentTarget, victimID));
            bids.add(bid);

        }

        world.getLeaderBids().clear();

        List<Pair<EntityID, Integer>> vics = new ArrayList<Pair<EntityID, Integer>>();
        for (AmbulanceTeamBid ambulanceTeamBid : bids) {
            vics.add(new Pair<EntityID, Integer>(ambulanceTeamBid.getHumanID(), ambulanceTeamBid.getBidValue()));
        }
        System.out.println("BBBBBBBBBBBB " + world.getTime() + " " + world.getSelf().getID() + " " + vics);

//        ambulanceMessageHelper.sendBidMessage(bids);


    }

}
