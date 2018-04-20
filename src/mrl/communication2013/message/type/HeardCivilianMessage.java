package mrl.communication2013.message.type;

import mrl.communication2013.entities.HeardCivilian;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 7:12 PM
 * Author: Mostafa Movahedi
 */
public class HeardCivilianMessage extends AbstractMessage<HeardCivilian> {
    int civilianID;
    int locationX;
    int locationY;

    public HeardCivilianMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public HeardCivilianMessage(HeardCivilian heardCivilian) {
        super(heardCivilian);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public HeardCivilianMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public HeardCivilian read(int sendTime) {
        EntityID civilianID = new EntityID(propertyValues.get(PropertyTypes.HumanID));
        int locationX = propertyValues.get(PropertyTypes.LocationX);
        int locationY = propertyValues.get(PropertyTypes.LocationY);
        return new HeardCivilian(civilianID, locationX, locationY, sendTime);
    }

    @Override
    protected void setFields(HeardCivilian heardCivilian) {
        civilianID = heardCivilian.getCivilianID().getValue();
        locationX = heardCivilian.getLocationX();
        locationY = heardCivilian.getLocationY();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(civilianID));
        properties.put(PropertyTypes.LocationX, new LocationXProperty(locationX));
        properties.put(PropertyTypes.LocationY, new LocationYProperty(locationY));
    }

    @Override
    protected void setSendTypes() {
//        sendTypes.add(SendType.Say); //todo.........
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
        setMessageType(MessageTypes.HeardCivilian);
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

        HeardCivilianMessage that = (HeardCivilianMessage) o;

        if (civilianID != that.civilianID) return false;
        if (locationX != that.locationX) return false;
        if (locationY != that.locationY) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = civilianID;
        result = 31 * result + locationX;
        result = 31 * result + locationY;
        return result;
    }
}
