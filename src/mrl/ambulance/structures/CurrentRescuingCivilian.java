package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G.
 * User: Pooyad
 * Date: Nov 2, 2010
 * Time: 6:26:13 PM
 */

/**
 * This packet will sent, when any AT keep rescuing any civilian
 */
public class CurrentRescuingCivilian {
    private EntityID AmbulanceID; // who sent the packet
    private EntityID CivilianId;   // the civilian that is rescuing
    private int currentTime;   //currentTime of starting Rescue Action


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

    public int getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }
}
