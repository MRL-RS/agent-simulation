package mrl.platoon.genericsearch;

import javolution.util.FastSet;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.VisibilityHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Mahdi
 */
public class CivilianSearchStrategy extends SearchStrategy {
    protected Set<Building> buildings;
    private Building targetBuilding;
    private Map<Integer, EntityID> moveLogs;
    private int lastCycleExecute;
    private int thisCycleTryCount = 0;
    private Set<Polygon> polygonBlackList;
    private Set<Point> pointsBlackList;
    private Set<Building> discoveredBuildings;
    private boolean searchUnvisited;
    private Area destination;
    private int moveToPointCount;
    private VisibilityHelper visibilityHelper;

    public CivilianSearchStrategy(MrlPlatoonAgent agent, MrlWorld world) {
        super(agent, world);
        buildings = new FastSet<Building>();
        targetBuilding = null;
        moveLogs = new HashMap<Integer, EntityID>();
        polygonBlackList = new FastSet<Polygon>();
        lastCycleExecute = 0;
        discoveredBuildings = new HashSet<Building>();
        moveToPointCount = 0;
        pointsBlackList = new HashSet<Point>();
        this.visibilityHelper = world.getHelper(VisibilityHelper.class);
    }

    @Override
    public SearchStatus manualMoveToSearchingBuilding(Building targetBuildingToSearch, boolean searchInside, boolean searchUnvisitedBuildings) throws CommandException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchStatus search(boolean inPartition, boolean searchUnvisited) throws CommandException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SearchStatus searchPath() throws CommandException {
        if (MRLConstants.DEBUG_SEARCH)
            world.printData("start path search...");
        if (path == null) {
            return SearchStatus.CANCELED;
        }
        SearchStatus status;
        do {
            if (targetBuilding == null) {
                if (buildings.isEmpty()) {
                    if (MRLConstants.DEBUG_SEARCH)
                        world.printData(path + " have no any buildings!! we should try another path...");
                    break;
                }

                Building nearestBuilding = null;
                int minTTA = Integer.MAX_VALUE;
                for (Building building : buildings) {
                    int tta = simpleTTA(building);
                    if (tta < minTTA) {
                        minTTA = tta;
                        nearestBuilding = building;
                    }
                }
                if (MRLConstants.DEBUG_SEARCH)
                    world.printData(nearestBuilding + " is nearest building to me! assigned as target");
                targetBuilding = nearestBuilding;
            }
            status = searchBuilding(targetBuilding);
            if (status.equals(SearchStatus.CANCELED)) {
                if (MRLConstants.DEBUG_SEARCH)
                    world.printData(targetBuilding + " : CANCELED");
//                nearestBuilding.add(targetBuilding);
                buildings.remove(targetBuilding);
                targetBuilding = null;
            } else if (status.equals(SearchStatus.FINISHED)) {
                if (MRLConstants.DEBUG_SEARCH)
                    world.printData(targetBuilding + " : FINISHED");
                discoveredBuildings.add(targetBuilding);
                buildings.remove(targetBuilding);
                targetBuilding = null;
            }
        } while (!buildings.isEmpty());

        return SearchStatus.FINISHED;
    }


    @Override
    public SearchStatus searchBuilding(Building building) throws CommandException {
        if (MRLConstants.DEBUG_SEARCH) {
            world.printData("..............I'm going to search " + building);
        }
        if (world.getTime() == lastCycleExecute) {
            thisCycleTryCount++;
        } else {
            lastCycleExecute = world.getTime();
            thisCycleTryCount = 0;
        }

        if (building == null) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("building = null");
            }
            return SearchStatus.CANCELED;
        }

        if (amIInLoop()) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("I'M in loop. search failed");
            }
            thisCycleTryCount = 0;
            return SearchStatus.CANCELED;
        }
        MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
        if (this.building == null || !this.building.getID().equals(building.getID())) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("target changed!");
            }
            blackList.clear();
            polygonBlackList.clear();
            pointsBlackList.clear();
            moveLogs.clear();
            destination = null;
            this.building = building;
        } else {
            moveLogs.put(world.getTime(), world.getSelfPosition().getID());
        }
        if (searchUnvisited && mrlBuilding.isVisited()) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("This building have been visited before. search finished.");
            }
            return SearchStatus.FINISHED;
        } else if (world.isBuildingBurnt(building)) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("This building was burnt!");
            }
            return SearchStatus.FINISHED;
        } else if (!searchUnvisited && visibilityHelper.canISeeInside(mrlBuilding)) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("I can see inside of this building. search finished");
            }
            return SearchStatus.FINISHED;
        }

        if (!mrlBuilding.isVisitable()) {//todo is visitable may not work properly... double check and test it.....
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData(mrlBuilding + " is not visitable");
            }
            return SearchStatus.CANCELED;
        }

        if (destination != null) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("i am going to reach " + destination + ". this destination allocated before");
            }
            moveToPointCount = 0;
            agent.move(destination, MRLConstants.IN_TARGET, false);
            blackList.add(destination);
        }

        boolean checkInside = shouldCheckInside(mrlBuilding);

        if (checkInside) {
            if (MRLConstants.DEBUG_SEARCH) {
                world.printData("i should check inside of this building");
            }
            if (world.getSelfPosition().equals(building)) {
                if (MRLConstants.DEBUG_SEARCH) {
                    world.printData("I'm in target");
                }
                return SearchStatus.FINISHED;
            } else {
                destination = building;
                moveToPointCount = 0;
                agent.move(building, MRLConstants.IN_TARGET, false);
                if (MRLConstants.DEBUG_SEARCH) {
                    world.printData("i Can't Move inside of this building!");
                }
            }
        } else {
            destination = getDestinationArea(mrlBuilding);
            if (destination != null) {
                moveToPointCount = 0;
                if (MRLConstants.DEBUG_SEARCH) {
                    world.printData("I guess i can see inside of this target out of it! destination area: " + destination);
                }
                agent.move(destination, MRLConstants.IN_TARGET, false);
                if (MRLConstants.DEBUG_SEARCH) {
                    world.printData("failed to move in this destination. add it into blacklist!");
                }
                blackList.add(destination);
                destination = null;
                searchBuilding(building);
            } else {
                Pair<EntityID, Point> destinationPoint = getDestinationPoint(mrlBuilding);
                if (moveToPointCount >= 1) {
                    if (MRLConstants.DEBUG_SEARCH) {
                        world.printData("Move To a point in " + mrlBuilding + " visitable part failed!");
                    }
                    agent.move(building, MRLConstants.IN_TARGET, false);

                } else {
                    if (destinationPoint.first() != null && destinationPoint.second() != null) {
                        if (MRLConstants.DEBUG_SEARCH)
                            world.printData("Move to point...............");
                        destination = null;
                        if (world.getSelfPosition().getID().equals(destinationPoint.first())) {
                            moveToPointCount++;
                        }
                        agent.moveToPoint(destinationPoint.first(), destinationPoint.second().x, destinationPoint.second().y);
                    } else {
                        if (MRLConstants.DEBUG_SEARCH) {
                            world.printData("no point found to move on it. so move into building....");
                        }
                        agent.move(building, MRLConstants.IN_TARGET, false);
                        if (MRLConstants.DEBUG_SEARCH) {
                            world.printData("i can't move to this building! so set it not visitable!");
                        }
                        mrlBuilding.setVisitable(false);
                    }
                }
            }
        }

        destination = null;
        if (MRLConstants.DEBUG_SEARCH) {
            world.printData("buildingSearch failed.");
        }

        return SearchStatus.CANCELED;
    }

    private Area getDestinationArea(MrlBuilding mrlBuilding) {
        Map<EntityID, List<Polygon>> centerVisitRoadShapes = mrlBuilding.getCenterVisitRoadShapes();
        Set<Area> targets = new FastSet<Area>();
        //first try to choose roads which visible part shape of this building contains center of it
        for (EntityID roadID : centerVisitRoadShapes.keySet()) {
            Area target = world.getEntity(roadID, Area.class);
            Point point = new Point(target.getLocation(world).first(), target.getLocation(world).second());
            for (Polygon polygon : centerVisitRoadShapes.get(roadID)) {
                if (polygon.contains(point)) {
                    targets.add(target);
                }
            }
        }
//        targets.removeAll(blackList);
        if (targets.isEmpty()) {
            return null;
        }
        Area closestTarget = null;
        int minDist = Integer.MAX_VALUE;
        Point2D selfLocation = Util.getPoint(world.getSelfLocation());
        Point2D areaLocation;
        for (Area area : targets) {
//            if (!blackList.contains(area)) {
            areaLocation = Util.getPoint(area.getLocation(world));
            int distance = Util.distance(selfLocation, areaLocation);
            if (distance < minDist) {
                minDist = distance;
                closestTarget = area;
            }
//            }
        }
        return closestTarget;
    }

    private Pair<EntityID, Point> getDestinationPoint(MrlBuilding mrlBuilding) {
        EntityID destination;
        Point destinationPoint;
        Map<EntityID, List<Polygon>> centerVisitRoadShapes = mrlBuilding.getCenterVisitRoadShapes();
//        boolean firstTimeInLoop = true;
        Set<EntityID> roadSet = centerVisitRoadShapes.keySet();
        for (EntityID roadID : roadSet) {
            Area target = world.getEntity(roadID, Area.class);
//            if (!blackList.contains(target)) {
            if (mrlBuilding.getCenterVisitRoadPoints().containsKey(roadID)) {
                for (Point point : mrlBuilding.getCenterVisitRoadPoints().get(roadID)) {
                    if (pointsBlackList.contains(point)) {
                        continue;
                    }
                    destination = target.getID();
                    destinationPoint = new Point((int) point.getX(), (int) point.getY());

                    return new Pair<EntityID, Point>(destination, destinationPoint);
                }
            }
        }
        if (MRLConstants.DEBUG_SEARCH) {
            world.printData("no visitable point found in " + mrlBuilding);
        }
        return new Pair<EntityID, Point>(null, null);
    }

    private boolean amIInLoop() {
        if (thisCycleTryCount >= 5) {
            if (MRLConstants.DEBUG_SEARCH)
                world.printData("I have more than normal loop.");
            return true;
        }/* else if (thisCycleTryCount == 0) {
            int time = world.getTime();
            if (moveLogs.get(time) == moveLogs.get(time - 1)) {
                if (MRLConstants.DEBUG_SEARCH)
                    world.printData("I'm involved in loop.");
                return true;
            }
        }*/

        return false;
    }

    @Override
    public SearchStatus search() throws CommandException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSearchingPath(Path searchingPath, boolean searchUnvisited) {
        this.searchUnvisited = searchUnvisited;
        if (this.path == null || !this.path.getMiddleArea().equals(path.getMiddleArea())) {
            discoveredBuildings.clear();
        }
        this.path = searchingPath;
        setShouldDiscoverBuildings();
    }

    @Override
    public void setSearchUnvisited(boolean searchUnvisited) {
        this.searchUnvisited = searchUnvisited;
    }

    @Override
    public void setSearchingBuilding(Building searchingBuilding) {
        this.building = searchingBuilding;
    }

    /**
     * this method check this conditions:
     * if building is too large or has unNormal shape so that center of it will not be seen from out of. Therefore we should go inside of it!
     * but if building is in fire we should not see inside of it
     *
     * @param building target building
     * @return return true if agent should check inside of this building
     */
    private boolean shouldCheckInside(MrlBuilding building) {
        MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
        if (mrlBuilding.getCenterVisitRoadShapes().isEmpty()) {
            if (building.getSelfBuilding().isFierynessDefined()) {
                int fieryness = building.getSelfBuilding().getFieryness();
                if (fieryness == 0 || fieryness == 4) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }


    private void setShouldDiscoverBuildings() {
        buildings.clear();
        if (path == null) {
            return;
        }
        for (Area area : path.getBuildings()) {
            MrlBuilding mrlBuilding = world.getMrlBuilding(area.getID());
//            if (!discoveredBuildings.contains(mrlBuilding.getSelfBuilding())) {
            if (!this.searchUnvisited || !mrlBuilding.isVisited()) {
                buildings.add(mrlBuilding.getSelfBuilding());
            }
//            }
        }
    }

    /**
     * calculate time to arrive target with euclidean distance
     *
     * @param target target area entity
     * @return needed time
     */
    protected int simpleTTA(StandardEntity target) {
        StandardEntity position = world.getSelfPosition();
        double distance = Util.distance(position.getLocation(world), target.getLocation(world));
        return (int) Math.ceil(distance / MRLConstants.MEAN_VELOCITY_OF_MOVING);
    }
}
