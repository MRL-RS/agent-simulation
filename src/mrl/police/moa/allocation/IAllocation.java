package mrl.police.moa.allocation;
/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/28/12
 * Time: 11:02 AM
 */

import mrl.police.moa.Bid;
import mrl.task.Task;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;


public interface IAllocation {

    Map<EntityID, Task> taskAllocation(Map<EntityID, Map<EntityID, Bid>> targetBidsMap);

}
