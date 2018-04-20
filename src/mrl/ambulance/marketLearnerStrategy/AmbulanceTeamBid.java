package mrl.ambulance.marketLearnerStrategy;

import rescuecore2.worldmodel.EntityID;

/**
 * User: pooyad
 * Date: 3/24/11
 * Time: 12:12 AM
 */
public class AmbulanceTeamBid {
    private EntityID bidderID;
    private EntityID humanID;
    boolean isCivilian;
    private int bidValue;

    public EntityID getBidderID() {
        return bidderID;
    }

    public void setBidderID(EntityID bidderID) {
        this.bidderID = bidderID;
    }

    public EntityID getHumanID() {
        return humanID;
    }

    public void setHumanID(EntityID humanID) {
        this.humanID = humanID;
    }

    public boolean isCivilian() {
        return isCivilian;
    }

    public void setCivilian(boolean civilian) {
        isCivilian = civilian;
    }

    public int getBidValue() {
        return bidValue;
    }

    public void setBidValue(int bidValue) {
        this.bidValue = bidValue;
    }
}
