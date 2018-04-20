package mrl.communication2013.message.type;

import mrl.communication2013.entities.WarmBuilding;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/23/13
 * Time: 4:13 PM
 *
 * @Author: Mostafa Movahedi
 */
public class WarmBuildingMessage extends AbstractMessage<WarmBuilding> {
    int buildingIndex;

    public WarmBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public WarmBuildingMessage(WarmBuilding warmBuilding) {
        super(warmBuilding);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public WarmBuildingMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public WarmBuilding read(int sendTime) {
        return new WarmBuilding(IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex)), sendTime);
    }

    @Override
    protected void setFields(WarmBuilding warmBuilding) {
        buildingIndex = IDConverter.getBuildingKey(warmBuilding.getBuildingID());
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Emergency);
    }

    @Override
    protected void setReceivers() {
//        receivers.add(Receiver.AmbulanceTeam);
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
        setMessageType(MessageTypes.WarmBuilding);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
//        if (!super.equals(o)) return false;

        WarmBuildingMessage that = (WarmBuildingMessage) o;

        if (buildingIndex != that.buildingIndex) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return buildingIndex;
    }
}
