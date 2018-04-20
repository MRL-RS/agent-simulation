package mrl.platoon.genericsearch;

import javolution.util.FastSet;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Siavash
 */
public class LegacyHeatTracerDecisionMaker extends SearchDecisionMaker {

    private final static int HEAT_THRESHOLD = 1;
    private int SEARCH_RADIUS = 40000;
    private Set<Building> warmBuildings = new FastSet<Building>();
    private Set<Building> checkedBuildings = new FastSet<Building>();
    private Set<Road> checkedRoads = new FastSet<Road>();
    private Set<Road> importantRoads = new FastSet<Road>();
    private Set<Path> importantPaths = new FastSet<Path>();
    private Set<Path> checkedPaths = new FastSet<Path>();
    private Set<Building> shouldCheckBuildings = new FastSet<Building>();

    public LegacyHeatTracerDecisionMaker(MrlWorld world) {
        super(world);
        if (world.isMapHuge()) {
            SEARCH_RADIUS *= 2;
        }
    }

//    @Override
//    public void update() {
//        super.update();
//        setWarmBuildings();
//        setShouldCheckBuildings();
//        setImportantRoads();
//        setImportantPaths();
//    }

    private void setWarmBuildings() {
        warmBuildings.clear();
        for (Building building : world.getBuildingSeen()) {
            if (building.isTemperatureDefined() && building.getTemperature() > HEAT_THRESHOLD) {
                warmBuildings.add(building);
            }
        }
    }

    private void setShouldCheckBuildings() {
        for (Building building : warmBuildings) {
            if (building.isFierynessDefined()) {
                int fieryness = building.getFieryness();
                if (fieryness != 4 && fieryness != 7 && fieryness != 8) {
                    shouldCheckBuildings.add(building);
                }
            }
        }
        MrlBuilding mrlBuilding;
        List<Building> toRemove = new ArrayList<Building>();
        for (Building building : shouldCheckBuildings) {
            mrlBuilding = world.getMrlBuilding(building.getID());
            if (world.getTime() - mrlBuilding.getSensedTime() > 10) { //todo replace constant value
                //not sensed for a long time . so remove it....
                toRemove.add(building);
            }
        }
        shouldCheckBuildings.removeAll(toRemove);

    }

    private void setImportantRoads() {
        importantRoads.clear();

        int minTemperature = Integer.MAX_VALUE;
        Building coldBuilding = null;
        for (Building tempBuilding : shouldCheckBuildings) {
            if (tempBuilding.getTemperature() < minTemperature) {
                minTemperature = tempBuilding.getTemperature();
                coldBuilding = tempBuilding;
            }
        }

        Road road;
        if (coldBuilding != null) {
            for (StandardEntity entity : world.getObjectsInRange(coldBuilding.getID(), SEARCH_RADIUS)) {
                if (entity instanceof Road) {
                    road = (Road) entity;
                    importantRoads.add(road);
                }
            }
        }
    }

    private void setImportantPaths() {
        importantPaths.clear();

        MrlRoad mrlRoad;
        for (Road road : importantRoads) {
            mrlRoad = world.getMrlRoad(road.getID());
            importantPaths.addAll(mrlRoad.getPaths());
        }

        if (searchInPartition) {
            importantPaths.retainAll(validPaths);
        }
    }

    @Override
    public void update() {
        super.update();
        Set<Path> tempImportantPaths = new FastSet<Path>();
        EntityID selfID = world.getSelf().getID();
        Building building;
        int fieryness;

        for (StandardEntity entity : world.getObjectsInRange(selfID, world.getViewDistance())) {//todo building seen may be better
            if (entity instanceof Building) {
                building = (Building) entity;
                if (building.isTemperatureDefined()) {
                    if (building.getTemperature() >= HEAT_THRESHOLD) {
                        warmBuildings.add(building);
                    }
                }
            }
        }

        for (Building b : warmBuildings) {
            if (b.getTemperature() < HEAT_THRESHOLD) {
                checkedBuildings.add(b);
            } else {
                checkedBuildings.remove(b);
            }

            if (b.isFierynessDefined()) {
                fieryness = b.getFieryness();
                if (fieryness == 3
                        || fieryness == 7
                        || fieryness == 8) {
                    checkedBuildings.add(b);
                } else {
                    checkedBuildings.remove(b);
                }
            }
        }

        warmBuildings.removeAll(checkedBuildings);

        int maxTemperature = Integer.MIN_VALUE;
        int minTemperature = Integer.MAX_VALUE;
        Building hotBuilding = null;
        Building coldBuilding = null;

        for (Building tempBuilding : warmBuildings) {
            if (tempBuilding.getTemperature() > maxTemperature) {
                maxTemperature = tempBuilding.getTemperature();
                hotBuilding = tempBuilding;
            }

            if (tempBuilding.getTemperature() < minTemperature) {
                minTemperature = tempBuilding.getTemperature();
                coldBuilding = tempBuilding;
            }
        }

        Road road;
        if (coldBuilding != null) {
            for (StandardEntity entity : world.getObjectsInRange(coldBuilding.getID(), SEARCH_RADIUS)) {
                if (entity instanceof Road) {
                    road = (Road) entity;
                    importantRoads.add(road);
                }
            }
        }
        importantRoads.removeAll(checkedRoads);

        Path path;
        for (Road tmpRoad : importantRoads) {
            path = world.getPath(tmpRoad.getID());
            if (path != null) {
                importantPaths.add(path);
            }
        }

        for (Road tmpRoad : checkedRoads) {
            path = world.getPath(tmpRoad.getID());
            if (path != null) {
                checkedPaths.add(path);
            }
        }

        importantPaths.removeAll(checkedPaths);

        if (searchInPartition) {
            for (Path p : importantPaths) {
                if (validPaths.contains(p)) {
                    tempImportantPaths.add(p);
                }
            }

            importantPaths = tempImportantPaths;
        }
    }

    @Override
    public void initialize() {
        update();
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
        Road selectedRoad = null;
        Road checked = null;
        if (importantPaths != null) {
            int distance;
            int minDistance = Integer.MAX_VALUE;
            Road road;
            Path selectedPath = null;
            for (Path path : importantPaths) {
                road = path.getMiddleRoad();
                distance = world.getDistance(world.getSelf().getID(), road.getID());
                if (distance < minDistance) {
                    minDistance = distance;
                    selectedRoad = road;
                    selectedPath = path;
                }
            }
            if (selectedPath != null) {
                checkedPaths.add(selectedPath);
                checked = (Road) world.getEntity(selectedPath.getId());
                checkedRoads.add(checked);
            }
        }
        return selectedRoad;
    }
}
