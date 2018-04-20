package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Sep 9, 2010
 * Time: 7:21:28 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MessageEntity {
    private EntityID sender;
    //    private int receiveTime;
    private MessageTypes messageEntityType;
    int sendTime = 0;

    public MessageEntity(int sendTime) {
        setMessageEntityType();
        this.sendTime = sendTime;
    }

    public MessageEntity() {
        setMessageEntityType();
    }

    public EntityID getSender() {
        return sender;
    }

    public void setSender(EntityID sender) {
        this.sender = sender;
    }

    public int getSendTime() {
        return sendTime;
    }

    public void setSendTime(int sendTime) {
        this.sendTime = sendTime;
    }

    public MessageTypes getMessageEntityType() {
        return messageEntityType;
    }

    public void setMessageEntityType(MessageTypes messageEntityType) {
        this.messageEntityType = messageEntityType;
    }

    abstract protected void setMessageEntityType();
}
