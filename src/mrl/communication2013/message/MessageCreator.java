package mrl.communication2013.message;

import mrl.communication2013.message.property.SendType;
import mrl.communication2013.message.type.AbstractMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Aug 26, 2010
 * Time: 11:53:50 AM
 * To change this template use File | Settings | File Templates.
 */


public class MessageCreator {
    protected Message msg;
    public List<AbstractMessage> messages = new ArrayList<AbstractMessage>();

    public MessageCreator(int len) {
        msg = new Message(len);
    }

    public void create(List<AbstractMessage> messages, SendType sendType) {
        if (messages != null)
            this.messages = messages;
        for (AbstractMessage message : this.messages) {
            if (!message.write(msg, sendType)) {
                System.out.println(message.getMessageType().name() + " : Message write Error");
            } else {
            }
        }
    }

    public Message getMessage() {
        return msg;
    }
}
