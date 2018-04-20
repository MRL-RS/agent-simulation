package mrl.strategy;


import mrl.task.ActionStatus;
import mrl.task.Task;

/**
 * @author Siavash
 */
public interface Strategy {
    public ActionStatus runTask(Task task);
}
