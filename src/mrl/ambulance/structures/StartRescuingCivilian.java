package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Oct 21, 2010
 * Time: 8:28:51 PM
 */


/**
 * This packet will sent, when any AT Starts rescuing any civilian
 */
public class StartRescuingCivilian {

    private EntityID AmbulanceID; // who sent the packet
    private EntityID CivilianId;   // the civilian that is rescuing
    private int startTime;   //time of starting Rescue Action


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

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }
}
