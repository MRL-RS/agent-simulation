package mrl.police.clear;

import mrl.common.CommandException;
import mrl.common.Util;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * @author Mahdi
 *         This class is an action manager to executting clear acts.<br/>
 */
public class ClearActManager {
    private ActionStyle actionStyle;
    private ActionExecutorType actionExecutorType;
    private ClearAreaActExecutor clearAreaActExecutor;
    private ClearBlockadeActExecutor clearBlockadeActExecutor;
    private ActResult lastActResult;
    private MrlWorld world;
    private boolean onBlockade = true;


    public ClearActManager(MrlWorld world) {
        this.world = world;
        clearAreaActExecutor = new ClearAreaActExecutor(this.world);
        clearBlockadeActExecutor = new ClearBlockadeActExecutor(this.world);
        actionExecutorType = ActionExecutorType.CLEAR_AREA;
    }


    public void clearWay(List<EntityID> path, EntityID target) throws CommandException {

        if (path == null) {
            //I have no task. so i should complete my postponed jobs...
            doPostponeJobs();
            //no postpone jobs i have
            return;
        }else if (path.isEmpty()){
            path.add(world.getSelfPosition().getID());
        }

        initActionExecutorType(path, target);
        ActResult result = ActResult.WORKING;
        switch (actionExecutorType) {
            case CLEAR_AREA:
                result = clearAreaActExecutor.clearWay(path, target);
                break;
            case CLEAR_BLOCKADE:
                result = clearBlockadeActExecutor.clearWay(path, target);
                break;
        }
        lastActResult = result;
    }

    public void doPostponeJobs() throws CommandException {
        clearBlockadeActExecutor.clearWay(null, null);
    }

    public void clearAroundTarget(Pair<Integer, Integer> targetLocation) throws CommandException {
        clearBlockadeActExecutor.clearAroundTarget(targetLocation);
    }

    private void initActionExecutorType(List<EntityID> path, EntityID target) {
        StandardEntity entity = world.getEntity(target);
        if (entity != null && entity instanceof Refuge && path.size() <= 1) {
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        } else if (world.getSelfPosition() instanceof Building || onBlockade && Util.isOnBlockade(world)) {//this condition is used to prevent kernel bug at the beginning of run(if agent is on blockade, new clear act will not work)
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        } else {
            onBlockade = false;
            actionExecutorType = ActionExecutorType.CLEAR_AREA;
        }

//        actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE; //todo FOR TEST OLD CLEAR IN NEW DESIGN
//        debugNewClearAct(path, entity);//todo -> this function used just for debug bugs of new clear act
    }

    private void debugNewClearAct(List<EntityID> path, StandardEntity target) {
        Point2D source = Util.getPoint(world.getSelfLocation());
        Point2D destination = Util.getPoint(target.getLocation(world));
        if (Util.distance(source, destination) <= world.getClearDistance()) {
            actionExecutorType = ActionExecutorType.CLEAR_BLOCKADE;
        }
    }


    public ActionStyle getActionStyle() {
        return actionStyle;
    }

    public void setActionStyle(ActionStyle actionStyle) {
        this.actionStyle = actionStyle;
    }

    public void clearBlockadesInRange(Pair<Integer, Integer> location, int range) throws CommandException {
        clearBlockadeActExecutor.clearBlockadesInRange(location, range);
    }

    private enum ActionExecutorType {
        CLEAR_AREA,
        CLEAR_BLOCKADE,
    }
}
