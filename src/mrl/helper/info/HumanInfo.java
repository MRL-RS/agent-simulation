package mrl.helper.info;

import mrl.platoon.State;
import rescuecore2.worldmodel.EntityID;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 12:39:39 PM
 */
public class HumanInfo {

    Boolean lockedByBlockade;
    Boolean leader;
    State agentState;

    // Added by P.D.G.
    //
    //
    // How long is it far from nearest available refuge
    private int timeToRefuge = 0;

    // How emergence is this Civilian for rescue, it has value 1 for the highest emergence one and 6 for the lowest one
    private int emergencyLevel = -1;

    // CAOP = Damage / ( Buriedness * TTA ) ,  TTA is Time To Arrive to a civilian by an AT
    private double cAOP = -1;

    //Benefit of accepting this to rescue in an auction
    private double benefit = 0;

    //<timeToRefuge,nearestRefuge>
    private EntityID nearestRefuge = null;

    private int timeToArrive = -1;

    private int previousBuriedness = 0;
    private int currentBuriedness = 0;
    private int numberOfATsRescuing = 0;

    private int firstHP = -10000;
    private int previousHP = 10000;
    private int currentHP = 10000;
    private int deltaHP = 0;
    private int lastTimeHPChanged = 0;

    private int firstDamage = -1;
    private int firstBuriedness = -1;

    private int previousDamage = 0;
    private int currentDamage = 0;
    private int deltaDamage = 0;
    private int lastTimeDamageChanged = 0;

    private boolean fromSense = false;

    private int distanceToFire;
    private int timeToDeath;


    public HumanInfo(Boolean lockedByBlockade, Boolean leader) {
        this.lockedByBlockade = lockedByBlockade;
        this.leader = leader;
    }

    public Boolean isLockedByBlockade() {
        return lockedByBlockade;
    }

    public Boolean isLeader() {
        return leader;
    }

    public void setLeader(Boolean leader) {
        this.leader = leader;
    }

    public void setLockedByBlockade(Boolean lockedByBlockade) {
        this.lockedByBlockade = lockedByBlockade;
    }

    public State getAgentState() {
        return agentState;
    }

    public void setAgentState(State agentState) {
        this.agentState = agentState;
    }

    public int getTimeToRefuge() {
        return timeToRefuge;
    }

    public void setTimeToRefuge(Integer timeToRefuge) {
        this.timeToRefuge = timeToRefuge;
    }

    public int getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(int emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }

    public double getCAOP() {
        return cAOP;
    }

    public void setCAOP(double cAOP) {
        this.cAOP = cAOP;
    }

    public double getBenefit() {
        return benefit;
    }

    public void setBenefit(double benefit) {
        this.benefit = benefit;
    }

    public EntityID getNearestRefugeID() {
        return nearestRefuge;
    }

    public void setNearestRefuge(EntityID nearestRefuge) {
        this.nearestRefuge = nearestRefuge;
    }

    public int getPreviousBuriedness() {
        return previousBuriedness;
    }

    public void setPreviousBuriedness(int previousBuriedness) {
        this.previousBuriedness = previousBuriedness;
    }

    public int getCurrentBuriedness() {
        return currentBuriedness;
    }

    public void setCurrentBuriedness(int currentBuriedness) {
        this.currentBuriedness = currentBuriedness;
    }

    public int getNumberOfATsRescuing() {
        return numberOfATsRescuing;
    }

    public void setNumberOfATsRescuing(int numberOfATsRescuing) {
        this.numberOfATsRescuing = numberOfATsRescuing;
    }

    public int getFirstHP() {
        return firstHP;
    }

    public void setFirstHP(int firstHP) {
        this.firstHP = firstHP;
    }

    public int getFirstDamge() {
        return firstDamage;
    }

    public void setFirstDamage(int firstDamage) {
        this.firstDamage = firstDamage;
    }

    public int getPreviousHP() {
        return previousHP;
    }

    public void setPreviousHP(int previousHP) {
        this.previousHP = previousHP;
    }

    public int getPreviousDamage() {
        return previousDamage;
    }

    public void setPreviousDamage(int previousDamage) {
        this.previousDamage = previousDamage;
    }

    public int getCurrentHP() {
        return currentHP;
    }

    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public void setCurrentDamage(int currentDamage) {
        this.currentDamage = currentDamage;
    }

    public int getDeltaDamage() {
        return deltaDamage;
    }

    public void setDeltaDamage(int deltaDamage) {
        this.deltaDamage = deltaDamage;
    }

    public int getLastTimeHPChanged() {
        return lastTimeHPChanged;
    }

    public void setLastTimeHPChanged(int lastTimeHPChanged) {
        this.lastTimeHPChanged = lastTimeHPChanged;
    }

    public int getLastTimeDamageChanged() {
        return lastTimeDamageChanged;
    }

    public void setLastTimeDamageChanged(int lastTimeDamageChanged) {
        this.lastTimeDamageChanged = lastTimeDamageChanged;
    }


    public int getDeltaHP() {
        return deltaHP;
    }

    public void setDeltaHP(int deltaHP) {
        this.deltaHP = deltaHP;
    }

    public int getTimeToArrive() {
        return timeToArrive;
    }

    public void setTimeToArrive(int timeToArrive) {
        this.timeToArrive = timeToArrive;
    }

    public boolean isFromSense() {
        return fromSense;
    }

    public void setFromSense(boolean fromSense) {
        this.fromSense = fromSense;
    }

    public int getFirstBuriedness() {
        return firstBuriedness;
    }

    public void setFirstBuriedness(int firstBuriedness) {
        this.firstBuriedness = firstBuriedness;
    }

    public int getDistanceToFire() {
        return distanceToFire;
    }

    public void setDistanceToFire(int distanceToFire) {
        this.distanceToFire = distanceToFire;
    }

    public int getTimeToDeath() {
        return timeToDeath;
    }

    public void setTimeToDeath(int timeToDeath) {
        this.timeToDeath = timeToDeath;
    }
}
