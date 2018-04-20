package mrl.police.clear;

import mrl.common.CommandException;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * @author Mahdi
 */
public abstract class ClearActExecutor {
    protected ClearTools clearTools;
    protected MrlWorld world;

    protected ClearActExecutor(MrlWorld world) {
        this.world = world;
        clearTools = new ClearTools(world);
    }

    public abstract ActResult clearWay(List<EntityID> path, EntityID target) throws CommandException;

    public abstract void clearAroundTarget(Pair<Integer, Integer> targetLocation) throws CommandException;
}
