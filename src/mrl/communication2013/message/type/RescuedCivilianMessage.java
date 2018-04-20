package mrl.communication2013.message.type;

import mrl.communication2013.entities.RescuedCivilian;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:23 PM
 * Author: Mostafa Movahedi
 */
public class RescuedCivilianMessage extends AbstractMessage<RescuedCivilian> {
    int humanID;

    public RescuedCivilianMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(20);
    }

    public RescuedCivilianMessage(RescuedCivilian rescuedCivilian) {
        super(rescuedCivilian);
        setDefaultSayTTL(20);
        setSayTTL();
    }

    public RescuedCivilianMessage() {
        super();
        setDefaultSayTTL(20);
        setSayTTL();
        createProperties();
    }

    @Override
    public RescuedCivilian read(int sendTime) {
        return new RescuedCivilian(new EntityID(propertyValues.get(PropertyTypes.HumanID)), sendTime);
    }

    @Override
    protected void setFields(RescuedCivilian rescuedCivilian) {
        this.humanID = rescuedCivilian.getHumanID().getValue();
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
        channelConditions.add(ChannelCondition.Low);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.Loader);
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

        RescuedCivilianMessage that = (RescuedCivilianMessage) o;

        if (humanID != that.humanID) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return humanID;
    }
}
