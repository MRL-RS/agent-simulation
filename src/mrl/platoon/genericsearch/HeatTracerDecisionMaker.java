package mrl.platoon.genericsearch;

import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Mahdi
 */
public class HeatTracerDecisionMaker extends SearchDecisionMaker {
    private List<EntityID> shouldCheckBuildings;
    private Set<EntityID> checkedBuildings;
    private Set<EntityID> burntBuildings;
    private Set<EntityID> warmBuildings;
    private Set<EntityID> unvisitableBuildings;
    private int resetTime = 10;
    private int range;
    private MrlBuilding targetBuilding;
    private boolean filterAroundFireWarmBuildings = true;
    private Map<Integer, Set<EntityID>> aroundFireWarmBuildings;
    private int lastUpdateTime;

    public HeatTracerDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    public void initialize() {
        shouldCheckBuildings = new ArrayList<EntityID>();
        warmBuildings = new HashSet<EntityID>();
        unvisitableBuildings = new HashSet<EntityID>();
        checkedBuildings = new HashSet<EntityID>();
        burntBuildings = new HashSet<EntityID>();
        aroundFireWarmBuildings = new HashMap<Integer, Set<EntityID>>();
        if (world.isMapHuge()) {
            range = 50000;
            resetTime = 20;
        } else if (world.isMapMedium()) {
            range = 40000;
            resetTime = 15;
        } else {
            range = 30000;
            resetTime = 10;
        }
        update();
    }

    @Override
    public void update() {
        shouldCheckBuildings.clear();


        //update warm and burning buildings
        ////remove all old sensed warm buildings
        Set<EntityID> toRemove = new HashSet<EntityID>();
        for (EntityID warmID : warmBuildings) {
            MrlBuilding mrlBuilding = world.getMrlBuilding(warmID);
            if (world.getTime() - mrlBuilding.getSensedTime() > resetTime) {
                toRemove.add(warmID);
            }
        }
        warmBuildings.removeAll(toRemove);
        toRemove.clear();

        ////add warm buildings
        if (lastUpdateTime < world.getTime()) {
            updateWarmBuildings();
        }

        MrlBuilding warmestBuilding = null;
        int maxTemp = 0;
        for (EntityID id : warmBuildings) {
            MrlBuilding mrlBuilding = world.getMrlBuilding(id);
            int temp = mrlBuilding.getSelfBuilding().getTemperature();
            if (temp > maxTemp) {
                maxTemp = temp;
                warmestBuilding = mrlBuilding;
            }
        }
        if (targetBuilding == null ||
                world.getTime() - targetBuilding.getSensedTime() > resetTime ||
                warmestBuilding != null && warmestBuilding.getSelfBuilding().getTemperature() > targetBuilding.getSelfBuilding().getTemperature()) {
            targetBuilding = warmestBuilding;
        }

        //update sensed buildings
        toRemove.clear();
        for (EntityID id : checkedBuildings) {
            MrlBuilding building = world.getMrlBuilding(id);
            if (world.getTime() - building.getSensedTime() > resetTime) {
                toRemove.add(id);
            }
        }
        checkedBuildings.removeAll(toRemove);

        //update should check buildings
        shouldCheckBuildings.clear();
        if (targetBuilding != null) {
            //todo buildings in range list should be replaced with buildings which affects in temperature
            Collection<StandardEntity> objectsInRange = world.getObjectsInRange(targetBuilding.getSelfBuilding(), range);
            for (StandardEntity entity : objectsInRange) {
                if (entity instanceof Building) {
                    MrlBuilding mrlBuilding = world.getMrlBuilding(entity.getID());
                    if (world.getTime() - mrlBuilding.getSensedTime() > resetTime) {
                        shouldCheckBuildings.add(entity.getID());
                    }
                }
            }
        }


//            toRemove.clear();
//            for (Map.Entry<Integer, Set<EntityID>> timeIDs : aroundFireWarmBuildings.entrySet()) {
//                if (world.getTime() - timeIDs.getKey() < resetTime) {
//                    toRemove.addAll(timeIDs.getValue());
//                }
//            }
//            world.printData("There is " + toRemove.size() + " Buildings around fire.");
//            shouldCheckBuildings.removeAll(toRemove);
//        }

        shouldCheckBuildings.removeAll(warmBuildings);//it means we sensed these and know their temperature up to 10 cycles ago
        shouldCheckBuildings.removeAll(burntBuildings);//remove all buildings with fieriness 8
        shouldCheckBuildings.removeAll(checkedBuildings);//remove buildings checked up to 10 cycles ago
        shouldCheckBuildings.removeAll(unvisitableBuildings);//remove all buildings which is not visitable
    }

    public void updateWarmBuildings() {
//        Set<EntityID> entityIDs = aroundFireWarmBuildings.get(world.getTime());
//        if (entityIDs == null) {
//            entityIDs = new HashSet<EntityID>();
//        }
        lastUpdateTime = world.getTime();
        for (Building building : world.getBuildingSeen()) {
            if (burntBuildings.contains(building.getID())) {
                continue;
            }

            if (building.isTemperatureDefined() && building.isFierynessDefined()) {
                int fieriness = building.getFieryness();
                if (fieriness == 8) {
                    burntBuildings.add(building.getID());
                } else if (fieriness == 1 || fieriness == 2 || fieriness == 3) {
                    //this building is burning... so no need to search around it.
//                    if (filterAroundFireWarmBuildings) {
//                        for (StandardEntity entity : world.getObjectsInRange(building, range)) {
//                            if (entity instanceof Building) {
//                                MrlBuilding b = world.getMrlBuilding(entity.getID());
//                                if (b.getSelfBuilding().isFierynessDefined() && world.getTime() - b.getSensedTime() > resetTime) {
//                                    int f = b.getSelfBuilding().getFieryness();
//                                    if (f != 1 && f != 2 && f != 3) {
//                                        entityIDs.add(b.getID());
//                                    }
//                                }
//                            }
//                        }
//                    }
                } else {
                    if (building.getTemperature() > 0) {

                        warmBuildings.add(building.getID());
                    } else {
                        warmBuildings.remove(building.getID());
                    }
                }
            }
        }

//        aroundFireWarmBuildings.put(world.getTime(), entityIDs);
        List<EntityID> toRemove = new ArrayList<EntityID>();
        if (filterAroundFireWarmBuildings) {
            toRemove.clear();
            for (EntityID id : warmBuildings) {
                for (StandardEntity entity : world.getObjectsInRange(id, range)) {
                    if (entity instanceof Building) {
                        MrlBuilding mrlBuilding = world.getMrlBuilding(entity.getID());
                        if (mrlBuilding.getSelfBuilding().isFierynessDefined()) {
                            int fieriness = mrlBuilding.getSelfBuilding().getFieryness();
                            if (mrlBuilding.getSelfBuilding().isFierynessDefined() &&
                                    world.getTime() - mrlBuilding.getSensedTime() < resetTime &&
                                    fieriness != 4 && fieriness >= 1 && fieriness <= 7) {
                                toRemove.add(id);
                            }
                        }
                    }
                }
            }
        }
//        if (toRemove.size() > 0) {
//            world.printData("There is " + toRemove.size() + " Buildings around fire.");
//        }
        warmBuildings.removeAll(toRemove);
    }

    @Override
    public List<Area> evaluateTargets() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    @Override
    public Path getNextPath() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    @Override
    public Area getNextArea() {
        if (shouldCheckBuildings.isEmpty()) {
            targetBuilding = null;
            return null;
        }
        MrlBuilding mrlBuilding = world.getMrlBuilding(shouldCheckBuildings.remove(0));

        checkedBuildings.add(mrlBuilding.getID());
        return mrlBuilding.getSelfBuilding();
    }


}
