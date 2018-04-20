package mrl.police.moa.allocation;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/28/12
 * Time: 11:08 AM
 */

import javolution.util.FastMap;
import mrl.police.moa.Bid;
import mrl.task.PoliceActionStyle;
import mrl.task.Task;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;
import java.util.Set;


/**
 * It is an allocation algorithm which allocate tasks to agents greedily, means each task will allocated to the agent
 * with the best value
 */
public class GreedyAllocation implements IAllocation {


    @Override
    public Map<EntityID, Task> taskAllocation(Map<EntityID, Map<EntityID, Bid>> targetBidsMap) {

        Map<EntityID, Task> taskMap = new FastMap<EntityID, Task>();
        if (targetBidsMap.isEmpty()) {
            return taskMap;
        }
        EntityID bestAgent;
        Task task;
        for (EntityID targetID : targetBidsMap.keySet()) {
            dropAssignedAgentsBids(taskMap.keySet(), targetBidsMap.get(targetID));
            if (targetBidsMap.get(targetID).isEmpty()) {
                continue;
            }
            bestAgent = getTheBestAgent(targetBidsMap.get(targetID));
            task = new Task(targetBidsMap.get(targetID).get(bestAgent).getTarget(), PoliceActionStyle.CLEAR_DIRECTION_NORMAL);
            taskMap.put(bestAgent, task);
        }

        return taskMap;
    }

    /**
     * Drop bids of assigned agents to assign each agent only one task
     *
     * @param assignedAgents list of agents who has a task to do
     * @param bids           map of  each agent to its  bid for a known target
     */
    private void dropAssignedAgentsBids(Set<EntityID> assignedAgents, Map<EntityID, Bid> bids) {
        for (EntityID assignedAgent : assignedAgents) {
            if (bids.keySet().contains(assignedAgent)) {
                bids.remove(assignedAgent);
            }
        }
    }


    /**
     * finds agent with lowest bid value as the best one which costs less than others
     *
     * @param bids bids to compare together
     * @return the agent ID with lowest bid cost
     */
    private EntityID getTheBestAgent(Map<EntityID, Bid> bids) {
        Bid bestBid = null;
        if (bids.values().iterator().hasNext()) {
            bestBid = bids.values().iterator().next();
        }
        for (Bid bid : bids.values()) {
            if (bid.getValue() < bestBid.getValue()) {
                bestBid = bid;
            }
        }
        return bestBid.getBidder();

    }
}
