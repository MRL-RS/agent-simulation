package mrl.world.routing.pathPlanner.move;

import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mahdi Taherian
 */
public class MoveActManager {

    private MrlWorld world;
    private double stuckThreshold = 2000;


    private List<EntityID> lastMovePlan;
    private List<EntityID> currentMovePlan;

    private StandardEntity lastPosition;
    private StandardEntity currentPosition;

    private ActionState actionState;

    private ActionExecutorType actionExecutorType;
    private Map<ActionExecutorType, MoveActExecutor> actExecutorTypeMap;

    private int lastMoveTime;

    private Point lastPositionCoordinate;
    private Point currentPositionCoordinate;

    private MoveActExecutor moveActExecutor;
    private boolean hardWalkFailed = false;//todo should be replaced with more intelligent methods.


    public MoveActManager(MrlWorld world) {
        this.world = world;
        this.lastMovePlan = new ArrayList<>();
        this.currentMovePlan = new ArrayList<>();

        this.actExecutorTypeMap = new HashMap<>();
        this.actExecutorTypeMap.put(ActionExecutorType.REGULAR_MOVE, null);
        this.actExecutorTypeMap.put(ActionExecutorType.HARD_WALK, null);
        this.actExecutorTypeMap.put(ActionExecutorType.MOVE_BY_RAY, new RayMoveActExecutor(world));
    }


    public void moveOnPlan(List<EntityID> plan) throws CommandException, TimeOutException {

        if (plan == null) {
            return;
        }

        actionExecutorType = getActionExecutorType();
        if(actExecutorTypeMap==null){
            return;
        }

        prepareForMove(plan);

        moveActExecutor = actExecutorTypeMap.get(actionExecutorType);


        lastMoveTime = world.getTime();
        actionState = moveActExecutor.execute(plan);

        switch (actionState) {
            case SUCCEED:
                postMoveAction(true);
            case FAILED:
                postMoveAction(false);
                moveOnPlan(plan);
                break;
            case CANCELED:
                postMoveAction(false);
                moveOnPlan(plan);
                break;
        }

    }

    private void postMoveAction(boolean isSuccessful) throws CommandException {
        if (isSuccessful) {
            //update depend variables

            throw new CommandException(actionExecutorType.name());

        } else {
            //update depend variables
        }

    }

    private void prepareForMove(List<EntityID> plan) {
        lastMovePlan.clear();
        lastMovePlan.addAll(currentMovePlan);

        currentMovePlan.clear();
        currentMovePlan.addAll(plan);

        lastPosition = currentPosition;
        currentPosition = world.getSelfPosition();

        lastPositionCoordinate = currentPositionCoordinate;
        currentPositionCoordinate = new Point(world.getSelfLocation().first(), world.getSelfLocation().second());
    }

    private ActionExecutorType getActionExecutorType() {

        if (moveActExecutor != null && !moveActExecutor.acceptInterrupt()) {
            return moveActExecutor.getType();
        }


        if ((!currentMovePlan.isEmpty() && !currentMovePlan.contains(currentPosition.getID())) || stuckCondition()) {
            if (hardWalkFailed) {
                return ActionExecutorType.HARD_WALK;
            } else {
                return ActionExecutorType.MOVE_BY_RAY;
            }
        } else {
            return ActionExecutorType.REGULAR_MOVE;
        }
    }

    private boolean stuckCondition() {
        if (lastMoveTime <= world.getIgnoreCommandTime()) {
            return false;
        }

        if (!currentMovePlan.isEmpty()) {//todo Is this really need?
            EntityID target = currentMovePlan.get(currentMovePlan.size() - 1);
            if (target.equals(currentPosition.getID())) {
                return false;
            }
        }


        double moveDistance = Util.distance(lastPositionCoordinate, currentPositionCoordinate);
        if (moveDistance <= stuckThreshold) {
            return true;
        }
        return false;
    }

}
