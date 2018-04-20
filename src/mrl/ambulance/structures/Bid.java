package mrl.ambulance.structures;

import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by IntelliJ IDEA.
 * User: Pooyad
 * Date: Nov 19, 2010
 * Time: 3:08:37 AM
 */
public class Bid {

    private int AmbulanceID; // who sent the packet
    private EntityID CivilianID;
    private int timeToFree;

    public Bid() {
    }

    public Bid(int ambulanceID, EntityID civilian, int timeToFree) {
        AmbulanceID = ambulanceID;
        CivilianID = civilian;
        this.timeToFree = timeToFree;
    }

    public int getAmbulanceID() {
        return AmbulanceID;
    }

    public void setAmbulanceID(int ambulanceID) {
        AmbulanceID = ambulanceID;
    }

    public Civilian getCivilian(MrlWorld world) {
        return (Civilian) world.getEntity(CivilianID);
    }

    public void setCivilianID(EntityID civilianID) {
        CivilianID = civilianID;
    }

    public int getTimeToFree() {
        return timeToFree;
    }

    public void setTimeToFree(int timeToFree) {
        this.timeToFree = timeToFree;
    }

}
