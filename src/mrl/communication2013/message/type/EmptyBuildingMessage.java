package mrl.communication2013.message.type;

import mrl.communication2013.entities.EmptyBuilding;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:14 PM
 * Author: Mostafa Movahedi
 */
public class EmptyBuildingMessage extends AbstractMessage<EmptyBuilding> {
    private int buildingIndex;

    public EmptyBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(10);
    }

    public EmptyBuildingMessage(EmptyBuilding emptyBuilding) {
        super(emptyBuilding);
        setDefaultSayTTL(10);
        setSayTTL();
    }

    public EmptyBuildingMessage() {
        super();
        setDefaultSayTTL(10);
        setSayTTL();
        createProperties();
    }

    @Override
    public EmptyBuilding read(int sendTime) {
        return new EmptyBuilding(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)), sendTime);
    }

    @Override
    public void setFields(EmptyBuilding emptyBuilding) {
        this.buildingIndex = IDConverter.getBuildingKey(emptyBuilding.getBuildingID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
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
        channelConditions.add(ChannelCondition.Low);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.EmptyBuilding);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }


    @Override
    public int hashCode() {
        return getPropertyValues().get(PropertyTypes.BuildingIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
//        if (!super.equals(o)) return false;

        EmptyBuildingMessage that = (EmptyBuildingMessage) o;

        if (buildingIndex != that.buildingIndex) return false;

        return true;
    }
}
