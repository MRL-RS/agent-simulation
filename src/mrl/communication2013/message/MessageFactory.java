package mrl.communication2013.message;

import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.common.comparator.ConstantComparators;
import mrl.communication2013.entities.*;
import mrl.communication2013.message.type.*;
import mrl.helper.HumanHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 6:17 PM
 *
 * @Author: Mostafa Movahedi
 */
public class MessageFactory {
    public static final int maxDamageToReport = 256;
    private MessageManager messageManager;
    private Map<EntityID, Integer> buildingLastTemperature;

    public MessageFactory(MessageManager messageManager, MrlWorld world) {
        this.messageManager = messageManager;

        buildingLastTemperature = new HashMap<EntityID, Integer>();
    }

    public void createPlatoonMessages(MrlWorld world, MrlPlatoonAgent platoonAgent) {
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        // agent info
        messageManager.addMessage(new AgentInfoMessage(new AgentInfo(world.getSelfPosition() instanceof Road ? PositionTypes.Road : PositionTypes.Building, world.getSelfPosition().getID(), platoonAgent.getAgentState(), world.getTime())));
        // buried & CLBuried Agent
        int damage;
        Human human = world.getSelfHuman();
        if (human.getBuriedness() > 0) {
            damage = human.getDamage();
            if (damage < maxDamageToReport) {
                if (damage == 0) {
                    damage = 1;
                }

//                if (ambulanceUtilities.isAlivable(human)) {
                if (world.getSelfPosition() instanceof Building) {
                    messageManager.addMessage(new BuriedAgentMessage(new BuriedAgent(human.getPosition(), human.getHP(), human.getBuriedness(), damage, world.getTime())));
                    messageManager.addMessage(new BuriedAgentSayMessage(new BuriedAgentSay(human.getID(), human.getPosition(), human.getHP(), human.getBuriedness(), damage, world.getTime())));
                }
//                } else {
//                    messageManager.addMessage(new BuriedAgentMessage(new BuriedAgent(human.getPosition(), 0, human.getBuriedness(), damage, world.getTime())));
//                    messageManager.addMessage(new BuriedAgentSayMessage(new BuriedAgent(human.getID(),human.getPosition(), 0, human.getBuriedness(), damage, world.getTime())));
//                }
            }
        }

        // heard civilian
        Pair<Integer, Integer> location = world.getSelfLocation();
        for (EntityID id : world.getHeardCivilians()) {
            messageManager.addMessage(new HeardCivilianMessage(new HeardCivilian(id, location.first(), location.second(), world.getTime())));
        }
        world.getHeardCivilians().clear();

        //empty building
        for (EntityID id : world.getThisCycleEmptyBuildings()) {
            MrlBuilding mrlBuilding = world.getMrlBuilding(id);
            if (mrlBuilding.getCivilians().isEmpty()) {
                EmptyBuilding emptyBuilding = new EmptyBuilding(id, world.getTime());
                messageManager.addMessage(new EmptyBuildingMessage(emptyBuilding));
            }
        }
        world.getThisCycleEmptyBuildings().clear();

        SortedSet<EntityID> agentsInSamePosition = new TreeSet<EntityID>(ConstantComparators.EntityID_COMPARATOR);
        StandardEntity position = world.getSelfPosition();
        boolean iCanSendMessage = true;

        for (Human agent : world.getAgentsSeen()) {
            if (position.getID().equals(agent.getPosition())) {
                agentsInSamePosition.add(agent.getID());
            }
        }
        //do not send this info, because it is an agent and if it was buried, it could itself send buriedMessage
        if (agentsInSamePosition.size() > 1) {
            if (!agentsInSamePosition.first().equals(world.getSelf().getID())) {
                iCanSendMessage = false;
            }

        }
        if (iCanSendMessage) {
            // burning building
            for (Building building : world.getBuildingSeen()) {
                EntityID id = building.getID();
                Integer lastTemperature = buildingLastTemperature.get(id);
                if (building.isTemperatureDefined()) {
                    if (lastTemperature == null || lastTemperature != building.getTemperature()){
                        buildingLastTemperature.put(id, building.getTemperature());
                        if (building.isFierynessDefined()) {
                            if (building.getFieryness() > 3 /*&& building.getFieryness() < 8*/) {
                                messageManager.addMessage(new ExtinguishedBuildingMessage(new ExtinguishedBuilding(id, building.getFieryness(), world.getTime())));
                            } else if (building.getFieryness() > 0 /*&& building.getFieryness() != 8*/) {
                                messageManager.addMessage(new BurningBuildingMessage(new BurningBuilding(id, building.getFieryness(), building.getTemperature(), world.getTime())));
                            }
                        }
                    }
                }
            }

            // civilian
            for (Civilian civilian : world.getCiviliansSeen()) {
                if (civilian.isDamageDefined() && civilian.getDamage() < maxDamageToReport && civilian.isHPDefined() && civilian.getHP() > maxDamageToReport) {
                    if (!(world.getEntity(civilian.getPosition()) instanceof Refuge)
                            && !(world.getEntity(civilian.getPosition()) instanceof AmbulanceTeam)
                            && civilian.getDamage() > 0) {
                        EntityID civilianPos = civilian.getPosition();
                        EntityID id = civilian.getID();
                        if (world.getEntity(civilianPos) instanceof Road) {
                            civilianPos = findNearestBuilding(world, civilianPos);
                            if (civilianPos != null) {
                                messageManager.addMessage(new CivilianSeenMessage(new CivilianSeen(id, civilian.getBuriedness(), civilian.getDamage(), civilian.getHP(), civilianPos, humanHelper.getTimeToRefuge(id), world.getTime())));
                            }
                        } else {
                            messageManager.addMessage(new CivilianSeenMessage(new CivilianSeen(id, civilian.getBuriedness(), civilian.getDamage(), civilian.getHP(), civilianPos, humanHelper.getTimeToRefuge(id), world.getTime())));
                        }
                    }
                }
            }

            //aftershock
            if (world.getTime() > world.getIgnoreCommandTime() && world.getLastAfterShockTime() == world.getTime()) {
                messageManager.addMessage(new AftershockMessage(new AftershockEntity(world.getTime(), world.getAftershockCount())));
            }
        }
    }

    private EntityID findNearestBuilding(MrlWorld world, EntityID roadId) {
        MrlRoad road = world.getMrlRoad(roadId);
        for (EntityID id : road.getVisibleFrom()) {
            StandardEntity entity = world.getEntity(id);
            if (entity instanceof Building) {
                return id;
            }
        }
        for (EntityID neighbourId : road.getParent().getNeighbours()) {
            StandardEntity entity = world.getEntity(neighbourId);
            if (entity instanceof Building) {
                return neighbourId;
            } else if (entity instanceof Road) {
                MrlRoad neighbour = world.getMrlRoad(neighbourId);
                if (neighbour != null) {
                    for (EntityID id : neighbour.getVisibleFrom()) {
                        StandardEntity entity2 = world.getEntity(id);
                        if (entity2 instanceof Building) {
                            return id;
                        }
                    }
                }
            }
        }
//        for (Path path : road.getPaths()) {
//            if (path.getBuildings().size() > 0) {
//                return path.getBuildings().get(0).getID();
//            }
//        }
        return null;
    }

    public void createCenterMessages(MrlWorld world) {

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);

        for (EntityID id : world.getChanges()) {
            StandardEntity entity = world.getEntity(id);
            // burning building
            if (entity instanceof Building) {
                Building building = (Building) entity;
                if (building.isFierynessDefined()) {
                    if (building.getFieryness() > 3 && building.getFieryness() < 8) {
                        messageManager.addMessage(new ExtinguishedBuildingMessage(new ExtinguishedBuilding(id, building.getFieryness(), world.getTime())));
                    } else if (building.getFieryness() > 0 && building.getFieryness() != 8) {
                        messageManager.addMessage(new BurningBuildingMessage(new BurningBuilding(id, building.getFieryness(), building.getTemperature(), world.getTime())));
                    }
                }
            }
            // civilian
            else if (entity instanceof Civilian) {
                Civilian civilian = (Civilian) entity;
                if (civilian.getDamage() < maxDamageToReport) {
                    if (!(world.getEntity(civilian.getPosition()) instanceof Refuge)
                            && (world.getEntity(civilian.getPosition()) instanceof Building)
                            && civilian.getDamage() != 0) {
                        messageManager.addMessage(new CivilianSeenMessage(new CivilianSeen(id, civilian.getBuriedness(), civilian.getDamage(), civilian.getHP(), civilian.getPosition(), humanHelper.getTimeToRefuge(id), world.getTime())));
                    }
                }
            }
        }
    }
}
