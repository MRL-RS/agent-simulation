package mrl.world.routing.pathPlanner;

import mrl.common.CommandException;
import mrl.world.routing.graph.Graph;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Sep 22, 2010
 * Time: 5:05:57 PM
 */
public interface IPathPlanner {
    public Graph getGraph();

    public boolean move(Area target, int maxDistance, boolean force) throws CommandException;

    public boolean move(Collection<? extends Area> targets, int maxDistance, boolean force) throws CommandException;

    public void moveToRefuge() throws CommandException;

    public void moveToPoint(EntityID area, int destX, int destY) throws CommandException;

    public void update();

    public List<EntityID> getLastMovePlan();

    public EntityID getPreviousTarget();

    public int getPathCost();

    public List<EntityID> planMove(Area source, Area destination, int maxDistance, boolean force);

    public void moveOnPlan(List<EntityID> planMove) throws CommandException;

    public List<EntityID> getRefugePath(Area source, boolean force);

    public int getNearestAreaPathCost();

    public CheckAreaPassable getAreaPassably();

    public List<EntityID> getNextPlan();

    void moveToHydrant() throws CommandException;
}
