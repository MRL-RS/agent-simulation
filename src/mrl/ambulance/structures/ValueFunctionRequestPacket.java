package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G
 * User: mrl
 * Date: Oct 13, 2010
 * Time: 3:16:46 AM
 */
public class ValueFunctionRequestPacket {
    private EntityID civilianID;
    private EntityID myID;

    public EntityID getCivilianID() {
        return civilianID;
    }

    public void setCivilianID(EntityID civilianID) {
        this.civilianID = civilianID;
    }

    public EntityID getMyID() {
        return myID;
    }

    public void setMyID(EntityID myID) {
        this.myID = myID;
    }
}
