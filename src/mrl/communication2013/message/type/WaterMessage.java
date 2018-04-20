package mrl.communication2013.message.type;

import mrl.communication2013.entities.Water;
import mrl.communication2013.entities.WaterTypes;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 2:31 PM
 * Author: Mostafa Movahedi
 */
public class WaterMessage extends AbstractMessage<Water> {
    int buildingIndex;
    int waterType;

    public WaterMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(1);
    }

    public WaterMessage(Water water) {
        super(water);
        setDefaultSayTTL(1);
        setSayTTL();
    }

    public WaterMessage() {
        super();
        setDefaultSayTTL(1);
        setSayTTL();
        createProperties();
    }

    @Override
    public Water read(int sendTime) {
        EntityID buildingID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex));
        int waterType = propertyValues.get(PropertyTypes.Water);
        return new Water(buildingID, WaterTypes.indexToEnum(waterType), sendTime);
    }

    @Override
    protected void setFields(Water water) {
        this.buildingIndex = IDConverter.getBuildingKey(water.getBuildingID());
        waterType = water.getWaterType().ordinal();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.Water, new WaterTypeProperty(waterType));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Speak);
    }

    @Override
    protected void setReceivers() {
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
        setMessageType(MessageTypes.WaterMessage);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
//        if (!super.equals(o)) return false;

        WaterMessage that = (WaterMessage) o;

        if (buildingIndex != that.buildingIndex) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return buildingIndex;
    }
}
