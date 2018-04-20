package mrl.communication2013.message.type;

import mrl.communication2013.entities.Loader;
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
public class LoaderMessage extends AbstractMessage<Loader> {
    int humanID;

    public LoaderMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
        setDefaultSayTTL(2);
    }

    public LoaderMessage(Loader loader) {
        super(loader);
        setDefaultSayTTL(2);
        setSayTTL();
    }

    public LoaderMessage() {
        super();
        setDefaultSayTTL(2);
        setSayTTL();
        createProperties();
    }

    @Override
    public Loader read(int sendTime) {
        return new Loader(new EntityID(propertyValues.get(PropertyTypes.HumanID)), sendTime);
    }

    @Override
    protected void setFields(Loader loader) {
        this.humanID = loader.getHumanID().getValue();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.HumanID, new HumanIDProperty(humanID));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Say);
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

        LoaderMessage that = (LoaderMessage) o;

        if (humanID != that.humanID) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return humanID;
    }
}
