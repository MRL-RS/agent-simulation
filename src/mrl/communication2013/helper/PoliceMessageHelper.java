package mrl.communication2013.helper;

//import mrl.communication2013.entities.ClearedPath;

import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.message.MessageManager;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForceWorld;
import rescuecore2.worldmodel.EntityID;

//import mrl.communication2013.message.type.ClearedPathMessage;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 4:35 PM
 * Author: Mostafa Movahedi
 */
public class PoliceMessageHelper extends PlatoonMessageHelper {
    MrlPoliceForceWorld world;

    public PoliceMessageHelper(MrlPoliceForceWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
    }

    public void sendClearedRoadMessage(EntityID roadID) {
//        messageManager.addMessage(new ClearedRoadMessage(new ClearedRoad(roadID, world.getTime())));
    }

//    public void sendClearedPathMessage(EntityID pathID) {
//        messageManager.addMessage(new ClearedPathMessage(new ClearedPath(pathID, world.getTime())));
//    }

    public void processMessage(MessageEntity messageEntity) {

    }
}
