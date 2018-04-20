package mrl.ambulance.targetSelector;

import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 2/25/13
 *         Time: 5:02 PM
 */
public class AmbulanceTarget {

    private EntityID victimID;
    private EntityID PositionID;
    private int distanceToRefuge = Integer.MAX_VALUE;
    private int distanceToPartition= Integer.MAX_VALUE;
    private int distanceToMe= Integer.MAX_VALUE;
    private double victimSituation;
    private int timeToDeath;
    private double value;
    private int numberOfNeededAmulances;

    private double cost;

    private int estimatedHP;
    private int estimatedDamage;
    private List<EntityID> rescuingAmbulances = new ArrayList<>();


    public AmbulanceTarget(EntityID targetHumanID) {
        this.victimID = targetHumanID;
    }

    public EntityID getVictimID() {
        return victimID;
    }

    public EntityID getPositionID() {
        return PositionID;
    }

    public void setPositionID(EntityID positionID) {
        PositionID = positionID;
    }

    public int getDistanceToRefuge() {
        return distanceToRefuge;
    }

    public void setDistanceToRefuge(int distanceToRefuge) {
        this.distanceToRefuge = distanceToRefuge;
    }

    public int getDistanceToPartition() {
        return distanceToPartition;
    }

    public void setDistanceToPartition(int distanceToPartition) {
        this.distanceToPartition = distanceToPartition;
    }

    public int getDistanceToMe() {
        return distanceToMe;
    }

    public void setDistanceToMe(int distanceToMe) {
        this.distanceToMe = distanceToMe;
    }

    public double getVictimSituation() {
        return victimSituation;
    }

    public void setVictimSituation(double victimSituation) {
        this.victimSituation = victimSituation;
    }

    public int getTimeToDeath() {
        return timeToDeath;
    }

    public void setTimeToDeath(int timeToDeath) {
        this.timeToDeath = timeToDeath;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getEstimatedHP() {
        return estimatedHP;
    }

    public void setEstimatedHP(int estimatedHP) {
        this.estimatedHP = estimatedHP;
    }

    public int getEstimatedDamage() {
        return estimatedDamage;
    }

    public void setEstimatedDamage(int estimatedDamage) {
        this.estimatedDamage = estimatedDamage;
    }

    public int getNumberOfNeededAmulances() {
        return numberOfNeededAmulances;
    }

    public void setNumberOfNeededAmulances(int numberOfNeededAmulances) {
        this.numberOfNeededAmulances = numberOfNeededAmulances;
    }

    public List<EntityID> getRescuingAmbulances() {
        return rescuingAmbulances;
    }

    public void setRescuingAmbulances(List<EntityID> rescuingAmbulances) {
        this.rescuingAmbulances = rescuingAmbulances;
    }
}
