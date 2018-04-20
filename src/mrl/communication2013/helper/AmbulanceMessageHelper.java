package mrl.communication2013.helper;

import mrl.ambulance.MrlAmbulanceTeamWorld;
import mrl.common.MRLConstants;
import mrl.communication2013.entities.Loader;
import mrl.communication2013.entities.MessageEntity;
import mrl.communication2013.entities.RescuedCivilian;
import mrl.communication2013.entities.TransportingCivilian;
import mrl.communication2013.message.MessageManager;
import mrl.communication2013.message.type.LoaderMessage;
import mrl.communication2013.message.type.RescuedCivilianMessage;
import mrl.communication2013.message.type.TransportingCivilianMessage;
import mrl.helper.PropertyHelper;
import mrl.platoon.MrlPlatoonAgent;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 4:37 PM
 * Author: Mostafa Movahedi
 */
public class AmbulanceMessageHelper extends PlatoonMessageHelper {
    MrlAmbulanceTeamWorld world;
    private int lastClearTime = -1;

    public AmbulanceMessageHelper(MrlAmbulanceTeamWorld world, MrlPlatoonAgent platoonAgent, MessageManager messageManager) {
        super(world, platoonAgent, messageManager);
        this.world = world;
    }

    public boolean processMessage(MessageEntity messageEntity) {
        switch (messageEntity.getMessageEntityType()) {
            case RescuedCivilian:
                processRescuedCivilianMessage((RescuedCivilian) messageEntity);
                break;
            case TransportingCivilian:
                processTransportingCivilianMessage((TransportingCivilian) messageEntity);
                break;
            case Loader:
                processLoaderMessage((Loader) messageEntity);
                break;
            default:
                break;
        }

        return false;
    }

    // Rescued Civilian
    public void sendRescuedCivilianMessage(EntityID shouldRescueCivilianId) {
        messageManager.addMessage(new RescuedCivilianMessage(new RescuedCivilian(shouldRescueCivilianId, world.getTime())));
    }

    private void processRescuedCivilianMessage(RescuedCivilian rescuedCivilian) {

        mrl.ambulance.structures.RescuedCivilian rc = new mrl.ambulance.structures.RescuedCivilian();
        rc.setAmbulanceID(rescuedCivilian.getSender());
        rc.setCivilianId(rescuedCivilian.getHumanID());

        world.getCurrentlyRescuedCivilians().add(rc);
        world.getRescuedCivilians().add(rc.getCivilianId());
        world.getTransportingCivilians().remove((Civilian) world.getEntity(rc.getCivilianId()));
    }

    // transportingCivilian
    public void sendTransportingCivilian(EntityID currentRescueCivilianId) {
        if (currentRescueCivilianId == null) {
            return;
        }
        messageManager.addMessage(new TransportingCivilianMessage(new TransportingCivilian(currentRescueCivilianId, world.getTime())));

        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println(" Transporting Civilian Sent------------- " + currentRescueCivilianId + " time:" + world.getTime());
        }
    }

    private void processTransportingCivilianMessage(TransportingCivilian transportingCivilian) {
        EntityID id = transportingCivilian.getHumanID();

        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println("meID:" + platoonAgent.getID() + " got Transproted civilian: " + id);
        }
        Civilian civ = (Civilian) world.getEntity(id);

        if (civ == null) {
            civ = new Civilian(id);
        }

        civ.setPosition(transportingCivilian.getSender());

        world.getCivilians().add(civ);

        world.getHelper(PropertyHelper.class).setPropertyTime(civ.getPositionProperty(), world.getTime() - 1);
        if (!(world.getTransportingCivilians().contains(civ)))
            world.getTransportingCivilians().add(civ);
    }

    // Loader Message
    public void sendLoaderMessage(EntityID civilianID) {
        messageManager.addMessage(new LoaderMessage(new Loader(civilianID, world.getTime())));
        world.getLoaders().add(new Pair<EntityID, EntityID>(world.getSelf().getID(), civilianID));
        if (MRLConstants.DEBUG_AMBULANCE_TEAM) {
            System.out.println(" Loader Message Sent------------- " + world.getSelf().getID() + " time:" + world.getTime());
        }
    }

    private void processLoaderMessage(Loader loader) {
        world.getLoaders().add(new Pair<EntityID, EntityID>(loader.getSender(), loader.getHumanID()));
    }

}
