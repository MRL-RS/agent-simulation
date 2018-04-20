package mrl.communication2013.message.type;

import javolution.util.FastMap;
import mrl.communication2013.message.Message;
import mrl.communication2013.message.property.*;
import mrl.world.MrlWorld;
import rescuecore2.worldmodel.EntityID;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 *
 * @Author: Mostafa Movahedi
 * Date: Sep 24, 2010
 * Time: 6:12:23 PM
 */
public abstract class AbstractMessage<MessageEntity> {
    public static final int MsgType_Bit_num = MessageTypes.getMessageTypesBitNum();
    public static final int Say_TTL_Bit_num = 5;
    protected int messageBitSize;
    protected int messageByteSize;
    protected MessageTypes msgType = null;
    protected boolean sendable = false;
    protected EntityID sender;
    protected Map<PropertyTypes, Integer> propertyValues;
    protected Set<SendType> sendTypes = new HashSet<SendType>(); // message will be send in these sending types (say, speak or emergency speak)
    protected Set<Receiver> receivers = new HashSet<Receiver>(); // receivers of this message
    protected Set<ChannelCondition> channelConditions = new HashSet<ChannelCondition>(); // message will be send in these channel conditions
    protected int sayTTL = 1;
    protected int speakTTL = 1;
    protected int emergencyTTL = 1;
    protected int defaultSayTTL = 1;
    protected int defaultSpeakTTL = 1;
    protected int defaultEmergencyTTL = 1;

    protected Map<PropertyTypes, AbstractProperty> properties = new FastMap<PropertyTypes, AbstractProperty>();

    public AbstractMessage(Message msgToRead, SendType sendType) {
        this();
        createProperties();
        propertyValues = getPropertyValues(msgToRead, sendType);
        fillProperties();
    }

    protected void fillProperties() {
        for (PropertyTypes propertyType : properties.keySet()) {
            properties.get(propertyType).setValue(propertyValues.get(propertyType));
        }
    }

    public AbstractMessage(MessageEntity messageEntity/* , int time*/) {
        this();
        sendable = true;
        setFields(messageEntity);
        createProperties();
        setPropertyValues();
    }

    protected void setSayTTL() {
        setSayTTL(defaultSayTTL);
    }

    public int getSendTime(SendType sendType, int time) {
        int sendTime = time;
        switch (sendType) {
            case Say:
                sendTime = time - (getDefaultSayTTL() - getSayTTL()) - 1;
                break;
            case Speak:
                sendTime = time - (getDefaultSpeakTTL() - getSpeakTTL()) - 1;
                break;
            case Emergency:
                sendTime = time - (getDefaultEmergencyTTL() - getEmergencyTTL()) - 1;
                break;
        }
        return sendTime;
    }

    public AbstractMessage() {
        setMessageType();
        setSendTypes();
        setReceivers();
        setChannelConditions();
    }

    protected void setPropertyValues() {
        propertyValues = new FastMap<PropertyTypes, Integer>();
        for (PropertyTypes propertyType : properties.keySet()) {
            propertyValues.put(propertyType, properties.get(propertyType).getValue());
        }
    }

    public boolean write(Message msg, SendType sendType) {
        if (msg.getPtr() + MsgType_Bit_num + getMessageBitSize(sendType) >= msg.getBitSize())
            return false;
        if (!sendable) {
            System.out.println("UnSendable " + getClass().getName());
            return false;
        }
        int msgSize = 0;
        try {
            if (!msg.write(msgType.ordinal(), MsgType_Bit_num)) {
                System.out.println(getClass().getName() + " write error : " + msgType.ordinal() + " bits:" + MsgType_Bit_num);
                return false;
            }
            msgSize += MsgType_Bit_num;
            if (sendType.equals(SendType.Say)) {
                if (!msg.write(sayTTL, Say_TTL_Bit_num)) {
                    System.out.println(getClass().getName() + " sayTTL write error : " + sayTTL + " bits:" + Say_TTL_Bit_num);
                }
            }
            msgSize += Say_TTL_Bit_num;
            for (AbstractProperty property : properties.values()) {
                if (!property.write(msg)) {
                    clearMessage(msg, msgSize);
                    return false;
                }
                msgSize += property.getPropertyBitSize();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    abstract public MessageEntity read(int sendTime);

    abstract protected void setFields(MessageEntity messageEntity);

    abstract protected void createProperties();

    abstract protected void setSendTypes();

    abstract protected void setReceivers();

    abstract protected void setChannelConditions();

    public boolean shouldSend(MrlWorld world, SendType sendType, ChannelCondition channelCondition) {
        boolean shouldSend = true;
        if (!getSendTypes().contains(sendType)) {
            shouldSend = false;
        }
        if (channelCondition != null && !getChannelConditions().contains(channelCondition)) {
            shouldSend = false;
        }
        if (sendType.equals(SendType.Say)) {
            sayTTL--;
            if (sayTTL < 0) {
                shouldSend = false;
            }
        }
        if (sendType.equals(SendType.Speak)) {
            speakTTL--;
            if (speakTTL < 0) {
                shouldSend = false;
            }
        }
        if (sendType.equals(SendType.Emergency)) {
            emergencyTTL--;
            if (emergencyTTL < 0) {
                shouldSend = false;
            }
        }
        return shouldSend;
    }

    protected Map<PropertyTypes, Integer> getPropertyValues(Message msg, SendType sendType) {
        Map<PropertyTypes, Integer> propertyValues = new FastMap<PropertyTypes, Integer>();
        if (sendType.equals(SendType.Say)) {
            sayTTL = msg.read(Say_TTL_Bit_num, false);
        }
        for (PropertyTypes propertyType : properties.keySet()) {
            propertyValues.put(propertyType, properties.get(propertyType).read(msg));
        }
        return propertyValues;
    }

    public int getMessageBitSize(SendType sendType) {
        int size = MsgType_Bit_num;
        if (sendType.equals(SendType.Say)) size += Say_TTL_Bit_num;
        for (AbstractProperty property : properties.values()) {
            size += property.getPropertyBitSize();
        }
        return size;
    }

//    public void setMessageBitSize(int messageBitSize) {
//        this.messageBitSize = messageBitSize;
//    }

    protected abstract void setMessageType();

    public void setMessageType(MessageTypes msgType) {
        this.msgType = msgType;
    }

    public MessageTypes getMessageType() {
        return msgType;
    }

    protected void clearMessage(Message msg, int msgsize) {
        msg.setPtr(msg.getPtr() - msgsize);
        msg.write(0, msgsize);
        msg.setPtr(msg.getPtr() - msgsize);
    }

    public EntityID getSender() {
        return sender;
    }

    public void setSender(EntityID sender) {
        this.sender = sender;
    }

    public Map<PropertyTypes, AbstractProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<PropertyTypes, AbstractProperty> properties) {
        this.properties = properties;
    }

    public Map<PropertyTypes, Integer> getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(Map<PropertyTypes, Integer> propertyValues) {
        this.propertyValues = propertyValues;
    }

    public Set<SendType> getSendTypes() {
        return sendTypes;
    }

    public void setSendTypes(Set<SendType> sendTypes) {
        this.sendTypes = sendTypes;
    }

    public Set<Receiver> getReceivers() {
        return receivers;
    }

    public void setReceivers(Set<Receiver> receivers) {
        this.receivers = receivers;
    }

    public Set<ChannelCondition> getChannelConditions() {
        return channelConditions;
    }

    public void setChannelConditions(Set<ChannelCondition> channelConditions) {
        this.channelConditions = channelConditions;
    }

    public int getMessageByteSize(SendType sendType) {
        int bitSize = getMessageBitSize(sendType);
        return (int) Math.ceil(bitSize / 8f);
    }

    public int getSayTTL() {
        return sayTTL;
    }

    public void setSayTTL(int sayTTL) {
        this.sayTTL = sayTTL;
    }

    public int getEmergencyTTL() {
        return emergencyTTL;
    }

    public void setEmergencyTTL(int emergencyTTL) {
        this.emergencyTTL = emergencyTTL;
    }

    public int getSpeakTTL() {
        return speakTTL;
    }

    public void setSpeakTTL(int speakTTL) {
        this.speakTTL = speakTTL;
    }

    public boolean isSendable() {
        return sendable;
    }

    public void setSendable(boolean sendable) {
        this.sendable = sendable;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AbstractMessage)) {
            return false;
        }
        AbstractMessage message = (AbstractMessage) obj;
        if (!message.getMessageType().equals(this.getMessageType())) {
            return false;
        }
        for (PropertyTypes propertyType : properties.keySet()) {
            if (!(propertyValues.get(propertyType).equals(message.getPropertyValues().get(propertyType)))) {
                return false;
            }
        }
        return true;
    }

    public int getDefaultSayTTL() {
        return defaultSayTTL;
    }

    public void setDefaultSayTTL(int defaultSayTTL) {
        this.defaultSayTTL = defaultSayTTL;
    }

    public int getDefaultSpeakTTL() {
        return defaultSpeakTTL;
    }

    public void setDefaultSpeakTTL(int defaultSpeakTTL) {
        this.defaultSpeakTTL = defaultSpeakTTL;
    }

    public int getDefaultEmergencyTTL() {
        return defaultEmergencyTTL;
    }

    public void setDefaultEmergencyTTL(int defaultEmergencyTTL) {
        this.defaultEmergencyTTL = defaultEmergencyTTL;
    }

    @Override
    public String toString() {
        String str = msgType.toString();
        for (PropertyTypes p : propertyValues.keySet()) {
            str += " " + p + ":" + propertyValues.get(p);
        }
        str += " EmergencyTTL=" + emergencyTTL + " SpeakTTL=" + speakTTL + " SayTTL=" + sayTTL;
        return str;
    }

    public String toStringPropertyValues() {
        String str = "";
        for (PropertyTypes p : propertyValues.keySet()) {
            str += " " + p + ":" + propertyValues.get(p);
        }
        return str;
    }
}
