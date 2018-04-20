package mrl.police.moa;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/27/12
 * Time: 7:35 PM
 */


import javolution.util.FastMap;
import mrl.common.MRLConstants;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.moa.allocation.GreedyAllocation;
import mrl.police.moa.allocation.IAllocation;
import mrl.task.Task;
import mrl.world.MrlWorld;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * this class makes auctioning process available to police agents
 */
public class Auction implements IAuction {

    private MrlWorld world;
    private MrlPlatoonAgent self;

    //    private IMessageHelper messageHelper;
    private PoliceMessageHelper policeMessageHelper;
    private IAllocation allocationAlgorithm;
    private Map<EntityID, Task> tasks;
    private Map<EntityID, Map<EntityID, Bid>> targetBidsMap;
    private List<EntityID> allocatedTargets;
    private List<EntityID> clearedTargets;
    //    private int numberOfBids; // this value is based on available agents
    private Task myTask;
    private int availableAgents;

    private static final int AUCTION_PERIOD = 3;
    private static final int TIME_TO_FREE_THRESHOLD = 5;
    private PoliceForceUtilities utilities;


    //    public Auction(MrlWorld world, MrlPlatoonAgent self, IMessageHelper messageHelper) {
//        this.world = world;
//        this.self = self;
//        this.messageHelper = messageHelper;
//        allocationAlgorithm = new GreedyAllocation();
//        tasks = new FastMap<EntityID, Task>();
//        targetBidsMap=new FastMap<EntityID,Map<EntityID,Bid>>();
//        foundTargets = new ArrayList<EntityID>();
//        clearedTargets = new ArrayList<EntityID>();
//
//    }
    public Auction(MrlWorld world, MrlPlatoonAgent self, PoliceMessageHelper messageHelper, PoliceForceUtilities utilities) {
        this.world = world;
        this.self = self;
        this.policeMessageHelper = messageHelper;
        allocationAlgorithm = new GreedyAllocation();
        tasks = new FastMap<EntityID, Task>();
        targetBidsMap = new FastMap<EntityID, Map<EntityID, Bid>>();
        allocatedTargets = new ArrayList<EntityID>();
        clearedTargets = new ArrayList<EntityID>();
        this.utilities = utilities;
    }

    @Override
    public void startAuction(int time, Map<EntityID, Target> targets) {

        if (time % AUCTION_PERIOD != 0) {
            return;
        }

        if (!isTimeToParticipate(self.getID())) {
            return;
        }

        // clear previously received bids
        targetBidsMap.clear();
        System.err.println(self.getID() + " " + time + " Bids Cleared");

        List<Target> targetList = new ArrayList<Target>(targets.values());
        Collections.sort(targetList, utilities.TARGET_IMPORTANCE_COMPARATOR);

        List<Bid> bids = new ArrayList<Bid>();
        Bid bid;
        Map<EntityID, Bid> agentBidMap;
        for (int i = 0; i < Math.min(availableAgents, targets.size()); i++) {
            bid = new Bid(self.getID(), targetList.get(i), cost(targetList.get(i)));
            bids.add(bid);
            agentBidMap = new FastMap<EntityID, Bid>();
            // I should keep my bids for myself
            agentBidMap.put(self.getID(), bid);
            targetBidsMap.put(targetList.get(i).getId(), agentBidMap);

        }


//        policeMessageHelper.sendBidMessage(bids);
//        messageHelper.sendMessage(bids, MessageType.PoliceBid);

    }

    @Override
    public void taskAllocation(int time) {
        if ((time - 1) % AUCTION_PERIOD == 0) {
            tasks = allocationAlgorithm.taskAllocation(targetBidsMap);
            targetBidsMap.clear();
            keepAllocatedTasks(tasks);
            setMyTask(self.getID());
        }

    }

    @Override
    public Task getTask(EntityID agentID) {
        //which is not in clearedTargets
        return myTask;
    }

    @Override
    public void doneTask() {
        clearedTargets.add(myTask.getTarget().getId());
        myTask = null;
    }

    @Override
    public int cost(Target target) {

        //TODO: test it with real distance
        //simple distance/velocity
        return (int) (world.getDistance(self.getID(), target.getId()) / MRLConstants.MEAN_VELOCITY_OF_MOVING);
    }

    @Override
    public void setAvailableAgents(int availableAgents) {
        this.availableAgents = availableAgents;
    }

    @Override
    public void setTargetBidsMap(Map<EntityID, Map<EntityID, Bid>> targetBidsMap) {
        this.targetBidsMap = targetBidsMap;
    }

    @Override
    public List<EntityID> getAllocatedTargets() {
        return allocatedTargets;
    }

    @Override
    public List<EntityID> getClearedTasks() {
        return clearedTargets;
    }


    /**
     * Identifies if it is time to participate in next auction
     *
     * @param agentID the agent who want to know whether should participate in next auction or not
     * @return true if it is time to participate, otherwise false
     */
    private boolean isTimeToParticipate(EntityID agentID) {

        return getTask(agentID) == null /* TODO  || utilities.timeToFree(getTask(agentID)) < TIME_TO_FREE_THRESHOLD*/;
    }


    /**
     * If there is any allocated task to this agent then set it as its current task
     *
     * @param agentID the id of the current agent
     */
    private void setMyTask(EntityID agentID) {
        if (tasks != null && tasks.get(agentID) != null) {
            myTask = tasks.get(self.getID());
        }
    }


    /**
     * keep allocated tasks in a separate list
     *
     * @param tasks allocated tasks
     */
    private void keepAllocatedTasks(Map<EntityID, Task> tasks) {
        for (Task task : tasks.values()) {
            allocatedTargets.add(task.getTarget().getId());
        }
    }

}
