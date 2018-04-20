package mrl.police.moa;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/27/12
 * Time: 7:37 PM
 */

import mrl.task.Task;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.Map;


public interface IAuction {

    /**
     * Starting an auction in a period of time by sending some bids
     *
     * @param time    cycle of the simulation
     * @param targets map of targets
     */
    void startAuction(int time, Map<EntityID, Target> targets);


    /**
     * Allocating task to agents based on their bids or their costs to do the works
     *
     * @param time cycle of the simulation
     */
    void taskAllocation(int time);

    /**
     * Gets my own task which I won previously in an auction
     *
     * @param anentID agent ID to get its task
     * @return the task which I have to do
     */
    Task getTask(EntityID anentID);


    /**
     * after doing the allocated task, set it as done
     */
    void doneTask();


    /**
     * Computes cost of reaching the target
     *
     * @param target the target to reach
     * @return cost to reach the target
     */
    int cost(Target target);

    /**
     * Sets available agents
     *
     * @param availableAgents number of available agents
     */
    void setAvailableAgents(int availableAgents);


    /**
     * It is a data structure for keeping a Map of bidder and their bids for each specified target
     *
     * @param targetBidsMap bids for each target
     */
    void setTargetBidsMap(Map<EntityID, Map<EntityID, Bid>> targetBidsMap);


    /**
     * Gets Allocated tasks
     *
     * @return list of allocated task IDs
     */
    List<EntityID> getAllocatedTargets();

    /**
     * Gets Done Tasks
     *
     * @return list of done task IDs
     */
    List<EntityID> getClearedTasks();

}
