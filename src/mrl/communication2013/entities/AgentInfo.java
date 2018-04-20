package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import mrl.platoon.State;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 1:41 PM
 *
 * @Author: Mostafa Movahedi
 */
public class AgentInfo extends MessageEntity {
    PositionTypes positionType;
    EntityID positionID;
    State state;

    public AgentInfo(PositionTypes positionType, EntityID positionID, State state, int sendTime) {
        super(sendTime);
        this.positionType = positionType;
        this.positionID = positionID;
        this.state = state;
    }

    public AgentInfo() {
        super();
    }

    public PositionTypes getPositionType() {
        return positionType;
    }

    public void setPositionType(PositionTypes positionType) {
        this.positionType = positionType;
    }

    public EntityID getPositionID() {

        return positionID;
    }

    public void setPositionID(EntityID positionID) {
        this.positionID = positionID;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return positionID + " , " + state;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.AgentInfo);
    }
}
