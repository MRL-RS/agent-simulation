package mrl.firebrigade.extinguishBehaviour;

import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.targetSelection.FireBrigadeTarget;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static rescuecore2.misc.Handy.objectsToIDs;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 7:01 PM
 * Author: Mostafa Movahedi
 */
public class DirectionBasedExtinguishBehaviour implements IExtinguishBehaviour {
    @Override
    public void extinguish(MrlFireBrigadeWorld world, FireBrigadeTarget target) throws CommandException, TimeOutException {
        if (target == null) {
            return;
        }
//        lastTarget = target;
        int myDistanceToTarget = world.getDistance(world.getSelfHuman(), target.getMrlBuilding().getSelfBuilding());
        EntityID targetId = target.getMrlBuilding().getID();
//        moveToEntranceForCheck();
        // move to target if I can't see this.
        int maxExtinguishDistance = world.getMaxExtinguishDistance();
        MrlPlatoonAgent platoonAgent = world.getPlatoonAgent();
        if (myDistanceToTarget > maxExtinguishDistance) {
            StandardEntity locationToGo = chooseBestLocationToStandForExtinguishFire(world, target);

            MrlPersonalData.VIEWER_DATA.setStand(world.getSelf().getID(), locationToGo);

            if (!platoonAgent.move((Area) locationToGo, 0, false)) {
                //TODO @Mostafa&Pooya: review below codes
                //age jayee ke mikhast bere rahi behesh nabood miad in khate code
                Collection<StandardEntity> inRange = world.getObjectsInRange(locationToGo, maxExtinguishDistance / 3);
                int counter = 0;
                for (StandardEntity entity : inRange) {
                    if (entity instanceof Area && world.getDistance(target.getMrlBuilding().getID(), entity.getID()) < maxExtinguishDistance) {
                        MrlPersonalData.VIEWER_DATA.setStand(world.getSelf().getID(), entity);

                        counter++;
                        platoonAgent.move((Area) entity, 0, false);
                        if (counter > 3) {
                            break;
                        }
                    }
                }

            }
            //if agent unable to move to any possible location to extinguish target do this action
//            world.printData("I cant extinguish my main target.");
            lastTryToExtinguish(world, platoonAgent);


        } else {
            //go to another location if possibles
//            List<EntityID> forbiddenLocations = getForbiddenLocations(world, target);
//            if (!forbiddenLocations.isEmpty() && forbiddenLocations.contains(world.getSelfPosition().getID())) {
//                StandardEntity newLocation = chooseBestLocationToStandForExtinguishFire(world, target);
//                if (newLocation != null) {
//                    platoonAgent.move((Area) newLocation, 0, false);
//                }
//            }
            if (((FireBrigade) world.getSelfHuman()).getWater() != 0) {
                int waterPower = FireBrigadeUtilities.calculateWaterPower(world, target.getMrlBuilding());

                ((MrlFireBrigade) platoonAgent).getFireBrigadeMessageHelper().sendWaterMessage(targetId, waterPower);
                target.getMrlBuilding().increaseWaterQuantity(waterPower);

                platoonAgent.sendExtinguishAct(world.getTime(), targetId, waterPower);
            }

        }
        platoonAgent.isThinkTimeOver("extinguishTarget");
    }

    private void lastTryToExtinguish(MrlFireBrigadeWorld world, MrlPlatoonAgent platoonAgent) throws CommandException {
        Set<MrlBuilding> buildingsInMyExtinguishRange = FireBrigadeUtilities.getBuildingsInMyExtinguishRange(world);
        List<MrlBuilding> fieryBuildingsInMyExtinguishRange = new ArrayList<MrlBuilding>();
        for (MrlBuilding building : buildingsInMyExtinguishRange) {
            if (building.getSelfBuilding().isOnFire()) {
                fieryBuildingsInMyExtinguishRange.add(building);
            }
        }
        MrlBuilding tempTarget = Util.findNearest(fieryBuildingsInMyExtinguishRange, world.getSelfLocation());
        if (tempTarget != null) {
            int waterPower = FireBrigadeUtilities.calculateWaterPower(world, tempTarget);
            ((MrlFireBrigade) platoonAgent).getFireBrigadeMessageHelper().sendWaterMessage(tempTarget.getID(), waterPower);
            tempTarget.increaseWaterQuantity(waterPower);
            platoonAgent.sendExtinguishAct(world.getTime(), tempTarget.getID(), waterPower);
        }
    }

    private List<EntityID> getForbiddenLocations(MrlFireBrigadeWorld world, FireBrigadeTarget target) {
        List<EntityID> forbiddenLocations = new ArrayList<EntityID>();

        if (target.getCluster() != null) {
            forbiddenLocations.addAll(objectsToIDs(target.getCluster().getAllEntities()));
        }
        forbiddenLocations.addAll(world.getBurningBuildings());
        //if i am nth smallest FB, i should move over there
        //to force FBs to create a ring around fire
        int n = 3;
        if (world.isMapMedium()) n = 10;
        if (world.isMapHuge()) n = 15;
        for (FireBrigade next : world.getFireBrigadeList()) {
            if (world.getSelf().getID().getValue() < next.getID().getValue() && world.getDistance(world.getSelf().getID(), next.getID()) < world.getViewDistance() && --n <= 0) {
                MrlRoad roadOfNearFB = world.getMrlRoad(next.getPosition());
                MrlBuilding buildingOfNearFB = world.getMrlBuilding(next.getPosition());
                if (roadOfNearFB != null) {
                    forbiddenLocations.addAll(roadOfNearFB.getObservableAreas());
                }
                if (buildingOfNearFB != null) {
                    forbiddenLocations.addAll(buildingOfNearFB.getObservableAreas());
                }
            }
        }

        MrlPersonalData.VIEWER_DATA.setForbiddenLocations(world.getSelf().getID(), forbiddenLocations);
        return forbiddenLocations;
    }

    private StandardEntity chooseBestLocationToStandForExtinguishFire(MrlFireBrigadeWorld world, FireBrigadeTarget target) {
        double minDistance = Integer.MAX_VALUE;
        List<EntityID> forbiddenLocationIDs = getForbiddenLocations(world, target);
        List<StandardEntity> possibleAreas = new ArrayList<StandardEntity>();
        StandardEntity targetToExtinguish = null;
        double dis;
        List<EntityID> extinguishableFromAreas = target.getMrlBuilding().getExtinguishableFromAreas();
//        List<StandardEntity> bigBorder = world.getAreasIntersectWithShape(target.getCluster().getBigBorderPolygon());
        for (EntityID next : extinguishableFromAreas) {
            StandardEntity entity = world.getEntity(next);
//            if (bigBorder.contains(entity)) {
            possibleAreas.add(entity);
//            }
        }
        MrlPersonalData.VIEWER_DATA.setBestPlaceToStand(world.getSelf().getID(), world.getEntities(extinguishableFromAreas));

        List<StandardEntity> forbiddenLocations = world.getEntities(forbiddenLocationIDs);
        possibleAreas.removeAll(forbiddenLocations);
        if (possibleAreas.isEmpty()) {
            for (EntityID next : extinguishableFromAreas) {
                possibleAreas.add(world.getEntity(next));
            }
            /*Collection<Area> possibleLocations = new ArrayList<Area>();
            for (StandardEntity next : world.getAreas()) {
                possibleLocations.add((Area) next);
            }
            possibleLocations.removeAll(forbiddenLocations);
            return Util.nearestAreaTo(possibleLocations, world.getSelfLocation());*/
        }


        //fist search for a road to stand there
        for (StandardEntity entity : possibleAreas) {
            if (entity instanceof Road) {
                dis = world.getDistance(world.getSelfPosition(), entity);
                if (dis < minDistance) {
                    minDistance = dis;
                    targetToExtinguish = entity;
                }
            }
        }
        //if there is no road to stand, search for a no fiery building to go
        if (targetToExtinguish == null) {
            for (StandardEntity entity : possibleAreas) {
                if (entity instanceof Building) {
                    Building building = (Building) entity;
                    dis = world.getDistance(world.getSelfPosition(), entity);
                    if (dis < minDistance && (!building.isFierynessDefined() || (building.isFierynessDefined() && (building.getFieryness() >= 4 || building.getFieryness() <= 1)))) {
                        minDistance = dis;
                        targetToExtinguish = entity;
                    }
                }
            }
        }
        return targetToExtinguish;
    }
}
