package mrl.communication2013;

import mrl.communication2013.message.property.SendType;
import mrl.communication2013.message.type.AbstractMessage;
import mrl.communication2013.message.type.MessageTypes;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/22/13
 * Time: 12:48 PM
 * Author: Mostafa Movahedi
 */
public class MessageTester {
    public static void main(String[] args) {

        for (MessageTypes messageType : MessageTypes.values()) {
            try {
                Class<? extends AbstractMessage> messageClass = messageType.abstractMessageClass;
                AbstractMessage message = messageClass.newInstance();
//                Constructor<? extends AbstractMessage> constructor = messageClass.getConstructor(messageType.messageEntityClass);
//                AbstractMessage message = constructor.newInstance(messageType.messageEntityClass.newInstance());

                int msgBitSize = message.getMessageBitSize(SendType.Say);
                int msgByteSize = message.getMessageByteSize(SendType.Say);
                System.out.println(msgByteSize + "(" + msgBitSize + ")\t" + messageType.name());
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }
}
