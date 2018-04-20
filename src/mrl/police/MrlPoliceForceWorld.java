package mrl.police;

import mrl.helper.RoadHelper;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import rescuecore2.config.Config;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * created by Mandana.
 * Date: May 5, 2010
 * Time: 11:28:02 PM
 */
public class MrlPoliceForceWorld extends MrlWorld {

    RoadHelper roadHelper;
//    Set<Path> badePaths = new FastSet<Path>();
//    TargetToGoHelper targetToGoHelper;
//    PoliceDecision decision;

    public MrlPoliceForceWorld(StandardAgent standardAgent, Collection<? extends Entity> entities, Config config) {
        super(standardAgent, entities, config);
        roadHelper = this.getHelper(RoadHelper.class);
//        targetToGoHelper = new TargetToGoHelper(this);
//        helpers.add(targetToGoHelper);
//        decision = new PoliceDecision(this);
    }

    public List<EntityID> getPathsIDsOfThisArea(Area area) {
        List<EntityID> pathsId = new ArrayList<EntityID>();
        List<Path> paths = getPathsOfThisArea(area);

        for (Path path : paths) {
            pathsId.add(path.getId());
        }
        return pathsId;
    }

    public Path getPath(int pathId) {
        for (Path path : getPaths()) {
            if (path.getId().equals(new EntityID(pathId)))
                return path;
        }
        return null;
    }

    public Path getPath(EntityID entityID) {
        if (this.getEntity(entityID) instanceof Road) {
            // Road road = (Road)this.getEntity(entityID);
            for (Path path : getPaths()) {
                if (path.containsId(entityID)) {
                    return path;
                }
            }
        }
        return null;
    }

//    private void fillViewer() {
//        p.createPaths();
//        ArrayList<D_Road> roads = new ArrayList<D_Road>();
//        for (Path p : this.paths) {
//            EntityID id = p.getId();
//            roads.add(new D_Road(id.getValue()));
//        }
//        this.getDebugDataObject().paths().add(new D_Path(roads));
//    }


    @Override
    public void updateAfterSense() {
        super.updateAfterSense();
//        targetToGoHelper.update();
    }


//    public List<Road> getConnectedRoadsToThisBuilding(Building building) {
//        Road road = null;
//        Set<Road> connectedRoads = new HashSet<Road>();
//        List<Road> connectedRoadsL = new ArrayList<Road>();
//        BuildingHelper buildingHelper = getHelper(BuildingHelper.class);
//
//        for (Entrance entrance : getMrlBuilding(building.getID()).getEntrances()) {
//            if (entrance.getNeighbour() instanceof Road)
//                road = (Road) entrance.getNeighbour();
//            connectedRoads.add(road);
//        }
//        connectedRoadsL.addAll(connectedRoads);
//        return connectedRoadsL;
//    }

//    public Map<Integer, ArrayList<PoliceForceBid>> getBidsMap() {
//        return bidsMap;
//    }

//    public List<PoliceForceBid> getBadePaths() {
//        return badePaths;
//    }

//    public Set<Path> getBadePaths() {
//        return badePaths;
//    }
//
//    public RoadHelper getRoadHelper() {
//        return roadHelper;
//    }
//
//    public TargetToGoHelper getTargetToGoHelper() {
//        return targetToGoHelper;
//    }
//
//    public PoliceDecision getDecision() {
//        return decision;
//    }


}
