package mrl.communication2013.message.type;

import mrl.communication2013.entities.AftershockEntity;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 3/30/14
 *         Time: 10:03 PM
 */
public class AftershockMessage extends AbstractMessage<AftershockEntity>{
    private int aftershockCount;

    public AftershockMessage(Message msgToRead, SendType sendType) {
        super(msgToRead, sendType);
    }

    public AftershockMessage(AftershockEntity aftershockEntity) {
        super(aftershockEntity);
    }

    public AftershockMessage() {
        createProperties();
    }

    @Override
    public AftershockEntity read(int sendTime) {
        int aftershockCount = propertyValues.get(PropertyTypes.AFTERSHOCK_COUNT);
        return new AftershockEntity(sendTime, aftershockCount);
    }

    @Override
    protected void setFields(AftershockEntity aftershockEntity) {
        this.aftershockCount=aftershockEntity.getAftershockCount();
    }

    @Override
    protected void createProperties() {
        properties.put(PropertyTypes.AFTERSHOCK_COUNT,new AftershockProperty(aftershockCount));
    }

    @Override
    protected void setSendTypes() {
        sendTypes.add(SendType.Speak);
    }

    @Override
    protected void setReceivers() {
        receivers.add(Receiver.PoliceForce);
    }

    @Override
    protected void setChannelConditions() {
        channelConditions.add(ChannelCondition.High);
        channelConditions.add(ChannelCondition.Medium);
        channelConditions.add(ChannelCondition.Low);
    }

    @Override
    protected void setMessageType() {
        setMessageType(MessageTypes.AftershockEntity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AftershockMessage that = (AftershockMessage) o;

        if (aftershockCount != that.aftershockCount) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return aftershockCount;
    }
}
