package mrl.communication2013.helper;

import javolution.util.FastMap;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.entities.Water;
import mrl.communication2013.entities.WaterTypes;
import mrl.communication2013.message.MessageManager;
import mrl.communication2013.message.type.WaterMessage;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.platoon.MrlPlatoonAgent;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:43 PM
 *
 * @Author: Mostafa Movahedi
 */

public class FireBrigadeMessageHelper extends PlatoonMessageHelper {
    MrlFireBrigadeWorld world;
    Map<EntityID, EntityID> letsGo;

    public FireBrigadeMessageHelper(MrlFireBrigadeWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
        letsGo = new FastMap<EntityID, EntityID>();
    }

    public void processMessage(MessageEntity messageEntity) {
        switch (messageEntity.getMessageEntityType()) {
            case WaterMessage:
                processWaterMessage((Water) messageEntity);
                break;
            default:
                break;
        }

    }

    public void sendWaterMessage(EntityID id, int water) {
        WaterTypes waterType = world.getMaxPower() == water ? WaterTypes.MaxPower : WaterTypes.Partial;
        messageManager.addMessage(new WaterMessage(new Water(id, waterType, world.getTime())));
    }

    private void processWaterMessage(Water water) {
        int waterQuantity = water.getWaterType().equals(WaterTypes.MaxPower) ? world.getMaxPower() : world.getMaxPower() >> 1;
        world.getMrlBuilding(water.getBuildingID()).increaseWaterQuantity(waterQuantity);
    }
}
