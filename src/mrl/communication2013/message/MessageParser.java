package mrl.communication2013.message;

import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.message.property.SendType;
import mrl.communication2013.message.type.AbstractMessage;
import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Aug 26, 2010
 * Time: 7:16:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class MessageParser {

    private Message msg;
    private EntityID sender;

    public MessageParser(Message msg, EntityID sender) {
        this.msg = new Message(msg.getBytes());
        this.sender = sender;
    }

    public MessageEntities readMessage(SendType sendType, int time) {
        msg.resetPtr();
        MessageEntities result = new MessageEntities();
        Collection<MessageEntity> messageEntities = new LinkedList<MessageEntity>();
        Collection<AbstractMessage> messages = new LinkedList<AbstractMessage>();
        int index = msg.read(AbstractMessage.MsgType_Bit_num, sendType.equals(SendType.Say));
        MessageTypes msgType = MessageTypes.indexToEnum(index);
        boolean endMessage = false;
        try {
            AbstractMessage message = null;
            while (msgType != MessageTypes.NOP && msg.getBitSize() > msg.getPtr()) {
                if (MessageTypes.valueOf(msgType.name()) == null) {
                    endMessage = true;
                    message = null;
                    break;
                }
                message = msgType.abstractMessageClass.getConstructor(Message.class, SendType.class).newInstance(msg, sendType);
                /*switch (msgType) {
                    default:
                        endMessage = true;
                        message = null;
                        break;
                    case NOP:
                        break;
                    case BurningBuilding:
                        message = new BurningBuildingMessage(msg);
                        break;
                    case AgentInfo:
                        message = new AgentInfoMessage(msg);
                        break;
                    case BuriedAgent:
                        message = new BuriedAgentMessage(msg);
                        break;
                    case BlockedRoad:
                        message = new BlockedRoadMessage(msg);
                        break;
                    case ClearedRoad:
                        message = new ClearedRoadMessage(msg);
                        break;
                    case EmptyBuilding:
                        message = new EmptyBuildingMessage(msg);
                        break;
                    case ExtinguishedBuilding:
                        message = new ExtinguishedBuildingMessage(msg);
                        break;
                    case StuckAgent:
                        message = new StuckAgentMessage(msg);
                        break;
                    case FullBuilding:
                        message = new FullBuildingMessage(msg);
                        break;
                    case VisitedBuilding:
                        message = new VisitedBuildingMessage(msg);
                        break;
                    case WaterMessage:
                        message = new WaterMessage(msg);
                        break;
                    case HeardCivilian:
                        message = new HeardCivilianMessage(msg);
                        break;
                    case Loader:
                        message = new LoaderMessage(msg);
                        break;
                    case RescuedCivilian:
                        message = new RescuedCivilianMessage(msg);
                        break;
                    case StartRescuingCivilian:
                        message = new StartRescuingCivilianMessage(msg);
                        break;
                    case TransportingCivilian:
                        message = new TransportingCivilianMessage(msg);
                        break;
                    case CivilianSeen:
                        message = new CivilianSeenMessage(msg);
                        break;
                    case ChannelScanner:
                        message = new ChannelScannerMessage(msg);
                        break;
                }*/
                if (message != null) {
                    messageEntities.add((MessageEntity) message.read(message.getSendTime(sendType, time)));
                }
                messages.add(message);
                if (msg.getBitSize() - msg.getPtr() < AbstractMessage.MsgType_Bit_num || endMessage)
                    break;
                index = msg.read(AbstractMessage.MsgType_Bit_num, sendType.equals(SendType.Say));
                msgType = MessageTypes.indexToEnum(index);
            }
        } catch (Exception e) {
            System.out.println("Message pars error");
            System.out.println("MessageIndex: " + index);
            e.printStackTrace();
        }
        msg.resetPtr();
        for (MessageEntity c : messageEntities)
            c.setSender(sender);
        for (AbstractMessage m : messages) {
            m.setSender(sender);
        }
        result.setMessageEntities(messageEntities);
        result.setMessages(messages);
        return result;
    }
}
