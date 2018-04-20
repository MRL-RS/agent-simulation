package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * Created by P.D.G
 * User: mrl
 * Date: Oct 13, 2010
 * Time: 3:20:06 AM
 */
public class ValueFunctionReplyPacket {
    private EntityID civilianID;
    private EntityID RequesterID;
    private EntityID myID;
    private double valueFunction;

    public EntityID getCivilianID() {
        return civilianID;
    }

    public void setCivilianID(EntityID civilianID) {
        this.civilianID = civilianID;
    }

    public EntityID getRequesterID() {
        return RequesterID;
    }

    public void setRequesterID(EntityID requesterID) {
        RequesterID = requesterID;
    }

    public EntityID getMyID() {
        return myID;
    }

    public void setMyID(EntityID myID) {
        this.myID = myID;
    }

    public double getValueFunction() {
        return valueFunction;
    }

    public void setValueFunction(double valueFunction) {
        this.valueFunction = valueFunction;
    }
}
