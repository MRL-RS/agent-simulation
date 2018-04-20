package mrl.world.routing.pathPlanner.move;

import mrl.common.CommandException;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * @author Mahdi
 */
public interface MoveActExecutor {

    public ActionState execute(List<EntityID> plan) throws CommandException;

    public boolean acceptInterrupt();

    public ActionExecutorType getType();
}
