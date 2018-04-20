package mrl.communication2013.message.type;

import mrl.communication2013.entities.ExtinguishedBuilding;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 1:19 PM
 *
 * @Author: Mostafa Movahedi
 */
public class ExtinguishedBuildingMessage extends AbstractMessage<ExtinguishedBuilding> {
    private int buildingIndex;
    private int fieryness;

    public ExtinguishedBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(15);
    }

    public ExtinguishedBuildingMessage(ExtinguishedBuilding extinguishedBuilding) {
        super(extinguishedBuilding);
        setDefaultSayTTL(15);
        setSayTTL();
    }

    public ExtinguishedBuildingMessage() {
        super();
        setDefaultSayTTL(15);
        setSayTTL();
        createProperties();
    }

    @Override

    public ExtinguishedBuilding read(int sendTime) {
        EntityID buildingID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex));
        return new ExtinguishedBuilding(buildingID, propertyValues.get(PropertyTypes.Fieryness), sendTime);
    }

    @Override
    public void setFields(ExtinguishedBuilding extinguishedBuilding) {
        this.buildingIndex = IDConverter.getBuildingKey(extinguishedBuilding.getBuildingID());
        this.fieryness = extinguishedBuilding.getFieryness();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.Fieryness, new FierynessProperty(fieryness));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Speak);
    }

    @Override
    protected void setReceivers() {
//        receivers.add(Receiver.AmbulanceTeam);
//        receivers.add(Receiver.PoliceForce);
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
        setMessageType(MessageTypes.ExtinguishedBuilding);
    }

    @Override
    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    @Override
    public boolean equals(Object message) {
        if (!(message instanceof ExtinguishedBuildingMessage) && !(message instanceof BurningBuildingMessage)) {
            return false;
        } else if (message instanceof ExtinguishedBuildingMessage) {
            return (getPropertyValues().get(PropertyTypes.BuildingIndex).equals(((ExtinguishedBuildingMessage) message).getPropertyValues().get(PropertyTypes.BuildingIndex)));
        } else {
            return (getPropertyValues().get(PropertyTypes.BuildingIndex).equals(((BurningBuildingMessage) message).getPropertyValues().get(PropertyTypes.BuildingIndex)));
        }
    }

    @Override
    public int hashCode() {
        return getPropertyValues().get(PropertyTypes.BuildingIndex);
    }
}
