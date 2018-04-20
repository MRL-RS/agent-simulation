package mrl.communication2013.message;

import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.message.type.AbstractMessage;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Sep 9, 2010
 * Time: 7:27:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageEntities {
    private Collection<MessageEntity> messageEntities = new ArrayList<MessageEntity>();
    private Collection<AbstractMessage> messages = new ArrayList<AbstractMessage>();

    public void merge(MessageEntities messageEntities) {
        this.messageEntities.addAll(messageEntities.getMessageEntities());
        this.messages.addAll(messageEntities.getMessages());
    }

    public Collection<MessageEntity> getMessageEntities() {
        return messageEntities;
    }

    public void setMessageEntities(Collection<MessageEntity> messageEntities) {
        this.messageEntities = messageEntities;
    }

    public Collection<AbstractMessage> getMessages() {
        return messages;
    }

    public void setMessages(Collection<AbstractMessage> messages) {
        this.messages = messages;
    }
}
