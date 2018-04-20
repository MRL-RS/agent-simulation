package mrl.platoon.genericsearch;

import mrl.common.MRLConstants;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import mrl.world.routing.path.Path;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * @author Mahdi
 */
public class SimpleSearchDecisionMaker extends SearchDecisionMaker {
    Map<Path, Set<Building>> pathBuildingsMap;
    Set<Building> shouldDiscoverBuildings;
    List<Building> discoveredBuildings;
    List<Path> shouldDiscoverPaths;
    List<Building> totalBuildings;
    List<Building> validBuildings;
    List<Path> discoveredPaths;

    Building buildingInProgress;
    Path pathInProgress;


    public SimpleSearchDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    public void update() {
        super.update();
        if (isPartitionChanged()) {
            resetSearch();
        }
        setShouldDiscoverPaths();
    }

    @Override
    public void initialize() {
        pathBuildingsMap = new HashMap<Path, Set<Building>>();
        discoveredPaths = new ArrayList<Path>();
        discoveredBuildings = new ArrayList<Building>();

        shouldDiscoverBuildings = new HashSet<Building>();
        shouldDiscoverPaths = new ArrayList<Path>();
        buildingInProgress = null;
        totalBuildings = new ArrayList<Building>();
        validBuildings = new ArrayList<Building>();
        Building building;
        for (StandardEntity buildingEntity : world.getBuildings()) {
            building = (Building) buildingEntity;
            totalBuildings.add(building);
        }
        for (EntityID buildingID : super.validBuildings) {
            building = world.getEntity(buildingID, Building.class);
            validBuildings.add(building);
        }
        setPathBuildingsMap();
        pathInProgress = null;
    }

    @Override
    public List<Area> evaluateTargets() {
        return null;
    }

    @Override
    public Path getNextPath() {
        Path nextPath = null;
        if (pathInProgress == null) {
            if (MRLConstants.DEBUG_SEARCH)
                world.printData("no path in progress... choose present path.");
            if (!shouldDiscoverPaths.isEmpty()) {
                nextPath = getMyPath();
                if (!shouldDiscoverPaths.contains(nextPath)) {
                    nextPath = shouldDiscoverPaths.get(0);
                }
            } else {
                if (MRLConstants.DEBUG_SEARCH)
                    world.printData("shouldDiscoverPath is empty!!! going to reset it.\nDiscovered paths:" + discoveredPaths.size() + "\nValid paths:" + validPaths.size());
                discoveredPaths.clear();
                setShouldDiscoverPaths();
            }
//                nextPath = shouldDiscoverPaths.get(0);
//            } else {
//                nextPath = null;
//            }
        } else {
            Set<Path> neighbours = pathInProgress.getNeighbours();
            for (Path path : neighbours) {
                if (shouldDiscoverPaths.contains(path)) {
                    nextPath = path;
                    break;
                }
            }
            if (nextPath == null) {
                if (shouldDiscoverPaths.isEmpty()) {
                    if (MRLConstants.DEBUG_SEARCH)
                        world.printData("shouldDiscoverPath is empty!!! going to reset it.\nDiscovered paths:" + discoveredPaths.size() + "\nValid paths:" + validPaths.size());
                    discoveredPaths.clear();
                    setShouldDiscoverPaths();
                } else {
                    int size = shouldDiscoverPaths.size();
                    Random random = new Random(System.currentTimeMillis());
                    int index = Math.abs(random.nextInt()) % size;
                    nextPath = shouldDiscoverPaths.get(index);
                }
            }
        }
        if (nextPath != null) {
            shouldDiscoverPaths.remove(nextPath);
            if (!discoveredPaths.contains(nextPath))
                discoveredPaths.add(nextPath);
        }
        pathInProgress = nextPath;
        return pathInProgress;
    }

    @Override
    public Area getNextArea() {
        throw new UnsupportedOperationException();
    }

    private void setPathBuildingsMap() {
        Set<Building> buildings = new HashSet<Building>();
        for (Path path : world.getPaths()) {
            buildings.clear();
            for (Area area : path.getBuildings()) {
                buildings.add((Building) area);
            }
            pathBuildingsMap.put(path, buildings);
        }
    }

    private void setShouldDiscoverPaths() {
        shouldDiscoverPaths.clear();
        if (searchInPartition) {
            shouldDiscoverPaths.addAll(validPaths);
        } else {
            shouldDiscoverPaths.addAll(world.getPaths());
        }
        shouldDiscoverPaths.removeAll(discoveredPaths);
    }

    private Path getMyPath() {
        Path myPath;
        if (world.getSelfPosition() instanceof Road) {
            MrlRoad mrlRoad = world.getMrlRoad(world.getSelfPosition().getID());
            myPath = mrlRoad.getPaths().get(0);
        } else {
            MrlBuilding mrlBuilding = world.getMrlBuilding(world.getSelfPosition().getID());
            Road roadEntrance = mrlBuilding.getEntrances().get(0).getNeighbour();
            MrlRoad mrlRoad = world.getMrlRoad(roadEntrance.getID());
            myPath = mrlRoad.getPaths().get(0);
        }
        if (myPath == null) {
            if (MRLConstants.DEBUG_SEARCH)
                world.printData("myPath = null");
        }
        return myPath;
    }

    private void resetSearch() {
        shouldDiscoverPaths.clear();
        pathInProgress = null;
        buildingInProgress = null;
    }

//    private void setShouldDiscoverBuildings(Path path) {
//        shouldDiscoverBuildings = pathBuildingsMap.get(path);
//    }
}
