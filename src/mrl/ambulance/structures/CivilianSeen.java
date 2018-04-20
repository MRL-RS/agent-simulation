package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G.
 * User: Pooyad
 * Date: Nov 8, 2010
 * Time: 8:14:55 PM
 */
public class CivilianSeen {
    private EntityID AmbulanceID; // who sent the packet
    private EntityID CivilianID;
    private EntityID NearestRefugeID;
    private int timeToRefuge; // time to nearest refuge

    public EntityID getAmbulanceID() {
        return AmbulanceID;
    }

    public void setAmbulanceID(EntityID ambulanceID) {
        AmbulanceID = ambulanceID;
    }

    public EntityID getCivilianID() {
        return CivilianID;
    }

    public void setCivilianID(EntityID civilianID) {
        CivilianID = civilianID;
    }

    public EntityID getNearestRefugeID() {
        return NearestRefugeID;
    }

    public void setNearestRefugeID(EntityID nearestRefugeID) {
        NearestRefugeID = nearestRefugeID;
    }

    public int getTimeToRefuge() {
        return timeToRefuge;
    }

    public void setTimeToRefuge(int timeToRefuge) {
        this.timeToRefuge = timeToRefuge;
    }
}
