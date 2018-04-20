package mrl.police;

import javolution.util.FastSet;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.RoadHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBlockade;
import mrl.world.object.MrlEdge;
import mrl.world.object.MrlRoad;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 4/1/12
 *         Time: 12:39 AM
 */
public class ClearHelper {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(ClearHelper.class);

    private MrlWorld world;
    private int maxClearDistance;
    Set<Blockade> blockadesToClear;
    private Blockade previousBlockade;
    private Blockade blockadeToMove;
    private RoadHelper roadHelper;
    private int tryCount;
    private static final int SECURE_RANGE = 300;
    private EntityID previousPosition = null;


    public ClearHelper(MrlWorld world) {
        this.world = world;
        this.maxClearDistance = world.getClearDistance();
        this.roadHelper = world.getHelper(RoadHelper.class);
        this.tryCount = 0;
    }

    /**
     * @param pathToGo    the paths which acting agent want to make it passable for reach target
     * @param targetID    the target that agent want to reach it
     * @param clearAround if this flag was true, acting agent clear around of itself
     * @throws CommandException
     */
    public void clearWay(List<EntityID> pathToGo, EntityID targetID, boolean clearAround) throws CommandException {
        if (clearAround) {
            clearAroundTarget(world.getSelfLocation());
        }
        Blockade targetBlockade = getTargetBlockade(pathToGo, targetID);
        previousBlockade = targetBlockade;
        if (targetBlockade != null) {
            if (isInClearRange(targetBlockade)) {
                Logger.info("Clearing blockade " + targetBlockade);
//                world.getMrlRoad(targetBlockade.getPosition()).setNeedUpdate(true);
                world.getPlatoonAgent().sendClearAct(world.getTime(), targetBlockade.getID());
            } else {
                if (blockadeToMove != null && blockadeToMove.equals(previousBlockade)) {
                    tryCount++;
                } else {
                    tryCount = 0;
                }
                blockadeToMove = previousBlockade;
//                if (tryCount > 1) { //todo commented by mahdi:
////                    blockadesToClear.clear();
////                    tryCount=0;
//                    if (pathToGo == null || pathToGo.isEmpty()) {
//                        findBlockadesInRange(world.getSelfLocation(), 1000);
//                    } else {
//                        findBlockadesOnWay(pathToGo, world.getRoadsSeen(), targetID);
//                    }
//                }

                if (targetBlockade.getPosition().equals(world.getSelfPosition().getID())) {
                    world.getPlatoonAgent().moveToPoint(targetBlockade.getPosition(), targetBlockade.getX(), targetBlockade.getY());
                } else if (pathToGo == null || pathToGo.isEmpty()) {
                    world.getPlatoonAgent().move((Area) world.getEntity(targetBlockade.getPosition()), 0, true);
                } else {
                    world.getPlatoonAgent().getPathPlanner().moveOnPlan(pathToGo);

                }

            }
        }
    }

    public void newClearWay(List<EntityID> pathToGo, EntityID targetID) throws CommandException {
        Point2D clearPoint = clearPoint(pathToGo, world.getClearDistance() + world.getClearRadius());
        if (clearPoint != null) {
//            int dist = Util.distance(Util.getPoint(world.getSelfLocation()), clearPoint);
//            if (dist < 11999) {
//                world.printData("clear distance is about " + dist);
//            }
            world.getPlatoonAgent().sendClearAct(world.getTime(), (int) clearPoint.getX(), (int) clearPoint.getY());
        } else {
            clearWay(pathToGo, targetID, false);
//            world.getPlatoonAgent().getPathPlanner().moveOnPlan(pathToGo);
        }
    }

    private Point2D clearPoint(List<EntityID> path, int range) {
        if (path == null || range < 0) {
            return null;
        }
        final double minimumRangeThreshold = range * 0.25;

        Area area;
        Point2D targetPoint = null;
        Point2D position = Util.getPoint(world.getSelfLocation());
        if (path.size() <= 1) {
            area = world.getEntity(world.getSelfPosition().getID(), Area.class);
            Point2D areaCenterPoint = Util.getPoint(area.getLocation(world));
            targetPoint = Util.clipLine(new Line2D(position, areaCenterPoint), range).getEndPoint();
        }
        if (path.size() > 1) {
            area = world.getEntity(path.get(0), Area.class);
            Edge edge = area.getEdgeTo(path.get(1));
            if (edge == null) {
                return null;
            }
            Point2D areaCenterPoint = Util.getPoint(area.getLocation(world));
            Point2D edgeCenterPoint = Util.getMiddle(edge.getLine());
            Point2D targetPoint2D = Util.clipLine(new Line2D(areaCenterPoint, edgeCenterPoint), range).getEndPoint();
//            targetPoint = Util.clipLine(new Line2D(position, edgeCenterPoint), range).getEndPoint();
            targetPoint = Util.clipLine(new Line2D(position, targetPoint2D), range).getEndPoint();
        }

        List<Road> roadsSeenInPath = new ArrayList<Road>();
        for (EntityID areaID : path) {
            area = world.getEntity(areaID, Area.class);
            if (area instanceof Road && world.getRoadsSeen().contains(area)) {
                roadsSeenInPath.add((Road) area);
            }
        }
        //target point is point that agent want to clear up to it.
        List<EntityID> checkedAreas = new ArrayList<EntityID>();
        if (targetPoint != null) {
            Line2D targetLine = new Line2D(position, targetPoint);
            Polygon polygon = Util.clearAreaRectangle(position.getX(), position.getY(), targetPoint.getX(), targetPoint.getY(), world.getClearRadius());
            Area neighbour;
            for (Road road : roadsSeenInPath) {//3 loop
                for (EntityID id : road.getNeighbours()) {
                    if (checkedAreas.contains(id) || path.contains(id)) {
                        //this area checked before...
                        continue;
                    }
                    checkedAreas.add(id);
                    neighbour = world.getEntity(id, Area.class);
                    if (!(neighbour instanceof Road)) {
                        //this area is not road! so no blockades is in it.
                        continue;
                    }
                    targetLine = normalizeClearLine(neighbour, targetLine, polygon, minimumRangeThreshold);
                    if (targetLine == null) {
                        return null;
                    }
                }
            }

            if (Util.lineLength(targetLine) < minimumRangeThreshold) {
                //it means new clear act is not profitable.
                return null;
            }
            if (anyBlockadeIntersection(roadsSeenInPath, targetLine)) {
                return targetLine.getEndPoint();
            }
        }
        return null;
    }

    public Line2D normalizeClearLine(Area road, Line2D targetLine, Polygon clearPoly, double minimumRangeThreshold) {

        List<Point2D> clearPointList = Util.getPoint2DList(clearPoly.xpoints, clearPoly.ypoints);
        Pair<Line2D, Line2D> lengthLines = Util.clearLengthLines(clearPointList, world.getClearRadius());
        Line2D finalLine = null;
        if (targetLine != null) {
            finalLine = new Line2D(targetLine.getOrigin(), targetLine.getEndPoint());
        }
        int dist1, dist2, dist, clearLength;
        MrlRoad mrlRoad = world.getMrlRoad(road.getID());
        Polygon neighbourPoly = Util.getPolygon(road.getApexList());

        for (Point2D point : clearPointList) {//4 loop
            if (neighbourPoly.contains(point.getX(), point.getY())) {
                //it means clear area may make blockades in out of clear way, bad shape an impassable!
                for (MrlBlockade blockade : mrlRoad.getMrlBlockades()) {
                    if (blockade.getPolygon().contains(point.getX(), point.getY())) {
                        //yeah! this blockades after clearing will be bad shape.
                        dist1 = decreaseLine(blockade.getPolygon(), lengthLines.first());
                        dist2 = decreaseLine(blockade.getPolygon(), lengthLines.second());
                        dist = Math.max(dist1, dist2);

                        //decrease distance is greater than or equals zero.
                        if (dist == 0) {
                            continue;
                        }
                        clearLength = (int) Util.lineLength(targetLine);
                        if (dist > 0 && (clearLength - dist > minimumRangeThreshold)) {
                            lengthLines = new Pair<Line2D, Line2D>(Util.improveLine(lengthLines.first(), -dist), Util.improveLine(lengthLines.second(), -dist));
                            finalLine = Util.improveLine(targetLine, -dist);
                        } else {
                            //it means new clear act is not profitable.
                            return null;
                        }
                    }
                }
            }
        }
        return finalLine;
    }

    private int decreaseLine(Polygon blockadePoly, Line2D clearLineLength) {
        List<Point2D> intersections = Util.intersections(blockadePoly, clearLineLength);
        int maxDist = 0;
        if (!intersections.isEmpty()) {
            for (Point2D point2D : intersections) {
                maxDist = Math.max(Util.distance(point2D, clearLineLength.getEndPoint()), maxDist);
            }
        }
        return maxDist;
    }

    private boolean anyBlockadeIntersection(List<Road> roadsSeenInPath, Line2D targetLine) {
        MrlRoad mrlRoad;
        for (Road road : roadsSeenInPath) {
            mrlRoad = world.getMrlRoad(road.getID());
            for (MrlBlockade mrlBlockade : mrlRoad.getMrlBlockades()) {
                if (Util.hasIntersection(mrlBlockade.getPolygon(), Util.improveLine(targetLine, -(world.getClearRadius() + 10)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<Blockade> getBlockadesInRange(Pair<Integer, Integer> targetLocation, Set<Blockade> blockadeSeen, int range) {
        Set<Blockade> blockades = new HashSet<Blockade>();
        for (Blockade blockade : blockadeSeen) {
            if (Util.findDistanceTo(blockade, targetLocation.first(), targetLocation.second()) < range) {
                blockades.add(blockade);
            }
        }
        return blockades;
    }

    /**
     * @param pathToGo
     * @param targetID
     * @throws CommandException
     */
    public void clearWay(List<EntityID> pathToGo, EntityID targetID) throws CommandException {
        clearWay(pathToGo, targetID, false);
//        newClearWay(pathToGo, targetID);
    }

    /**
     * this method is used when the agent want to clear around of target(usually in coincidental works)
     *
     * @param targetLocation
     * @throws CommandException
     */
    public void clearAroundTarget(Pair<Integer, Integer> targetLocation) throws CommandException {
        Set<Blockade> blockadesAround = getBlockadesInRange(targetLocation, world.getBlockadeSeen(), 1000);
        if (!blockadesAround.isEmpty()) {
            for (Blockade blockade : blockadesAround) {
                if (isInClearRange(blockade)) {
                    world.getPlatoonAgent().sendClearAct(world.getTime(), blockade.getID());
                }
            }
        }
    }


    /**
     * This method is used when the acting agent is inside a building and might not seen the outside of the building, so
     * it can not see what will happen outside, so it should do clear operation at last to the amount of blockades repair
     * cost range based on clear repairCost rate. By this method agent will process the {@code road} which is the entrance of the building and if it had
     * any blockades, tries to clear it at first, but after this clearing if the specified entrance was too small for the
     * agent to stand, then it should clear other neighbour roads to the specified entrance.
     *
     * @param road entrance to clear blockades around it
     * @throws mrl.common.CommandException it is thrown when the Clear act is done
     */
    public void clearBlockadeBlindly(Road road) throws CommandException {

        Blockade blockadeToClear = null;
        if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()) {
            clearNearestBlockade(road.getBlockades());
        }
        //TODO: must be completed

    }


    /**
     * This Method clears blockades which are in a specific range of a specific position
     *
     * @param position the specified position to clear blockades based on distance to it
     * @param range    the range to find and clear blockades from specified position
     * @throws CommandException it is an exception to give up handler to upper class
     */
    public void clearBlockadesInRange(Pair<Integer, Integer> position, int range) throws CommandException {

        Set<Blockade> blockades = findBlockadesInRange(position, range);
        clearNearestBlockade(blockades);

    }

    private void clearNearestBlockade(Set<Blockade> blockades) throws CommandException {

        Blockade blockadeToClear;
        Pair<Blockade, Integer> nearestBlockadePair = findNearestBlockade(blockades);
        if (nearestBlockadePair != null) {
            blockadeToClear = nearestBlockadePair.first();
            Logger.info("Clearing blockade " + blockadeToClear);
//            world.getMrlRoad(blockadeToClear.getPosition()).setNeedUpdate(true);
            world.getPlatoonAgent().sendClearAct(world.getTime(), blockadeToClear.getID());

        }
    }

    private void clearNearestBlockade(List<EntityID> blockades) throws CommandException {

        Set<Blockade> blocks = new FastSet<Blockade>();
        for (EntityID blockadeId : blockades) {
            blocks.add((Blockade) world.getEntity(blockadeId));
        }
        clearNearestBlockade(blocks);
    }


    /**
     * @param pathToGo
     * @param target
     * @return
     * @throws CommandException
     */
    private Blockade getTargetBlockade(List<EntityID> pathToGo, EntityID target) throws CommandException {

        Blockade blockadeToClear = null;

        // Find first blockade that is in range.
        if (blockadesToClear == null) {
            blockadesToClear = new FastSet<Blockade>();
        } else if (!blockadesToClear.isEmpty()) {
            Set<Blockade> tempSet = new FastSet<Blockade>();

            tempSet = refreshFoundBlockades(pathToGo, blockadesToClear);
            blockadesToClear.clear();
            blockadesToClear.addAll(tempSet);
        }


        if (!blockadesToClear.isEmpty()) {
            if (previousBlockade != null) {
                Road road = (Road) world.getEntity(previousBlockade.getPosition());
                if (road.getBlockades().contains(previousBlockade.getID())) {
                    previousBlockade = null;
                } else {
                    blockadesToClear.remove(previousBlockade);
                }
            }
        }


        if (blockadesToClear.isEmpty()) {
            if (pathToGo == null || pathToGo.isEmpty()) {
                //TODO: @Pooya check bellow line and change it with better one
//                blockadesToClear.addAll(getBlockadesOnWay_ImportanceBased(world.getPlatoonAgent().getPathPlanner().getNextPlan(), world.getRoadsSeen()));
//                blockadesToClear.addAll(findBlockadesInRange(world.getSelfLocation(), maxClearDistance));
                if (target != null && world.getSelfPosition() instanceof Road) {
                    blockadesToClear.addAll(getTargetRoadBlockades((Road) world.getSelfPosition(), null, target));
                }
                if (blockadesToClear.isEmpty()) {
                    //TODO: @Pooya check bellow line and check if it needs to be changed
//                    blockadeToClear = getTargetBlockade(pathToGo, target);

                    //do nothing
                } else {
                    Pair<Blockade, Integer> nearestBlockadePair = findNearestBlockade(blockadesToClear);
                    if (nearestBlockadePair != null) {
                        blockadeToClear = nearestBlockadePair.first();
                    }
                }
            } else {
                //TODO: @Pooya check bellow line and change it with better one(the one uses target based clearing(findBlockadesOnWay()))
//                blockadesToClear.addAll(getBlockadesOnWay_ImportanceBased(pathToGo, world.getRoadsSeen()));
                if (target == null) {
                    blockadesToClear.addAll(findBlockadesInRange(world.getSelfLocation(), 1000));
                } else {
                    blockadesToClear.addAll(findBlockadesOnWay(pathToGo, world.getRoadsSeen(), target));
                }
                //add blockades around too small edges.
                MrlRoad mrlRoad;
                Pair<Integer, Integer> edgeMiddle;
                for (int i = 0; i < pathToGo.size() - 1; i++) {
                    mrlRoad = world.getMrlRoad(pathToGo.get(i));
                    if (mrlRoad == null || !world.getChanges().contains(pathToGo.get(i))) {
                        continue;
                    }
                    for (MrlEdge mrlEdge : mrlRoad.getMrlEdgesTo(pathToGo.get(i + 1))) {
                        if (mrlEdge.isTooSmall() /*&& mrlEdge.isBlocked() && world.getChanges().contains(pathToGo.get(i + 1))*/) {
                            edgeMiddle = new Pair<Integer, Integer>((int) mrlEdge.getMiddle().getX(), (int) mrlEdge.getMiddle().getY());
                            Set<Blockade> blockades = getBlockadesInRange(edgeMiddle, world.getBlockadeSeen(), MRLConstants.AGENT_SIZE);
//                            world.printData(blockades.size() + " blockade(s) added into blockades To Clear. these are around too small edge of " + mrlRoad.getParent());
                            blockadesToClear.addAll(blockades);
                        }
                    }
                }

                Pair<Blockade, Integer> nearestBlockadePair = findNearestBlockade(blockadesToClear);
                if (nearestBlockadePair != null) {
                    blockadeToClear = nearestBlockadePair.first();
                }

            }

        } else {

            if (pathToGo == null || pathToGo.isEmpty()) {
                blockadesToClear.addAll(findBlockadesInRange(world.getSelfLocation(), 1000));
            } else {
                blockadesToClear.addAll(findBlockadesOnWay(pathToGo, world.getRoadsSeen(), target));
            }

            Pair<Blockade, Integer> nearestBlockadePair = findNearestBlockade(blockadesToClear);

            if (nearestBlockadePair != null) {
                blockadeToClear = nearestBlockadePair.first();
            }
        }

        return blockadeToClear;
    }


    private Set<Blockade> refreshFoundBlockades(List<EntityID> pathToGo, Set<Blockade> blockadesToClear) {

        Set<Blockade> blockadesToRemove = new FastSet<Blockade>();
        Set<Blockade> blockades = new FastSet<Blockade>(blockadesToClear);
        Road road;
        for (Blockade blockade : blockades) {
            road = (Road) world.getEntity(blockade.getPosition());
            //if this blockade is not in a road that I can see it
            if (!world.getRoadsSeen().contains(road) || (road.isBlockadesDefined() && !road.getBlockades().contains(blockade.getID())) || (pathToGo != null && !pathToGo.contains(road.getID()))) {
                blockadesToRemove.add(blockade);
            }
        }

        blockades.removeAll(blockadesToRemove);
        return blockades;

    }

    /**
     * This method returns blockades which block the middle point of a road or two passing edges of way roads
     *
     * @param pathToGo  entityID list of path entities to a target
     * @param roadsSeen roads can be seen by the agent
     * @param target
     * @return blockades which might make troubles for path planning of any agents
     */
    public Set<Blockade> findBlockadesOnWay(List<EntityID> pathToGo, Set<Road> roadsSeen, EntityID target) {

        Set<MrlBlockade> obstacles_MrlBlockades = new HashSet<MrlBlockade>();
        Set<Blockade> blockades = new FastSet<Blockade>();


        MrlEdge sourceEdge = null;
        Pair<Integer, Integer> nextMiddlePoint = null;
        Pair<Integer, Integer> sourceMiddlePoint = null;


        //find entityID sequence of which can be seen
        List<EntityID> seenPath = new ArrayList<EntityID>();
        int count = 0;
        for (EntityID entityID : pathToGo) {
            if (world.getEntity(entityID) instanceof Road) {
                if (!roadsSeen.contains((Road) world.getEntity(entityID))) {
                    if (pathToGo.size() > count) {
                        seenPath.add(pathToGo.get(count));
                    }
                    break;
                }
            }
            count++;
            seenPath.add(entityID);
        }

        if (seenPath.isEmpty()) {
            //TODO: @Pooya What should be done?
        } else if (seenPath.size() == 1) {
            if (world.getEntity(seenPath.get(0)) instanceof Road) {
                if (target != null) {
                    blockades = getTargetRoadBlockades((Road) world.getEntity(seenPath.get(0)), null, target);
                } else {//if there is no target

                    //clear blockades in range
                    blockades = findBlockadesInRange(world.getSelfLocation(), 500 * 2);
                }
            }
        } else {// if seenPath contains more than one entity
            int j;
            EntityID sourceAreaID = null;
            EntityID nextAreaID = null;
            Area sourceArea = null;
            Area nextArea = null;
            Set<Edge> edgeSet;
            MrlRoad mrlRoad = null;
            MrlEdge nextEdge = null;
            List<EntityID> neighbours;
            for (int i = 0; i < seenPath.size() - 1; i++) {
                j = i + 1;
                sourceAreaID = seenPath.get(i);
                nextAreaID = seenPath.get(j);
                sourceArea = world.getEntity(sourceAreaID, Area.class);
                nextArea = world.getEntity(nextAreaID, Area.class);
                if (sourceArea instanceof Road) {
                    mrlRoad = world.getMrlRoad(sourceAreaID);
//                    neighbours = sourceArea.getNeighboursByEdge();
//                    for (EntityID neighbourID : neighbours) {
//                        // the neighbour of this area is also in my way, so it should be cleared
//                        if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID)>pathToGo.indexOf(sourceAreaID)) {
                    edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                    for (Edge edge : edgeSet) {
                        nextEdge = mrlRoad.getMrlEdge(edge);
                        nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                        if (sourceEdge == null) {

                            if (world.getSelfPosition().equals(sourceArea)) {
                                obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, world.getSelfLocation(), nextMiddlePoint));
                            } else {
                                obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceArea.getLocation(world), nextMiddlePoint));
                            }
                        } else {
                            sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                        }
                    }


//                        }
//                    }
//                    edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
//                    nextEdge=null;
//                    if(edgeSet.iterator().hasNext()){
//                        nextEdge=mrlRoad.getMrlEdge(edgeSet.iterator().next());
//                    }
                    sourceEdge = nextEdge;


                } else {
                    if (nextArea instanceof Road) {
                        mrlRoad = world.getMrlRoad(nextAreaID);
                        neighbours = sourceArea.getNeighbours();
                        for (EntityID neighbourID : neighbours) {
                            // the neighbour of this area is also in my way, so it should be cleared
                            if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID) > pathToGo.indexOf(sourceAreaID)) {
                                edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                                for (Edge edge : edgeSet) {
                                    nextEdge = mrlRoad.getMrlEdge(edge);
                                    nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                                    if (sourceEdge == null) {

                                        if (world.getSelfPosition().equals(sourceArea)) {
                                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, world.getSelfLocation(), nextMiddlePoint));
                                        } else {
                                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceArea.getLocation(world), nextMiddlePoint));
                                        }
                                    } else {
                                        sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                                        obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                                    }
                                }
                            }
                        }
                        edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                        nextEdge = null;
                        if (edgeSet.iterator().hasNext()) {
                            nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                        }
                        sourceEdge = nextEdge;
                    } else {
                        sourceEdge = null;
                    }
                }
            }

            //find blockades of last Entity in the sequence
            if (nextArea instanceof Road) {
                blockades = getTargetRoadBlockades((Road) nextArea, null, target);
            }
        }

//        // adding blockades of the entrances near the specified path
//        blockades.addAll(findEntrancesNearPath(seenPath));


        for (MrlBlockade blockade : obstacles_MrlBlockades) {
            blockades.add(blockade.getParent());
        }


        return blockades;

    }


    /**
     * This method returns blockades which block the middle point of a road or two passing edges of way roads
     *
     * @param pathToGo entityID list of path entities to a target
     * @param target
     * @return blockades which might make troubles for path planning of any agents
     */
    public boolean shouldCheckForBlockadesOnWay(List<EntityID> pathToGo, EntityID target) {

        boolean shouldCheck = false;
        Set<MrlBlockade> obstacles_MrlBlockades = new HashSet<MrlBlockade>();
        Set<Blockade> blockades = new FastSet<Blockade>();


        MrlEdge sourceEdge = null;
        Pair<Integer, Integer> nextMiddlePoint = null;
        Pair<Integer, Integer> sourceMiddlePoint = null;


        if (pathToGo.isEmpty()) {
            shouldCheck = false;
        } else if (pathToGo.size() == 1) {
            StandardEntity entity = world.getEntity(pathToGo.get(0));
            if (entity instanceof Road) {
                Road road = (Road) entity;
                if (road.isBlockadesDefined()) {

                    if (target != null) {
                        blockades = getTargetRoadBlockades(road, null, target);
                        if (blockades == null || blockades.isEmpty()) {
                            shouldCheck = false;
                        } else {
                            shouldCheck = true;
                        }
                    } else {//if there is no target
                        shouldCheck = false;
                    }
                } else {
                    shouldCheck = true;
                }

            }
        } else {// if seenPath contains more than one entity
            int j;
            EntityID sourceAreaID = null;
            EntityID nextAreaID = null;
            Area sourceArea = null;
            Area nextArea = null;
            Set<Edge> edgeSet;
            MrlRoad mrlRoad = null;
            MrlEdge nextEdge = null;
            List<EntityID> neighbours;
            for (int i = 0; i < pathToGo.size() - 1; i++) {
                j = i + 1;
                sourceAreaID = pathToGo.get(i);
                nextAreaID = pathToGo.get(j);
                sourceArea = world.getEntity(sourceAreaID, Area.class);
                nextArea = world.getEntity(nextAreaID, Area.class);
                if (sourceArea instanceof Road) {
                    mrlRoad = world.getMrlRoad(sourceAreaID);
//                    neighbours = sourceArea.getNeighboursByEdge();
//                    for (EntityID neighbourID : neighbours) {
//                        // the neighbour of this area is also in my way, so it should be cleared
//                        if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID)>pathToGo.indexOf(sourceAreaID)) {
                    edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                    for (Edge edge : edgeSet) {
                        nextEdge = mrlRoad.getMrlEdge(edge);
                        nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                        if (sourceEdge == null) {

                            if (world.getSelfPosition().equals(sourceArea)) {
                                obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, world.getSelfLocation(), nextMiddlePoint));
                            } else {
                                obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceArea.getLocation(world), nextMiddlePoint));
                            }
                        } else {
                            sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                        }

                        if (!obstacles_MrlBlockades.isEmpty()) {
                            shouldCheck = true;
                        }
                    }


                    if (shouldCheck) {
                        break;
                    }
//                        }
//                    }
//                    edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
//                    nextEdge=null;
//                    if(edgeSet.iterator().hasNext()){
//                        nextEdge=mrlRoad.getMrlEdge(edgeSet.iterator().next());
//                    }
                    sourceEdge = nextEdge;


                } else {
                    if (nextArea instanceof Road) {
                        mrlRoad = world.getMrlRoad(nextAreaID);
                        neighbours = sourceArea.getNeighbours();
                        for (EntityID neighbourID : neighbours) {
                            // the neighbour of this area is also in my way, so it should be cleared
                            if (pathToGo.contains(neighbourID) && pathToGo.indexOf(neighbourID) > pathToGo.indexOf(sourceAreaID)) {
                                edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                                for (Edge edge : edgeSet) {
                                    nextEdge = mrlRoad.getMrlEdge(edge);
                                    nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
                                    if (sourceEdge == null) {

                                        if (world.getSelfPosition().equals(sourceArea)) {
                                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, world.getSelfLocation(), nextMiddlePoint));
                                        } else {
                                            obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceArea.getLocation(world), nextMiddlePoint));
                                        }
                                    } else {
                                        sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
                                        obstacles_MrlBlockades.addAll(getRoadObstacles(mrlRoad, sourceMiddlePoint, nextMiddlePoint));
                                    }

                                    if (!obstacles_MrlBlockades.isEmpty()) {
                                        shouldCheck = true;
                                    }
                                }

                                if (shouldCheck) {
                                    break;
                                }

                            }
                        }

                        if (shouldCheck) {
                            break;
                        }

                        edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                        nextEdge = null;
                        if (edgeSet.iterator().hasNext()) {
                            nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                        }
                        sourceEdge = nextEdge;
                    } else {
                        sourceEdge = null;
                    }
                }
            }


            if (!shouldCheck) {


                //find blockades of last Entity in the sequence
                if (nextArea instanceof Road) {
//                    blockades = getTargetRoadBlockades((Road) nextArea, null, target);
                    Road road = (Road) nextArea;
                    if (road.isBlockadesDefined() && !road.getBlockades().isEmpty()) {
                        shouldCheck = true;
                    }
                }

                if (blockades != null && !blockades.isEmpty()) {
                    shouldCheck = true;
                }

            }
        }


        return shouldCheck;
    }


    private Set<Blockade> findEntrancesNearPath(List<EntityID> seenPath) {
        Set<EntityID> blockades = new FastSet<EntityID>();
        Set<Blockade> blockadeEntities = new FastSet<Blockade>();

        Road road;
        Area area;
        for (EntityID entityID : seenPath) {
            //if this entity itself is an entrance
            area = world.getEntity(entityID, Area.class);
            if (area instanceof Road) {
                road = (Road) area;
                if (world.getEntranceRoads().keySet().contains(entityID)) {
                    if (road.isBlockadesDefined()) {
                        blockades.addAll(road.getBlockades());
                    }
                }

            }

            //if neighbours of the path is also entrance
            for (EntityID neighbourID : area.getNeighbours()) {
                area = world.getEntity(neighbourID, Area.class);
                if (area instanceof Road) {
                    road = (Road) area;
                    if (world.getEntranceRoads().keySet().contains(entityID)) {
                        if (road.isBlockadesDefined()) {
                            blockades.addAll(road.getBlockades());
                        }
                    }

                }
            }

        }


        for (EntityID blockadeID : blockades) {
            blockadeEntities.add(world.getEntity(blockadeID, Blockade.class));
        }

        return blockadeEntities;
    }

    /**
     * @param road
     * @param position
     * @param target
     * @return
     */
    private Set<Blockade> getTargetRoadBlockades(Road road, Pair<Integer, Integer> position, EntityID target) {

        MrlRoad mrlRoad = world.getMrlRoad(road.getID());
        Set<Blockade> blockades = new FastSet<Blockade>();
        Pair<Integer, Integer> targetPosition;
        StandardEntity targetEntity = world.getEntity(target);
/*
        if (targetEntity instanceof Human) {
            Human human = (Human) targetEntity;
            if (human.isXDefined() && human.isYDefined()) {
                targetPosition = new Pair<Integer, Integer>(human.getX(), human.getY());
            } else {
                //nothing
            }
        } else {
            Area area = (Area) targetEntity;
            Set<Edge> edges = roadHelper.getEdgesBetween(road, area);
            if (edges != null && !edges.isEmpty()) {
                Edge e = edges.iterator().next();

                targetPosition = new Pair<Integer, Integer>(e.)
            }
        }
*/


        if (world.getRoadsSeen().contains(road) && road.isBlockadesDefined()) {
            blockades = new FastSet<Blockade>();
            for (EntityID entityID : road.getBlockades()) {
                blockades.add((Blockade) world.getEntity(entityID));
            }
        }

        return blockades;
    }


    /**
     * This method finds blockades in a specified range
     *
     * @param position XY position to find blockades to it
     * @param range    the range to find blockades in it
     * @return blockades in the specified range
     */
    private Set<Blockade> findBlockadesInRange(Pair<Integer, Integer> position, int range) {
        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        for (Blockade blockade : world.getBlockadeSeen()) {

            if (blockade.getShape().contains(position.first(), position.second()) || findDistanceTo(blockade, position.first(), position.second()) < range) {
                blockadeSet.add(blockade);
            }
        }
        return blockadeSet;
    }

    private int findDistanceTo(Blockade b, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int) best;
    }


    /**
     * THis method gets obstacles which has intersect/s with a line from {@code firstPoint} to {@code secondPoint}
     *
     * @param mrlRoad
     * @param firstPoint  head point of the expressed line
     * @param secondPoint end point of the expressed line
     * @return set of {@code MrlBlockade} which has intersect to the expressed line
     */
    public Set<MrlBlockade> getRoadObstacles(MrlRoad mrlRoad, Pair<Integer, Integer> firstPoint, Pair<Integer, Integer> secondPoint) {
        Set<MrlBlockade> obstacles = new HashSet<MrlBlockade>();
        for (MrlBlockade blockade : mrlRoad.getMrlBlockades()) {
            Point2D sourcePoint = Util.getPoint(firstPoint);
            Point2D endPoint = Util.getPoint(secondPoint);
            if (blockade.getPolygon().contains(firstPoint.first(), firstPoint.second()) || Util.intersections(blockade.getPolygon(), new Line2D(sourcePoint, endPoint)).size() > 0
                    || findDistanceTo(blockade.getParent(), firstPoint.first(), firstPoint.second()) < 500
                    || findDistanceTo(blockade.getParent(), secondPoint.first(), secondPoint.second()) < 500) {
                obstacles.add(blockade);
            }
        }

        return obstacles;
    }


    public void clearHere() throws CommandException {
        // Am I near a blockade?
//        Blockade target = getTargetBlockade(pathToGo, target);
//        if (target != null) {
//            Logger.info("Clearing blockade " + target);
//            world.getPlatoonAgent().sendClearAct(world.getTime(), target.getID());
//        }

    }

    public void clearHere(List<EntityID> pathToGo) throws CommandException {

        Blockade target = getTargetBlockade(pathToGo);
        if (target != null) {
            Logger.info("Clearing blockade " + target);
            world.getPlatoonAgent().sendClearAct(world.getTime(), target.getID());
        }

    }

    private boolean isInClearRange(Blockade target) {
//        return Util.distance(world.getSelfLocation(), target.getLocation(world)) <= maxClearDistance;
        return findDistanceTo(target, world.getSelfLocation().first(), world.getSelfLocation().second()) < maxClearDistance;
    }

    private Blockade getTargetBlockade(List<EntityID> pathToGo) throws CommandException {
        Logger.debug("Looking for target blockade");
        Area location = (Area) world.getSelfPosition();
        Logger.debug("Looking in current location");
        Blockade result = getTargetBlockade(location, maxClearDistance);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area) world.getEntity(next);
            result = getTargetBlockade(location, maxClearDistance);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Blockade getTargetBlockade(Area area, int maxDistance) throws CommandException {
        //        Logger.debug("Looking for nearest blockade in " + area);
        if (area == null || !area.isBlockadesDefined()) {
            //            Logger.debug("Blockades undefined");
            return null;
        }
        List<EntityID> ids = area.getBlockades();
        // Find the first blockade that is in range.
        Set<Blockade> blockadesToClear = new FastSet<Blockade>();

        for (EntityID next : ids) {
            Blockade b = (Blockade) world.getEntity(next);
            blockadesToClear.add(b);
        }

        Pair<Blockade, Integer> nearestBlockadePair = findNearestBlockade(blockadesToClear);
        moveToBlockadeIfIsAway(nearestBlockadePair);

        if (nearestBlockadePair != null) {
            return nearestBlockadePair.first();
        }

        //        Logger.debug("No blockades in range");
        return null;
    }

    private Set<Blockade> getImportantBlockades(Set<Road> roadsSeen) {
        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        Blockade blockade;
        for (Road road : roadsSeen) {
            if (!road.isBlockadesDefined()) {
                continue;
            }
            //if is on entrance road
            if (world.isEntrance(road)) {
                for (EntityID entityID : road.getBlockades()) {
                    blockade = (Blockade) (world.getEntity(entityID));
                    if (world.getBlockadeSeen().contains(blockade)) {
                        blockadeSet.add(blockade);
                    }
                }
            }

            //if is on a passable edge
            for (Edge edge : road.getEdges()) {
                if (!edge.isPassable()) {
                    continue;
                }
                for (EntityID blockadeID : road.getBlockades()) {
                    blockade = (Blockade) world.getEntity(blockadeID);
                    rescuecore2.misc.geometry.Point2D middlePoint = Util.getMiddle(edge.getStart(), edge.getEnd());
                    if (findDistanceTo(blockade, (int) middlePoint.getX(), (int) middlePoint.getY()) < 600) {
                        blockadeSet.add(blockade);
                    }

                }
            }


        }
        return blockadeSet;
    }

    private Set<Blockade> getBlockadesOnWay(List<EntityID> pathToGo, Set<Road> roadsSeen) {


        Set<Blockade> blockadeSet = new FastSet<Blockade>();

        if (pathToGo == null || pathToGo.isEmpty()) {
            return blockadeSet;
        }

        Blockade blockade;

        Set<Road> onWayEntrances = getOnWayEntrances(pathToGo);


        for (Road road : roadsSeen) {
            if (!road.isBlockadesDefined()) {
                continue;
            }

            //if is on way road
            if (!pathToGo.contains(road.getID())) {
                //do nothing
            } else {
                if (road.isBlockadesDefined()) {
                    for (EntityID blockadeID : road.getBlockades()) {
                        blockadeSet.add((Blockade) world.getEntity(blockadeID));
                    }
                }
            }

            //if is on entrance road
            if (onWayEntrances.contains(road)) {
                if (road.isBlockadesDefined() && !isEntranceOfBurningBuilding(road)) {
                    for (EntityID blockadeID : road.getBlockades()) {
                        blockadeSet.add((Blockade) world.getEntity(blockadeID));
                    }
                }

                blockadeSet.addAll(getBlockadesInRange(road, world.getBlockadeSeen()));

            }
//            if (world.isEntrance(road)) {
//                for (EntityID entityID : road.getBlockades()) {
//                    blockade = (Blockade) (world.getEntity(entityID));
//                    if (world.getBlockadeSeen().contains(blockade)) {
//                        blockadeSet.add(blockade);
//                    }
//                }
//            }


        }
        return blockadeSet;
    }


//    private Set<MrlBlockade> getBlockadesToClearInArea(Area sourceArea, Area nextArea, MrlEdge sourceEdge) {
//
//        Pair<Integer, Integer> nextMiddlePoint;
//        Pair<Integer, Integer> sourceMiddlePoint;
//        Set<MrlBlockade> obstacles = null;
//
//        MrlRoad mrlRoad = world.getMrlRoad();
//        Set<Edge> edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
//        if (edgeSet.iterator().hasNext()) {
//            MrlEdge nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
//            nextMiddlePoint = new Pair<Integer, Integer>((int) nextEdge.getMiddle().getX(), (int) nextEdge.getMiddle().getY());
//            if (sourceEdge == null) {
//
//                if (world.getSelfPosition().equals(sourceArea)) {
//                    obstacles = mrlRoad.getRoadObstacles(world.getSelfLocation(), nextMiddlePoint);
//                } else {
//                    obstacles = mrlRoad.getRoadObstacles(sourceArea.getLocation(world), nextMiddlePoint);
//                }
//            } else {
//                sourceMiddlePoint = new Pair<Integer, Integer>((int) sourceEdge.getMiddle().getX(), (int) sourceEdge.getMiddle().getY());
//                obstacles = mrlRoad.getRoadObstacles(sourceMiddlePoint, nextMiddlePoint);
//            }
//            sourceEdge = nextEdge;
//        }
//
//        return obstacles;
//
//    }


    public Set<Blockade> getBlockadesOnWay_ImportanceBased(List<EntityID> pathToGo, Set<Road> roadsSeen) {
        Set<MrlBlockade> obstacles_MrlBlockades = new HashSet<MrlBlockade>();
        Set<Blockade> blockades = new FastSet<Blockade>();

        RoadHelper roadHelper = world.getHelper(RoadHelper.class);
        MrlEdge sourceEdge = null;


        //find entityID sequence of which can be seen
        List<EntityID> seenPath = new ArrayList<EntityID>();
        for (EntityID entityID : pathToGo) {
            if (world.getEntity(entityID) instanceof Road) {
                if (!roadsSeen.contains((Road) world.getEntity(entityID))) {
                    break;
                }
            }
            seenPath.add(entityID);
        }

        for (int i = 0; i < seenPath.size() - 1; i++) {
            int j = i + 1;
            EntityID entityID = seenPath.get(i);
            EntityID nextAreaID = seenPath.get(j);
            Area sourceArea = world.getEntity(entityID, Area.class);
            Area nextArea = world.getEntity(nextAreaID, Area.class);
            if (sourceArea instanceof Road) {
                Road road = (Road) sourceArea;
//                if (!roadsSeen.contains(road)) {
//                    break;
//                }
                MrlRoad mrlRoad = world.getMrlRoad(entityID);
                Set<Edge> edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                if (edgeSet.iterator().hasNext()) {
                    MrlEdge nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                    if (sourceEdge == null) {
                        for (MrlEdge mrlEdge : mrlRoad.getMrlEdges()) {
                            if (mrlEdge.isPassable() && !mrlEdge.equals(nextEdge)) {
                                sourceEdge = mrlEdge;
                                break;
                            }
                        }
                    }
                    obstacles_MrlBlockades.addAll(mrlRoad.getObstacles(sourceEdge, nextEdge));
                    sourceEdge = nextEdge;
                }
            } else {
                if (nextArea instanceof Road) {
                    Road road = (Road) nextArea;
//                    if (!roadsSeen.contains(road)) {
//                        break;
//                    }
                    MrlRoad mrlRoad = world.getMrlRoad(nextAreaID);
                    Set<Edge> edgeSet = roadHelper.getEdgesBetween(sourceArea, nextArea);
                    if (edgeSet.iterator().hasNext()) {
                        MrlEdge nextEdge = mrlRoad.getMrlEdge(edgeSet.iterator().next());
                        if (sourceEdge == null) {
                            for (MrlEdge mrlEdge : mrlRoad.getMrlEdges()) {
                                if (mrlEdge.isPassable() && !mrlEdge.equals(nextEdge)) {
                                    sourceEdge = mrlEdge;
                                    break;
                                }
                            }
                        }
                        obstacles_MrlBlockades.addAll(mrlRoad.getObstacles(sourceEdge, nextEdge));
                        sourceEdge = nextEdge;
                    }
                } else {
                    sourceEdge = null;
                }
            }
        }


        for (MrlBlockade blockade : obstacles_MrlBlockades) {
            blockades.add(blockade.getParent());
        }


        return blockades;
    }


    private boolean isEntranceOfBurningBuilding(Road road) {
        Building building = (Building) world.getEntity(world.getEntranceRoads().get(road.getID()));
        boolean isFierynessDefined = building.isFierynessDefined();
        int fieryness = -1;
        if (isFierynessDefined) {
            fieryness = building.getFieryness();
        }
        return isFierynessDefined && (fieryness == 1 || fieryness == 2 || fieryness == 3 || fieryness == 7 || fieryness == 8);
    }

    private Set<Blockade> getBlockadesInRange(Road road, Set<Blockade> blockadeSeen) {

        Set<Blockade> blockadeSet = new FastSet<Blockade>();
        for (Blockade blockade : blockadeSeen) {
            for (Edge edge : road.getEdges()) {
                if (edge.isPassable() && findDistanceTo(blockade, road.getX(), road.getY()) < 1200) {
                    blockadeSet.add(blockade);
                }

            }
        }

        return blockadeSet;

    }


    private Set<Road> getOnWayEntrances(List<EntityID> pathToGo) {

        Set<Road> onWayEntrances = new FastSet<Road>();
        Road road;
        for (EntityID entityID : pathToGo) {
            if (world.getEntity(entityID) instanceof Road) {
                road = (Road) world.getEntity(entityID);
                //if it is an entrance
                if (world.getEntranceRoads().containsKey(entityID)) {
                    onWayEntrances.add(road);
                } else {// if its neighbours are entrance
                    for (EntityID neighbourID : road.getNeighbours()) {
                        if (world.getEntity(neighbourID) instanceof Road) {
                            road = (Road) world.getEntity(neighbourID);
                            if (world.getEntranceRoads().keySet().contains(neighbourID)) {
                                if (!isEntranceOfBurningBuilding(road)) {
                                    onWayEntrances.add(road);
                                }
                            }
                        }
                    }

                }
            }

        }

        return onWayEntrances;
    }


    private Pair<Blockade, Integer> findNearestBlockade(Set<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return null;
        }

        int x = world.getSelfLocation().first();
        int y = world.getSelfLocation().second();


        int minDistance = Integer.MAX_VALUE;
        Pair<Blockade, Integer> nearestBlockadePair = new Pair<Blockade, Integer>(blockades.iterator().next(), 0);
        int tempDistance;
        for (Blockade blockade : blockades) {
//            tempDistance = Util.distance(world.getSelfHuman().getX(), world.getSelfHuman().getY(), blockade.getX(), blockade.getY());
            tempDistance = findDistanceTo(blockade, x, y);
            if (tempDistance < minDistance) {
                minDistance = tempDistance;
                nearestBlockadePair = new Pair<Blockade, Integer>(blockade, minDistance);
            }
        }

        return nearestBlockadePair;
    }

    private void moveToBlockadeIfIsAway(Pair<Blockade, Integer> blockadePair) throws CommandException {
        if (blockadePair == null) {
            return;
        }
        if (blockadePair.second() < maxClearDistance) {
            //do nothing
        } else {
            world.getPlatoonAgent().moveToPoint(blockadePair.first().getPosition(), blockadePair.first().getX(), blockadePair.first().getY());
        }

    }


}
