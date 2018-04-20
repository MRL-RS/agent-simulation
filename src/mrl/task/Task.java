package mrl.task;

import mrl.police.moa.Target;

/**
 * @author Siavash
 */
public class Task {
    Target target;
    PoliceActionStyle actionStyle;
    MoveStyle moveStyle;
    boolean started;

    public Task(Target target, PoliceActionStyle actionStyle, MoveStyle moveStyle) {
        this.target = target;
        this.actionStyle = actionStyle;
        this.moveStyle = moveStyle;
    }

    public Task(Target target, PoliceActionStyle actionStyle) {
        this.target = target;
        this.actionStyle = actionStyle;
    }

    public Target getTarget() {
        return target;
    }

    public PoliceActionStyle getActionStyle() {
        return actionStyle;
    }

    public MoveStyle getMoveStyle() {
        return moveStyle;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Task)) {
            return false;
        }
        Task task = (Task) obj;
        if (task.getTarget().equals(target)) {
            return true;
        }
        return false;
    }
}
