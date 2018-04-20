package mrl.police.strategies;

import javolution.util.FastMap;
import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.partitioning.Partition;
import mrl.police.moa.Importance;
import mrl.police.moa.Target;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.Map;

import static mrl.police.moa.Importance.BLOCKED_AMBULANCE_TEAM;
import static mrl.police.moa.Importance.NEIGHBOUR_PARTITION_CENTER;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/16/12
 *         Time: 1:21 PM
 */
public abstract class DefaultTargetManager implements ITargetManager {

    private int FIRE_COEFFICIENT=1000;

    public DefaultTargetManager(MrlWorld world) {
        this.world = world;
        this.foundTargets = new FastMap<EntityID, Target>();
        this.doneTargets = new FastMap<StandardEntity, Integer>();

    }

    protected MrlWorld world;
    // map of a done task and its done time
    protected Map<StandardEntity, Integer> doneTargets;

    protected Map<EntityID, Target> foundTargets;

    /**
     * Takes an entity and gets its importance based on predefined importance values
     *
     * @param entity the entity to get its importance
     * @return the importance of selected entity
     */
    protected Pair<Importance, Integer> getImportance(StandardEntity entity, Importance importanceType,boolean fireExists) {
        Building building;
        int importanceValue = 0;
        int inViewDistanceSum = 1000 - 10 * world.getTime();

        if (inViewDistanceSum < 0) {
            inViewDistanceSum = 0;
        }

        StandardEntity pos;
        Human human = null;

        if (entity instanceof Human) {
            human = (Human) entity;
            pos = human.getPosition(world);
        } else {
            pos = entity;
        }

        if (world.getChanges().contains(entity.getID()) && Util.distance(world.getSelfLocation(), pos.getLocation(world)) <= world.getViewDistance()) {
            importanceValue += inViewDistanceSum;
        }

        if (importanceType != null && (importanceType.equals(Importance.PARTITION_CENTER) || importanceType.equals(Importance.NEIGHBOUR_PARTITION_CENTER))) {
//            importanceType = Importance.PARTITION_CENTER;
            importanceValue +=importanceType.getImportance();
            if(fireExists){
                importanceValue += FIRE_COEFFICIENT;
            }
            return new Pair<>(importanceType, importanceValue);
        }

        if (!(entity instanceof Building) && entity instanceof Civilian && pos instanceof Building) {
            Human h = (Human) entity;
            if (h.isDamageDefined() && h.getDamage() == 0) {
                importanceType = Importance.BUILDING_WITH_HEALTHY_HUMAN;
                importanceValue += importanceType.getImportance();
            } else if (h.isDamageDefined() && h.getDamage() != 0) {
                importanceType = Importance.BUILDING_WITH_DAMAGED_CIVILIAN;
                importanceValue += importanceType.getImportance();
            } else {// undefined properties
                importanceType = Importance.DEFAULT;
                importanceValue = importanceType.getImportance();
            }
            return new Pair<Importance, Integer>(importanceType, importanceValue);
        }

        //checking Refuge Building
        if (entity instanceof Refuge) {
            importanceType = Importance.REFUGE_ENTRANCE;
            importanceValue += importanceType.getImportance();
            if(fireExists){
                importanceValue += FIRE_COEFFICIENT;
            }
            return new Pair<Importance, Integer>(importanceType, importanceValue);

            //checking Fiery Building
        } else if (entity instanceof Building) {
            building = (Building) entity;

            if (!building.isFierynessDefined() || building.getFieryness() == 0) {
                int i = 0;
                for (Human h : world.getMrlBuilding(entity.getID()).getCivilians()) {
                    if (h.isBuriednessDefined() && h.getBuriedness() == 0) {
                        i++;
                    }
                }
                importanceType = Importance.BUILDING_WITH_HEALTHY_HUMAN;
                importanceValue += importanceType.getImportance() * i;
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else if (building.isFierynessDefined()) {
                if (building.getFieryness() == 1) {
                    importanceType = Importance.FIERY_BUILDING_1;
                    importanceValue += importanceType.getImportance();
                    return new Pair<Importance, Integer>(importanceType, importanceValue);
                } else if (building.getFieryness() == 2) {
                    importanceType = Importance.FIERY_BUILDING_2;
                    importanceValue += importanceType.getImportance();
                    return new Pair<Importance, Integer>(importanceType, importanceValue);
                } else if (building.getFieryness() == 3 || (building.isTemperatureDefined() && building.getTemperature() > 0)) {
                    importanceType = Importance.FIERY_BUILDING_3;
                    importanceValue += importanceType.getImportance();
                    return new Pair<Importance, Integer>(importanceType, importanceValue);
                }
            }

        } else if (entity instanceof Road) {
            importanceType = Importance.DEFAULT;
            importanceValue = importanceType.getImportance();
            return new Pair<Importance, Integer>(importanceType, importanceValue);
        }

        // checking Fire Brigade
        if (entity instanceof FireBrigade) {
            if (world.isBuried(human)) {
                importanceType = Importance.BURIED_FIRE_BRIGADE;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.BLOCKED_FIRE_BRIGADE;
                importanceValue += importanceType.getImportance();
                if(fireExists){
                    importanceValue += FIRE_COEFFICIENT;
                }
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            }

            //checking Police Force
        } else if (entity instanceof PoliceForce) {
            importanceType = Importance.BURIED_POLICE_FORCE;
            importanceValue += importanceType.getImportance();
            return new Pair<Importance, Integer>(importanceType, importanceValue);


            //checking Ambulance Team
        } else if (entity instanceof AmbulanceTeam) {
            if (world.isBuried(human)) {
                importanceType = Importance.BURIED_AMBULANCE_TEAM;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = BLOCKED_AMBULANCE_TEAM;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            }

            //checking Civilian
        } else if (entity instanceof Civilian) {
            if (world.isBuried(human)) {
                importanceType = Importance.BUILDING_WITH_DAMAGED_CIVILIAN;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            } else {
                importanceType = Importance.DEFAULT;
                importanceValue += importanceType.getImportance();
                return new Pair<Importance, Integer>(importanceType, importanceValue);

            }

        }

        importanceType = Importance.DEFAULT;
        return new Pair<Importance, Integer>(importanceType, importanceType.getImportance());

    }

    protected boolean isFireExists(){
        return  !world.getBurningBuildings().isEmpty();
    }

    @Override
    /**
     * finds and gets targets which needed to be open(unAssigned ones)
     *
     * @param partition the partition to search targets in it
     * @return a map of each target entityID and its target object
     */
    public Map<EntityID, Target> getTargets(Partition partition) {

        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        Map<EntityID, Target> targets = new FastMap<EntityID, Target>();
        Human human;

        boolean fireExists = isFireExists();

        //get buried victims (civilians and platoon agents)

        // get buried platoon targets
        for (EntityID id : world.getBuriedAgents()) {
            putIntoTargets(partition, targets, world.getEntity(id), true,fireExists);
//            System.out.println(world.getTime() + " " + world.getSelf().getID() + " Buried Agent: " + id);
        }

        // get blocked platoon targets
        for (Human h : humanHelper.getBlockedAgents()) {
            if (!h.getID().equals(world.getSelfHuman().getID()) && h.getPosition() != null) {
                putIntoTargets(partition, targets, h, false,fireExists);
            }
//            System.out.println(world.getTime() + " " + world.getSelf().getID() + " Blocked Agent: " + human.getID());
        }

        // get blocked in a way platoon targets
        for (StandardEntity platoonEntity : world.getPlatoonAgents()) {
            human = (Human) platoonEntity;
            if ((human instanceof PoliceForce) || !human.isPositionDefined()) {
                continue;
            }
            if (Util.isOnBlockade(world, human) || Util.isNearBlockade(world, human)) {
                putIntoTargets(partition, targets, human, false,fireExists);
            }

        }


        // get Fiery Building Targets
        for (EntityID buildingID : world.getBurningBuildings()) {
            putIntoTargets(partition, targets, world.getEntity(buildingID), false,fireExists);
//            System.out.println(world.getTime() + " " + world.getSelf().getID() + " Fiery Building: " + buildingID);
        }


        //TODO: Targets To Go

        // get Blocked Civilians
        Civilian civilian;
        for (StandardEntity civilianEntity : world.getCivilians()) {
            civilian = (Civilian) civilianEntity;
            if (!civilian.isPositionDefined() || !civilian.isHPDefined() || civilian.getHP() < 0.1 * world.getSelfHuman().getStamina()) {
                continue;
            }
            StandardEntity positionEntity = civilian.getPosition(world);
            if (!(positionEntity instanceof Refuge) && (positionEntity instanceof Building /*|| Util.isNearBlockade(world, civilian)*/)) {
                putIntoTargets(partition, targets, positionEntity, false,fireExists);
            }
        }


        //get Refuge Entrances
        for (StandardEntity entity : world.getRefuges()) {
            putIntoTargets(partition, targets, entity, false,fireExists);
        }

//        //TODO: get Rendezvous Points
//        if (partition != null) {
//            for (EntityID rendezvous : partition.getRendezvous()) {
//                putIntoTargets(partition, targets, world.getEntity(rendezvous), false);
//            }
//        }
//

//        //TODO: get entities of Path to refuge
//        if (partition != null) {
//            for (EntityID entityID : partition.getRefugePathsToClearInPartition()) {
//                putIntoTargets(partition, targets, world.getEntity(entityID), false);
//            }
//        }


        if (partition != null) {
            //get my partition center
            putIntoTargets(partition, targets, partition.getCenterEntity(),Importance.PARTITION_CENTER,fireExists);


            //get neighbour partitions
            if(fireExists) {
//                for (EntityID partitionId : partition.getNeighbourRoutes().keySet()) {
//                    putIntoTargets(partition, targets, world.getEntity(partitionId), Importance.NEIGHBOUR_PARTITION_CENTER, fireExists);
//                }
                for (EntityID partitionId : partition.getNeighbours()) {
                    putIntoTargets(partition, targets, world.getEntity(partitionId), Importance.NEIGHBOUR_PARTITION_CENTER, fireExists);
                }
            }
        }

        return targets;

    }

    /**
     * This method is used for neighbour target partitions
     *
     * @param partition    Assigned partition of deciding agent
     * @param targets      Map of all targets
     * @param centerEntity Center entity of neighbour partition
     */
    private void putIntoTargets(Partition partition, Map<EntityID, Target> targets, StandardEntity centerEntity , Importance importanceType ,boolean fireExist) {
        Target target;
        Pair<Importance, Integer> importancePair;
        int distanceToIt;


        importancePair = getImportance(centerEntity, importanceType,fireExist);// new Pair<>(Importance.PARTITION_CENTER, Importance.PARTITION_CENTER.getImportance());

        //TODO @pooya DO jangooler thing


        target = foundTargets.get(centerEntity.getID());
        if (target == null) {
            target = new Target(centerEntity.getID(), centerEntity.getID(), importancePair.second(), importancePair.first());
            foundTargets.put(centerEntity.getID(), target);
        }
        distanceToIt = world.getMyDistanceTo(centerEntity);
        target.setDistanceToIt(distanceToIt);
        target.setImportance(importancePair.second());

        if (!doneTargets.containsKey(world.getEntity(target.getId())) && !targets.keySet().contains(target.getId())) {
            //in general conditions but not only in partitions
            if (partition.containsInEntities(target.getPositionID()) ||(fireExist && target.getImportanceType().equals(NEIGHBOUR_PARTITION_CENTER))) {
                targets.put(target.getId(), target);
            } else {
                target.setImportance(0);
            }
        }
    }


    @Override
    public Map<StandardEntity, Integer> getDoneTargets() {
        return doneTargets;
    }


    protected void putIntoTargets(Partition partition, Map<EntityID, Target> targets, StandardEntity entity, boolean isBuried, boolean fireExist) {
        Target target;
        Pair<Importance, Integer> importancePair;
        int distanceToIt;
        if (entity instanceof Human) {
            Human human = (Human) entity;
            importancePair = getImportance(entity, null,fireExist);

            target = foundTargets.get(human.getID());
            if (target != null && !target.getPositionID().equals(human.getPosition())) {
                target = null;
                foundTargets.remove(human.getID());
            }
            EntityID positionID;
            if (target == null) {
                if (human instanceof Civilian) {
                    positionID = human.getPosition();
                } else {
                    positionID = world.getAgentFirstPositionMap().get(human.getID());
                }
                target = new Target(human.getID(), positionID, importancePair.second(), importancePair.first());
                foundTargets.put(human.getID(), target);
            }
            distanceToIt = world.getMyDistanceTo(human.getPosition());
        } else {
            importancePair = getImportance(entity, null,fireExist);
            target = foundTargets.get(entity.getID());
            if (target == null) {
                target = new Target(entity.getID(), entity.getID(), importancePair.second(), importancePair.first());
                foundTargets.put(entity.getID(), target);
            }
            distanceToIt = world.getMyDistanceTo(entity);
        }
        target.setDistanceToIt(distanceToIt);
        target.setImportance(importancePair.second());

        if (!doneTargets.containsKey(world.getEntity(target.getId())) && !targets.keySet().contains(target.getId())) {
            //in general conditions but not only in partitions
            if (partition == null) {
                targets.put(target.getId(), target);
                //in partition-based conditions
            } else if (partition.containsInEntities(target.getPositionID()) || isTargetCloseToPartition(target, partition) || isNearMe(target)) {
                targets.put(target.getId(), target);
            }
        }
    }

    /**
     * Determines whether target is near enough to me to clear it or not.<br/>
     * <br/>
     * <b>WARNING:</b> This method ignores targets which are related to burning buildings.
     *
     * @param target the target to find out its closeness
     * @return true if target is clear range of the agent
     */
    protected boolean isNearMe(Target target) {

        boolean isNear = false;
        switch (target.getImportanceType()) {
            case REFUGE_ENTRANCE:
            case BLOCKED_AMBULANCE_TEAM:
            case BLOCKED_FIRE_BRIGADE:
            case BURIED_AMBULANCE_TEAM:
            case BURIED_FIRE_BRIGADE:
            case BURIED_POLICE_FORCE:
            case BUILDING_WITH_DAMAGED_CIVILIAN:
            case BUILDING_WITH_HEALTHY_HUMAN:
                isNear = checkCloseness(target);
                break;
        }

        return isNear;
    }

    private boolean checkCloseness(Target target) {

        StandardEntity pos = world.getEntity(target.getPositionID());


        if (pos instanceof Building) {
            MrlBuilding mrlBuilding = world.getMrlBuilding(pos.getID());
            int minDist = Integer.MAX_VALUE;
            int distance;
            Road nearestRoad = null;
            if (mrlBuilding.getEntrances() != null) {
                for (Entrance entrance : mrlBuilding.getEntrances()) {
                    distance = Util.distance(world.getSelfLocation(), entrance.getNeighbour().getLocation(world));
                    if (distance < minDist) {
                        minDist = distance;
                        nearestRoad = entrance.getNeighbour();
                    }
                }
            }

            if (nearestRoad != null) {
                return Util.distance(world.getSelfLocation(), nearestRoad.getLocation(world)) <= world.getClearDistance();
            } else {
                return false;
            }

        } else {
            Pair<Integer, Integer> targetLocation = world.getEntity(target.getId()).getLocation(world);

            return world.getChanges().contains(target.getId()) && Util.distance(world.getSelfLocation(), targetLocation)
                    <= world.getClearDistance();
        }
    }

    /**
     * This method computes whether the specified target is close enough(in clear range) to the specified partition or not
     *
     * @param target    The target to determine its closeness to the {@code partition}
     * @param partition The partition to determine the closeness of the {@code target} to its boundaries
     * @return True if {@code target} is close enough to the [@code partition]
     */
    protected boolean isTargetCloseToPartition(Target target, Partition partition) {

        boolean isClose = false;
        Pair<Integer, Integer> location = world.getEntity(target.getPositionID()).getLocation(world);
        Point2D point2D = new Point2D(location.first(), location.second());
        double distanceToPartitionPolygon = Util.distance(partition.getPolygon(), point2D);
        if (partition.getPolygon().contains(location.first(), location.second()) || distanceToPartitionPolygon < world.getClearDistance()) {
            isClose = true;

//            System.err.println("time:" + world.getTime() + " This target is close to my partition.   me" +
//                    world.getSelf().getID() + " target:" + target.getId());
        }

        return isClose;
    }


}
