package mrl.police.moa;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 1/27/12
 * Time: 6:46 PM
 */

import rescuecore2.worldmodel.EntityID;

import java.util.Map;


/**
 * It is a class to hold target properties
 */
public class Target {
    private EntityID id;
    private EntityID positionID;
    private int importance;
    private Importance importanceType;
    private EntityID nearestRoadID;
    private Map<EntityID, Boolean> roadsToMove;  // road and its openness
    private int distanceToIt;

    public Target(EntityID id, EntityID positionID, int importance, Importance importanceType) {
        this.id = id;
        this.positionID = positionID;
        this.importance = importance;
        this.importanceType = importanceType;
    }

    public Target(EntityID id, int importance) {
        this.id = id;
        this.importance = importance;
    }

    public EntityID getId() {
        return id;
    }

    public EntityID getPositionID() {
        return positionID;
    }

    public int getImportance() {
        return importance;
    }

    public void setImportance(int importance) {
        this.importance = importance;
    }

    public Importance getImportanceType() {
        return importanceType;
    }

    public EntityID getNearestRoadID() {
        return nearestRoadID;
    }

    public void setNearestRoadID(EntityID nearestRoadID) {
        this.nearestRoadID = nearestRoadID;
    }

    public int getDistanceToIt() {
        return distanceToIt;
    }

    public void setDistanceToIt(int distanceToIt) {
        this.distanceToIt = distanceToIt;
    }

    public Map<EntityID, Boolean> getRoadsToMove() {
        return roadsToMove;
    }

    public void setRoadsToMove(Map<EntityID, Boolean> roadsToMove) {
        this.roadsToMove = roadsToMove;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Target)) {
            return false;
        }
        Target target = (Target) obj;
        if (target.getId().equals(getId()) && target.getPositionID().equals(getPositionID()) && target.getImportanceType().equals(getImportanceType())) {
            return true;
        }
        return false;
    }

    public String toString(){
        return "EntityID: " + id + "   Importance: " + importanceType + "{"+importance+")";
    }
}
