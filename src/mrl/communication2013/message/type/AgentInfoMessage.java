package mrl.communication2013.message.type;

import mrl.communication2013.entities.AgentInfo;
import mrl.communication2013.entities.PositionTypes;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import mrl.platoon.State;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 1:18 PM
 * Author: Mostafa Movahedi
 */
public class AgentInfoMessage extends AbstractMessage<AgentInfo> {

    private int positionType;
    private int positionIndex;
    private int state;

    public AgentInfoMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public AgentInfoMessage(AgentInfo agentInfo) {
        super(agentInfo);
        setDefaultSayTTL(1);
        setSayTTL();
    }

    public AgentInfoMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    @Override
    public AgentInfo read(int sendTime) {

        EntityID positionID = null;
        PositionTypes positionType = PositionTypes.indexToEnum(propertyValues.get(PropertyTypes.PositionType));
        switch (positionType) {
            case Building:
                positionID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.PositionIndex));
                break;
            case Road:
                positionID = IDConverter.getRoadID(propertyValues.get(PropertyTypes.PositionIndex));
                break;
        }
        State state = State.getState(propertyValues.get(PropertyTypes.AgentState));

        return new AgentInfo(positionType, positionID, state, sendTime);
    }

    @Override
    public void setFields(AgentInfo agentInfo) {
        this.positionType = agentInfo.getPositionType().ordinal();
        switch (agentInfo.getPositionType()) {
            case Building:
                this.positionIndex = IDConverter.getBuildingKey(agentInfo.getPositionID());
                break;
            case Road:
                this.positionIndex = IDConverter.getRoadKey(agentInfo.getPositionID());
                break;
        }
        this.state = agentInfo.getState().ordinal();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.PositionType, new PositionTypeProperty(positionType));
        properties.put(PropertyTypes.PositionIndex, new PositionIndexProperty(positionIndex));
        properties.put(PropertyTypes.AgentState, new AgentStateProperty(state));
    }

    @Override
    protected void setSendTypes() {
//        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Speak);
    }

    @Override
    protected void setReceivers() {
        receivers.add(Receiver.AmbulanceTeam);
        receivers.add(Receiver.PoliceForce);
        receivers.add(Receiver.FireBrigade);
    }

    @Override
    protected void setChannelConditions() {
        channelConditions.add(ChannelCondition.High);
        channelConditions.add(ChannelCondition.Medium);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.AgentInfo);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AgentInfoMessage)) {
            return false;
        }

        AgentInfoMessage message = (AgentInfoMessage) obj;
        if (message.getSender() != null && message.getSender().equals(sender)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return sender == null ? -1 : sender.getValue();
    }
}
