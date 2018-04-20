package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 13, 2010
 * Time: 3:02:29 AM
 */
public class ShouldGetReward {

    private EntityID civilianID;
    private int stateValue;
    private int totalStateTransitionsBeforeIt;
    private int actionValue;
    private double nextStateBestValue;
    private double computedReward;
    private boolean gotReward = false;
    private boolean willDead = false;

    public EntityID getCivilianID() {
        return civilianID;
    }

    public void setVictimID(EntityID civilianID) {
        this.civilianID = civilianID;
    }

    public int getStateValue() {
        return stateValue;
    }

    public void setStateValue(int stateValue) {
        this.stateValue = stateValue;
    }

    public int getActionValue() {
        return actionValue;
    }

    public void setActionValue(int actionValue) {
        this.actionValue = actionValue;
    }

    public double getNextStateBestValue() {
        return nextStateBestValue;
    }

    public void setNextStateBestValue(double nextStateBestValue) {
        this.nextStateBestValue = nextStateBestValue;
    }

    public double getComputedReward() {
        return computedReward;
    }

    public void setComputedReward(double computedReward) {
        this.computedReward = computedReward;
    }

    public boolean isGotReward() {
        return gotReward;
    }

    public void setGotReward(boolean gotReward) {
        this.gotReward = gotReward;
    }

    public int getTotalStateTransitionsBeforeIt() {
        return totalStateTransitionsBeforeIt;
    }

    public void setTotalStateTransitionsBeforeIt(int totalStateTransitionsBeforeIt) {
        this.totalStateTransitionsBeforeIt = totalStateTransitionsBeforeIt;
    }

    public boolean isWillDead() {
        return willDead;
    }

    public void setWillDead(boolean willDead) {
        this.willDead = willDead;
    }
}
