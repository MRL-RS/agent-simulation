package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 12, 2010
 * Time: 7:25:35 PM
 */
public class RescuedCivilian {

    private EntityID AmbulanceID; // who sent the packet
    private EntityID CivilianId;
    private int hP;   //hp value when this civilian unloaded at Refuge
    private int totalATsInThisRescue;
    private int totalRescueTime;


    public EntityID getAmbulanceID() {
        return AmbulanceID;
    }

    public void setAmbulanceID(EntityID ambulanceID) {
        AmbulanceID = ambulanceID;
    }

    public EntityID getCivilianId() {
        return CivilianId;
    }

    public void setCivilianId(EntityID civilianId) {
        CivilianId = civilianId;
    }

    public int getHP() {
        return hP;
    }

    public void setHP(int hP) {
        this.hP = hP;
    }

    public int getTotalATsInThisRescue() {
        return totalATsInThisRescue;
    }

    public void setTotalATsInThisRescue(int totalATsInThisRescue) {
        this.totalATsInThisRescue = totalATsInThisRescue;
    }

    public int getTotalRescueTime() {
        return totalRescueTime;
    }

    public void setTotalRescueTime(int totalRescueTime) {
        this.totalRescueTime = totalRescueTime;
    }
}
