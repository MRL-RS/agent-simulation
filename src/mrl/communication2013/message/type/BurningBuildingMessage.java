package mrl.communication2013.message.type;

import mrl.communication2013.entities.BurningBuilding;
import mrl.communication2013.message.IDConverter;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by IntelliJ IDEA.
 * User: Mostafa
 * Date: Sep 24, 2010
 * Time: 6:01:16 PM
 */
public class BurningBuildingMessage extends AbstractMessage<BurningBuilding> {

    private int buildingIndex;
    private int fieryness;
    private int temperature;

    public BurningBuildingMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(10);
    }

    public BurningBuildingMessage(BurningBuilding burningBuilding) {
        super(burningBuilding);
        setDefaultSayTTL(10);
        setSayTTL();
    }

    public BurningBuildingMessage() {
        super();
        setDefaultSayTTL(10);
        setSayTTL();
        createProperties();
    }

    @Override
    public void setFields(BurningBuilding burningBuilding) {
        this.buildingIndex = IDConverter.getBuildingKey(burningBuilding.getID());
        this.fieryness = burningBuilding.getFieryness();
        this.temperature = burningBuilding.getTemperature();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.BuildingIndex, new BuildingIndexProperty(buildingIndex));
        properties.put(PropertyTypes.Fieryness, new FierynessProperty(fieryness));
        properties.put(PropertyTypes.Temperature, new TemperatureProperty(temperature));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Emergency);
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
        setMessageType(MessageTypes.BurningBuilding);
    }

    @Override
    public BurningBuilding read(int sendTime) {
        EntityID buildingID = IDConverter.getBuildingID(propertyValues.get(PropertyTypes.BuildingIndex));
        BurningBuilding burningBuilding = new BurningBuilding();
        burningBuilding.setID(buildingID);
        burningBuilding.setFieryness(propertyValues.get(PropertyTypes.Fieryness));
        burningBuilding.setTemperature(propertyValues.get(PropertyTypes.Temperature));
        burningBuilding.setSendTime(sendTime);
        return burningBuilding;
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
