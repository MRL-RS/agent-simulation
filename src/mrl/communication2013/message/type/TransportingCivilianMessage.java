package mrl.communication2013.message.type;

import mrl.communication2013.entities.TransportingCivilian;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:43 PM
 * Author: Mostafa Movahedi
 */
public class TransportingCivilianMessage extends AbstractMessage<TransportingCivilian> {
    int humanID;

    public TransportingCivilianMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(3);
    }

    public TransportingCivilianMessage(TransportingCivilian transportingCivilian) {
        super(transportingCivilian);
        setDefaultSayTTL(3);
        setSayTTL();
    }

    public TransportingCivilianMessage() {
        super();
        setDefaultSayTTL(3);
        setSayTTL();
        createProperties();
    }

    @Override
    public TransportingCivilian read(int sendTime) {
        return new TransportingCivilian(new EntityID(propertyValues.get(PropertyTypes.HumanID)), sendTime);
    }

    @Override
    protected void setFields(TransportingCivilian transportingCivilian) {
        this.humanID = transportingCivilian.getHumanID().getValue();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(humanID));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
        sendTypes.add(SendType.Speak);
    }

    @Override
    protected void setReceivers() {
        receivers.add(Receiver.AmbulanceTeam);
    }

    @Override
    protected void setChannelConditions() {
        channelConditions.add(ChannelCondition.High);
        channelConditions.add(ChannelCondition.Medium);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.TransportingCivilian);
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

        TransportingCivilianMessage that = (TransportingCivilianMessage) o;

        if (humanID != that.humanID) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return humanID;
    }
}
