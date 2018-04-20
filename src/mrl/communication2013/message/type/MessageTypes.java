package mrl.communication2013.message.type;

import mrl.communication2013.entities.*;

/**
 * Created by IntelliJ IDEA.
 * Author: Mostafa Movahedi
 * Date: Sep 24, 2010
 * Time: 7:15:20 PM
 * To change this template use File | Settings | File Templates.
 */
public enum MessageTypes {
    NOP(NOPMessage.class, NOP.class),
    BurningBuilding(BurningBuildingMessage.class, BurningBuilding.class),
    AgentInfo(AgentInfoMessage.class, AgentInfo.class),
    AftershockEntity(AftershockMessage.class, AftershockEntity.class),
    BuriedAgent(BuriedAgentMessage.class, BuriedAgent.class),
    BuriedAgentSay(BuriedAgentSayMessage.class,BuriedAgentSay.class),
    EmptyBuilding(EmptyBuildingMessage.class, EmptyBuilding.class),
    ExtinguishedBuilding(ExtinguishedBuildingMessage.class, ExtinguishedBuilding.class),
    FullBuilding(FullBuildingMessage.class, FullBuilding.class),
    WaterMessage(WaterMessage.class, Water.class),
    HeardCivilian(HeardCivilianMessage.class, HeardCivilian.class),
    Loader(LoaderMessage.class, Loader.class),
    RescuedCivilian(RescuedCivilianMessage.class, RescuedCivilian.class),
    TransportingCivilian(TransportingCivilianMessage.class, TransportingCivilian.class),
    CivilianSeen(CivilianSeenMessage.class, CivilianSeen.class),
//    ClearedRoad(ClearedRoadMessage.class, ClearedRoad.class),
    //ClearedPath(ClearedPathMessage.class, ClearedPath.class),
    WarmBuilding(WarmBuildingMessage.class, WarmBuilding.class),;

    public Class<? extends AbstractMessage> abstractMessageClass;
    public Class<? extends MessageEntity> messageEntityClass;

    MessageTypes(Class<? extends AbstractMessage> abstractMessageClass, Class<? extends MessageEntity> messageEntityClass) {
        this.abstractMessageClass = abstractMessageClass;
        this.messageEntityClass = messageEntityClass;
    }

//    MessageTypes(Class<? extends AbstractMessage> abstractMessageClass) {
//        this.abstractMessageClass = abstractMessageClass;
//    }

    public static MessageTypes indexToEnum(int index) {
        for (MessageTypes m : MessageTypes.values()) {
            if (m.ordinal() == index)
                return m;
        }
        return null;
    }

    public static int getMessageTypesBitNum() {
        int res = 1;
        int size = MessageTypes.values().length - 1;
        while ((size >>= 1) != 0)
            res++;
        return res;
    }

}
