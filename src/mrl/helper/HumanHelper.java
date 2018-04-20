package mrl.helper;

import javolution.util.FastMap;
import mrl.helper.info.HumanInfo;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.routing.graph.Graph;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 9:58:41 AM
 */
public class HumanHelper implements IHelper {

    protected MrlWorld world;
    protected Map<EntityID, HumanInfo> humanInfoMap = new FastMap<EntityID, HumanInfo>();
    protected boolean amIStuck = false;
    protected EntityID previousPosition = null;
    protected int stayInSamePositionCounter;
    protected int previousStuckTime;
    protected int stuckTimeCounter;
    protected List<EntityID> stuckAgents = new ArrayList<EntityID>();
    protected List<EntityID> buriedAgents = new ArrayList<EntityID>();
    protected Integer repeatHealthyMessageCount;

    public HumanHelper(MrlWorld world) {
        this.world = world;
    }

    public void init() {
        for (StandardEntity entity : world.getPlatoonAgents()) {
            humanInfoMap.put(entity.getID(), new HumanInfo(false, false));
        }
    }

    public void setInfoMap(EntityID id) {
        humanInfoMap.put(id, new HumanInfo(false, false));
    }

    public void setLockedByBlockade(EntityID id, Boolean locked) {
        humanInfoMap.get(id).setLockedByBlockade(locked);

        if (!stuckAgents.contains(id)) {
            if (locked) {
                stuckAgents.add(id);

            }
        } else {
            if (!locked) {
                stuckAgents.remove(id);
            }
        }
    }

    public void setLeader(EntityID id, Boolean leader) {
        humanInfoMap.get(id).setLeader(leader);
    }

    public void update() {
        if (world.getSelf() instanceof MrlPlatoonAgent) {
//            checkIsLockedByBlockade();
            if (world.getTime() > world.getIgnoreCommandTime()) {
                updateSelfHealthyValue();
            }
        }
    }

//    public Boolean isLockedByBlockade(EntityID id) {
//        return humanInfoMap.get(id).isLockedByBlockade();
//    }

    public Boolean isLeader(EntityID id) {
        return humanInfoMap.get(id).isLeader();
    }

    public Boolean isBuried(EntityID id) {
        Human human = world.getEntity(id, Human.class);
        return human.isBuriednessDefined() && human.getBuriedness() > 0;

    }

    public static Boolean isBuried(Human human) {
        return human.isBuriednessDefined() && human.getBuriedness() > 0;
    }

//    public void checkIsLockedByBlockade() {
//        Human self = world.getSelfHuman();
//        if (self instanceof PoliceForce || self.getBuriedness() > 0) {
//            return;
//        }
//        StandardEntity position = self.getPosition(world);
//
//        if ((position instanceof Area) && isOnTheBlockade((Area) position)) {
//            this.amIStuck = true;
//            setLockedByBlockade(world.getSelf().getID(), true);
//        } else if ((position instanceof Area) && (isInPreviousArea(position.getID()))) {
//            setAmIStuck(true);
//        } else {
//            setAmIStuck(false);
//        }
//    }

    private boolean isInPreviousArea(EntityID areaId) {
        String lastCommand = world.getPlatoonAgent().getLastCommand();

        if (lastCommand == null) {
            return false;
        }
        if (lastCommand.equalsIgnoreCase("Move") || lastCommand.equalsIgnoreCase("Move To Point")) {
            if (areaId.equals(previousPosition)) {
                stayInSamePositionCounter++;
                if (stayInSamePositionCounter > 6) {
//                    world.printData(" i Am in isolated are.....");
                    return true;
                }
            } else {
                stayInSamePositionCounter = 0;
                previousPosition = areaId;
            }
        }
        return false;
    }


//    public void setAmIStuck(boolean amIStuck) {
//        if (!amIStuck || (previousStuckTime + 1 == world.getTime() && stuckTimeCounter > 2)) {
//            stuckTimeCounter = 0;
//            if (!amIStuck) {
//                previousStuckTime = 0;
//            }
//            this.amIStuck = amIStuck;
//            setLockedByBlockade(world.getSelf().getID(), amIStuck);
////            if (amIStuck) {
////                world.printData("I AM STUCK");
////            }
//        } else {
//            previousStuckTime = world.getTime();
//            stuckTimeCounter++;
//        }
//    }

    private boolean buildingIsIsolated(Building position) {
        Graph graph = world.getPlatoonAgent().getPathPlanner().getGraph();

        for (Entrance entrance : world.getMrlBuilding(position.getID()).getEntrances()) {
            if (graph.getNodeBetweenAreas(position.getID(), entrance.getID(), null).isPassable()) {
                return false;
            }
        }
        return true;
    }

//    public boolean amIStuck() {
//        return amIStuck;
//    }

    public List<Human> getBlockedAgents() {
        List<Human> blockedAgents = new ArrayList<Human>();
        for (EntityID id : stuckAgents) {
            blockedAgents.add((Human) world.getEntity(id));
        }
        return blockedAgents;
    }

    public Boolean isBlocked(EntityID id) {
        return stuckAgents.contains(id);

    }

    public List<EntityID> getBuriedAgents() {
        return buriedAgents;
    }

    public EntityID getNearestRefugeID(EntityID id) {
        return humanInfoMap.get(id).getNearestRefugeID();
    }

    public void setNearestRefuge(EntityID id, EntityID nearestRefugeId) {
        humanInfoMap.get(id).setNearestRefuge(nearestRefugeId);
    }

    public int getTimeToRefuge(EntityID id) {
        return humanInfoMap.get(id).getTimeToRefuge();
    }

    public void setTimeToRefuge(EntityID id, int timeToRefuge) {
        humanInfoMap.get(id).setTimeToRefuge(timeToRefuge);
    }

    public int getPreviousBuriedness(EntityID id) {
        return humanInfoMap.get(id).getPreviousBuriedness();
    }

    public void setPreviousBuriedness(EntityID id, int previousBuriedness) {
        humanInfoMap.get(id).setPreviousBuriedness(previousBuriedness);
    }

    public int getCurrentBuriedness(EntityID id) {
        return humanInfoMap.get(id).getCurrentBuriedness();
    }

    public void setCurrentBuriedness(EntityID id, int currentBuriedness) {
        humanInfoMap.get(id).setCurrentBuriedness(currentBuriedness);
    }

    public int getNumberOfATsRescuing(EntityID id) {
        return humanInfoMap.get(id).getNumberOfATsRescuing();
    }

    public void setNumberOfATsRescuing(EntityID id, int numberOfATsRescuing) {
        humanInfoMap.get(id).setNumberOfATsRescuing(numberOfATsRescuing);
    }

    public int getFirstHP(EntityID id) {
        return humanInfoMap.get(id).getFirstHP();
    }

    public void setFirstHP(EntityID id, int firstHP) {
        humanInfoMap.get(id).setFirstHP(firstHP);
    }

    public int getCurrentHP(EntityID id) {
        return humanInfoMap.get(id).getCurrentHP();
    }

    public void setCurrentHP(EntityID id, int currentHP) {
        humanInfoMap.get(id).setCurrentHP(currentHP);
    }

    public int getFirstDamage(EntityID id) {
        return humanInfoMap.get(id).getFirstDamge();
    }

    public void setFirstDamage(EntityID id, int damage) {
        humanInfoMap.get(id).setFirstDamage(damage);
    }

    public int getCurrentDamage(EntityID id) {
        return humanInfoMap.get(id).getCurrentDamage();
    }

    public void setCurrentDamage(EntityID id, int currentDamage) {
        humanInfoMap.get(id).setCurrentDamage(currentDamage);
    }

    public int getPreviousHP(EntityID id) {
        return humanInfoMap.get(id).getPreviousHP();
    }

    public void setPreviousHP(EntityID id, int previousHP) {
        humanInfoMap.get(id).setPreviousHP(previousHP);
    }

    public int getPreviousDamage(EntityID id) {
        return humanInfoMap.get(id).getPreviousDamage();
    }

    public void setPreviousDamage(EntityID id, int previousDamage) {
        humanInfoMap.get(id).setPreviousDamage(previousDamage);
    }

    public int getEmergencyLevel(EntityID id) {
        return humanInfoMap.get(id).getEmergencyLevel();
    }

    public void setEmergencyLevel(EntityID id, int emergencyLevel) {
        humanInfoMap.get(id).setEmergencyLevel(emergencyLevel);
    }

    public double getBenefit(EntityID id) {
        return humanInfoMap.get(id).getBenefit();
    }

    public void setBenefit(EntityID id, double benefit) {
        humanInfoMap.get(id).setBenefit(benefit);
    }

    public double getCAOP(EntityID id) {
        return humanInfoMap.get(id).getCAOP();
    }

    public void setCAOP(EntityID id, double caop) {
        humanInfoMap.get(id).setCAOP(caop);
    }

    public int getDeltaDamage(EntityID id) {
        return humanInfoMap.get(id).getDeltaDamage();
    }

    public void setDeltaDamage(EntityID id, int deltaDamage) {
        humanInfoMap.get(id).setDeltaDamage(deltaDamage);
    }

    public int getLastTimeHPChanged(EntityID id) {
        return humanInfoMap.get(id).getLastTimeHPChanged();
    }

    public void setLastTimeHPChanged(EntityID id, int lastTimeHPChanged) {
        humanInfoMap.get(id).setLastTimeHPChanged(lastTimeHPChanged);
    }

    public int getLastTimeDamageChanged(EntityID id) {
        return humanInfoMap.get(id).getLastTimeDamageChanged();
    }

    public void setLastTimeDamageChanged(EntityID id, int lastTimeDamageChanged) {
        humanInfoMap.get(id).setLastTimeDamageChanged(lastTimeDamageChanged);
    }


    public int getDeltaHP(EntityID id) {
        return humanInfoMap.get(id).getDeltaHP();
    }

    public void setDeltaHP(EntityID id, int deltaHP) {
        humanInfoMap.get(id).setDeltaHP(deltaHP);
    }

    public int getTimeToArrive(EntityID id) {
        return humanInfoMap.get(id).getTimeToArrive();
    }

    public void setTimeToArrive(EntityID id, int timeToRArrive) {
        humanInfoMap.get(id).setTimeToArrive(timeToRArrive);
    }

    public int getTimeToDeath(EntityID id) {
        return humanInfoMap.get(id).getTimeToDeath();
    }

    public void setTimeToDeath(EntityID id, int timeToDeath) {
        humanInfoMap.get(id).setTimeToDeath(timeToDeath);
    }

    public boolean isFromSense(EntityID id) {
        return humanInfoMap.get(id).isFromSense();
    }

    public void setFromSense(EntityID id, boolean fromSense) {
        humanInfoMap.get(id).setFromSense(fromSense);
    }

    public int getFirstBuriedness(EntityID id) {
        return humanInfoMap.get(id).getFirstBuriedness();
    }

    public void setFirstBuriedness(EntityID id, int firstBuriedness) {
        humanInfoMap.get(id).setFirstBuriedness(firstBuriedness);
    }


    public int getDistanceToFire(EntityID id) {
        return humanInfoMap.get(id).getDistanceToFire();
    }

    public void setDistanceToFire(EntityID id, int distanceToFire) {
        humanInfoMap.get(id).setDistanceToFire(distanceToFire);
    }


    public void updateSelfHealthyValue() {
        Human human = world.getSelfHuman();
        if (repeatHealthyMessageCount == null && human.getBuriedness() == 0) {
            repeatHealthyMessageCount = 3;
        } else if (repeatHealthyMessageCount != null && human.getBuriedness() != 0) {
            repeatHealthyMessageCount = null;
        }
    }

    public boolean isSelfHealthyRepeat() {
        if (repeatHealthyMessageCount != null) {
            repeatHealthyMessageCount--;
            if (repeatHealthyMessageCount > 0) {
                return true;
            }
        }
        return false;
    }

    public void setAgentSate(EntityID id, State state) {
        humanInfoMap.get(id).setAgentState(state);
    }

    public State getAgentState(EntityID id) {
        return humanInfoMap.get(id).getAgentState();
    }

    /**
     * Checks if the agent's state specified by {@code id} is NOT DEAD AND NOT BURIED.
     *
     * @param id ID of the agent to find
     * @return true if the agent's state is not DEAD and not BURIED, false otherwise. If id is invalid, returns false.
     */
    public boolean isAgentStateHealthy(EntityID id) {
        HumanInfo info = humanInfoMap.get(id);
        return info != null
                && !info.getAgentState().equals(State.RESTING)
                && !info.getAgentState().equals(State.BURIED)
                && !info.getAgentState().equals(State.DEAD)
                && !info.getAgentState().equals(State.CRASH)
                ;
    }
}
