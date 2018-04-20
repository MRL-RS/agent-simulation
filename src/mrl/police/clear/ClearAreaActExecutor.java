package mrl.police.clear;

import com.poths.rna.data.Point;
import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.RoadHelper;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Mahdi
 */
public class ClearAreaActExecutor extends ClearActExecutor {

    private double clearRange;
    private static final int SECURE_RANGE = 1000;
    private Line2D lastClearLine;
    ActResult result;
    private RoadHelper roadHelper;
    private boolean wasOnBlockade = false;
    private boolean clearBlockadesOnWay = false;
//    private List<EntityID> lastPath;

    protected ClearAreaActExecutor(MrlWorld world) {
        super(world);
        clearRange = world.getClearDistance();
        roadHelper = world.getHelper(RoadHelper.class);
    }

    @Override
    public ActResult clearWay(List<EntityID> path, EntityID targetID) throws CommandException {
        Line2D clearLine;
        //Debug PF loop when reach to building entrance and no path was returned.
        StandardEntity targetEntity = world.getEntity(targetID);
        if((targetEntity instanceof Area) && path.size()<=1 && !path.contains(targetID)){
            List<EntityID> newPath = world.getPlatoonAgent().getPathPlanner().planMove(
                    (Area) world.getSelfPosition(),
                    (Area) targetEntity, MRLConstants.IN_TARGET, true
            );
            if(path.size()<newPath.size()){
                path = newPath;
            }
        }

        GuideLine guideLine = getTargetGuideline(path, clearRange, targetID);
        MrlPersonalData.VIEWER_DATA.setPFGuideline(world.getSelf().getID(), guideLine);

        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        if (guideLine != null) {
            PositionActState positionActState = moveToGuideLine(areasSeenInPath, guideLine);
//            if (positionActState.equals(PositionActState.KEEP_MOVING)) {
//                return ActResult.SUCCESSFULLY_COMPLETE;
//            }
        }

        clearLine = getTargetClearLine(areasSeenInPath, clearRange, guideLine);

        Pair<Line2D, Line2D> clearSecureLines = null;
        Point2D agentPosition = Util.getPoint(world.getSelfLocation());
        if (clearLine != null) {
            Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(agentPosition, clearLine.getEndPoint(), world.getClearRadius()), world.getClearRadius());

            double distance = (world.getClearRadius() - MRLConstants.AGENT_SIZE * 0.5) / 2;
            clearSecureLines = clearTools.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), world.getClearRadius(), distance);
            MrlPersonalData.VIEWER_DATA.setPFClearAreaLines(world.getSelf().getID(), clearLine, clearSecureLines.first(), clearSecureLines.second());
        }

        List<EntityID> thisRoadPath = new ArrayList<EntityID>();
        thisRoadPath.add(world.getSelfPosition().getID());

        if (clearLine != null &&
                (anyBlockadeIntersection(areasSeenInPath, clearLine, true) || anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), true) || anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), true))) {
            lastClearLine = clearLine;
            wasOnBlockade = clearTools.isOnBlockade(clearLine.getEndPoint(), areasSeenInPath);
            world.getPlatoonAgent().sendClearAct(world.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
        } else {
            if (wasOnBlockade && lastClearLine != null) {//move to point to the end of lastClearLine if was on the blockade!
                int x = (int) lastClearLine.getEndPoint().getX();
                int y = (int) lastClearLine.getEndPoint().getY();
                lastClearLine = null;
                world.getPlatoonAgent().sendMoveAct(world.getTime(), thisRoadPath, x, y);
            } else if (clearBlockadesOnWay && guideLine != null) {
                //looking for blockades on way....
                Point2D nearestIntersect = anyBlockadeIntersection(guideLine);
                if (nearestIntersect != null) {
                    int dist = Util.distance(agentPosition, nearestIntersect);
                    clearLine = new Line2D(agentPosition, nearestIntersect);
                    clearLine = Util.clipLine(clearLine, clearRange - world.getClearRadius() - SECURE_RANGE);
                    lastClearLine = clearLine;
                    wasOnBlockade = clearTools.isOnBlockade(clearLine.getEndPoint(), areasSeenInPath);
                    if (dist < clearRange - world.getClearRadius() - SECURE_RANGE) {
//                        world.printData("I found blockades which intersects with guideline and near me!!!!!");
                        world.getPlatoonAgent().sendClearAct(world.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
                    } else {
//                        world.printData("Move to point to clear blockades in way....");
                        world.getPlatoonAgent().sendMoveAct(world.getTime(), thisRoadPath, (int) nearestIntersect.getX(), (int) nearestIntersect.getY());
                    }
                }
            }

            lastClearLine = null;
        }

        result = ActResult.FAILED;
        return result;
    }

    @Override
    public void clearAroundTarget(Pair<Integer, Integer> targetLocation) throws CommandException {
        throw new NotImplementedException();
    }

    private PositionActState moveToGuideLine(List<Area> areasSeenInPath, GuideLine guideLine) throws CommandException {

        int distanceThreshold = world.getClearRadius() / 3;
        Point2D betterPosition = getBetterPosition(guideLine, distanceThreshold);
        Point2D agentPosition = Util.getPoint(world.getSelfLocation());
        if (betterPosition == null) {
            return PositionActState.IN_GUIDELINE;
        }


        Line2D line2D = new Line2D(agentPosition, betterPosition);

        Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(agentPosition, line2D.getEndPoint(), world.getClearRadius()), world.getClearRadius());
        double distance = (world.getClearRadius() - MRLConstants.AGENT_SIZE * 0.5);
        Pair<Line2D, Line2D> clearSecureLines = clearTools.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), world.getClearRadius(), distance);
        boolean shouldClear = anyBlockadeIntersection(areasSeenInPath, line2D, false) ||
                anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), false) ||
                anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), false);

        if (Util.lineLength(line2D) > clearRange) {
            world.printData("i have too much distance to guideline......");

            //todo should implement
            clearToPoint(agentPosition, line2D.getEndPoint());
        }
        if (shouldClear) {
            Line2D clearLine = Util.improveLine(line2D, world.getClearRadius());
            world.getPlatoonAgent().sendClearAct(world.getTime(), (int) clearLine.getEndPoint().getX(), (int) clearLine.getEndPoint().getY());
            return PositionActState.CLEAR_TO_GUIDELINE;
        }

        if (!isNeedToClear(agentPosition, areasSeenInPath, getClearLine(agentPosition, guideLine, clearRange))) {
            return PositionActState.KEEP_MOVING;
        }


        List<EntityID> path = new ArrayList<EntityID>(1);
        path.add(world.getSelfPosition().getID());
        world.getPlatoonAgent().sendMoveAct(world.getTime(), path, (int) line2D.getEndPoint().getX(), (int) line2D.getEndPoint().getY());
        return PositionActState.MOVE_TO_GUIDELINE;
    }

    private void clearToPoint(Point2D agentPosition, Point2D point) throws CommandException {
        double distance = Util.distance(agentPosition, point);
        Line2D clearLine = null, targetClearLine;
        distance = Math.min(distance, world.getClearDistance());
        distance -= SECURE_RANGE;
        clearLine = Util.clipLine(new Line2D(agentPosition, point), distance);
        targetClearLine = Util.clipLine(clearLine, distance - world.getClearRadius());

        List<Area> areasSeenInPath = new ArrayList<Area>();

        Area selfPosition = (Area) world.getSelfPosition();
        if (selfPosition.getShape().contains(point.getX(), point.getY())) {
            areasSeenInPath.add(selfPosition);
        } else {
            areasSeenInPath.addAll(world.getRoadsSeen());
        }
        if (anyBlockadeIntersection(areasSeenInPath, targetClearLine, true)) {
            Point2D endPoint = targetClearLine.getEndPoint();
            world.getPlatoonAgent().sendClearAct(world.getTime(), (int) endPoint.getX(), (int) endPoint.getY());
        } else {
            ArrayList<EntityID> path = new ArrayList<EntityID>();
            path.add(world.getSelfPosition().getID());
            world.getPlatoonAgent().sendMoveAct(world.getTime(), path, (int) point.getX(), (int) point.getY());
        }

    }

    private Point2D getBetterPosition(Line2D guideline, double distanceThreshold) {
        Point2D agentLocation = Util.getPoint(world.getSelfLocation());
        Point2D betterPosition = null;
        Point2D pointOnGuideline = Util.closestPoint(guideline, agentLocation);

        Area selfPosition = (Area) world.getSelfPosition();
        if (!selfPosition.getShape().contains(pointOnGuideline.getX(), pointOnGuideline.getY())) {
            List<Point> pointList = Util.getPointList(selfPosition.getApexList());
            Line2D line;
            Point2D p1, p2, nearestPoint = null;
            Point point;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < pointList.size() ; i++) {
                point = pointList.get(i);
                p1 = new Point2D(point.getX(), point.getY());
                point = pointList.get((i + 1)%pointList.size());
                p2 = new Point2D(point.getX(), point.getY());
                line = new Line2D(p1, p2);
                Point2D intersection = Util.getIntersection(line, guideline);
                int distance = Util.distance(agentLocation, intersection);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestPoint = intersection;
                }
            }

            betterPosition = nearestPoint;
        } else {
            betterPosition = pointOnGuideline;
        }

        if (betterPosition == null || Util.distance(betterPosition, agentLocation) < distanceThreshold) {
            //it means guideline is too close to me. so no need to move on it
            return null;
        }

        return betterPosition;
    }

    GuidelineProvider guidelineProvider = new GuidelineProvider(world, clearRange);

    private GuideLine getTargetGuideline(List<EntityID> path, double range, EntityID targetID) {
        return guidelineProvider.findTargetGuideline(path, range, targetID);
    }

    private Line2D getTargetClearLine(List<Area> areasSeenInPath, double range, GuideLine guideLine) {
        Point2D agentPosition = Util.getPoint(world.getSelfLocation());

        if (!isNeedToClear(agentPosition, areasSeenInPath, guideLine)) {
            return null;
        }
        return getClearLine(agentPosition, guideLine, range);
    }

    private boolean isNeedToClear(Point2D agentLocation, List<Area> areasSeenInPath, Line2D guideLine) {

        if (guideLine == null) {
            return false;
        }

        if (Util.distance(guideLine, agentLocation) > MRLConstants.AGENT_SIZE) {
            return true;
        }
//        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        for (Area area : areasSeenInPath) {
            if (area.isBlockadesDefined()) {
                for (EntityID blockID : area.getBlockades()) {
                    Blockade blockade = world.getEntity(blockID, Blockade.class);
                    if (blockade != null) {
                        Polygon blockadePoly = Util.getPolygon(blockade.getApexes());
                        if (!Util.isPassable(blockadePoly, guideLine, MRLConstants.AGENT_PASSING_THRESHOLD)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Line2D getClearLine(Point2D agentPosition, GuideLine guideLine, double range) {
        Line2D targetLine = new Line2D(agentPosition, guideLine.getDirection());

        ////////////////////////////////////////////////////////////////////
        //rotate target line for containing traffic simulator move points //
//        Polygon clearRectangle = Util.clearAreaRectangle(targetLine.getOrigin(), targetLine.getEndPoint(), world.getClearRadius());
        ////////////////////////////////////////////////////////////////////


        return Util.clipLine(targetLine, range - SECURE_RANGE);
    }


    /**
     * @param path
     * @return
     */
    private List<Area> getAreasSeenInPath(List<EntityID> path) {
        List<Area> areasSeenInPath = new ArrayList<Area>();
        Area area;
        for (EntityID id : path) {
            area = world.getEntity(id, Area.class);
            if (world.getChanges().contains(id)) {
                areasSeenInPath.add(area);
            } else {
                break;
            }
        }
        return areasSeenInPath;
    }


    private Point2D getTargetClearPoint(List<EntityID> path, double range) {
        if (path == null || range < 0) {
            return null;
        }
//        final double minimumRangeThreshold = range * clearRangeYieldCoefficient;

        Area area;
        Point2D targetPoint = null;
        Point2D positionPoint = Util.getPoint(world.getSelfLocation());
        if (path.size() <= 1) {
            area = world.getEntity(world.getSelfPosition().getID(), Area.class);
            Point2D areaCenterPoint = Util.getPoint(area.getLocation(world));
            targetPoint = Util.clipLine(new Line2D(positionPoint, areaCenterPoint), range).getEndPoint();
        } else if (path.size() > 1) {
            area = world.getEntity(path.get(0), Area.class);
            Edge edge = area.getEdgeTo(path.get(1));
            if (edge == null) {
                return null;
            }
            Point2D areaCenterPoint = Util.getPoint(area.getLocation(world));
            Point2D edgeCenterPoint = Util.getMiddle(edge.getLine());
            Point2D targetPoint2D = Util.clipLine(new Line2D(areaCenterPoint, edgeCenterPoint), range).getEndPoint();
//            targetPoint = Util.clipLine(new Line2D(positionPoint, edgeCenterPoint), range).getEndPoint();
            //guideline is the line from agent location area center toward edge to next area
            //to avoid bad shape clearing, guideline help pfs to clear in one direction in path
            double deltaX = positionPoint.getX() - areaCenterPoint.getX();
            double deltaY = positionPoint.getY() - areaCenterPoint.getY();
//            if(Util.contains())
            targetPoint2D = new Point2D(targetPoint2D.getX() + deltaX, targetPoint2D.getY() + deltaY);//rotate line to set it as parallel of guideline
            Polygon clearPoly = Util.clearAreaRectangle(positionPoint, targetPoint2D, world.getClearRadius());
            if (clearPoly.contains(edgeCenterPoint.getX(), edgeCenterPoint.getY())) {
                targetPoint = Util.clipLine(new Line2D(positionPoint, targetPoint2D), range).getEndPoint();
            } else {
                targetPoint = Util.clipLine(new Line2D(positionPoint, edgeCenterPoint), range).getEndPoint();
            }
        }

        List<Area> areasSeenInPath = getAreasSeenInPath(path);
        //target point is point that agent want to clear up to it.
//        List<EntityID> checkedAreas = new ArrayList<EntityID>();
//        Polygon polygon = null;
        if (targetPoint != null) {
            Line2D targetLine = new Line2D(positionPoint, targetPoint);
//            polygon = Util.clearAreaRectangle(positionPoint.getX(), positionPoint.getY(), targetPoint.getX(), targetPoint.getY(), world.getClearRadius());
//            Area neighbour;
//            cleaningBefore = false;
//            for (Road road : roadsSeenInPath) {//3 loop
//                for (EntityID id : road.getNeighboursByEdge()) {
//                    if (checkedAreas.contains(id) || path.contains(id)) {
//                        //this area checked before...
//                        continue;
//                    }
//                    checkedAreas.add(id);
//                    neighbour = world.getEntity(id, Area.class);
//                    if (!(neighbour instanceof Road)) {
//                        //this area is not road! so no blockades is in it.
//                        continue;
//                    }
//                    targetLine = normalizeClearLine(neighbour, targetLine, polygon, minimumRangeThreshold);
//                    if (targetLine == null) {
//                        return null;
//                    }
//                }
//            }
            Pair<Line2D, Line2D> clearLengthLines = Util.clearLengthLines(Util.clearAreaRectangle(Util.getPoint(world.getSelfLocation()), targetLine.getEndPoint(), world.getClearRadius()), world.getClearRadius());
            double distance = (world.getClearRadius() - MRLConstants.AGENT_SIZE * 0.5) / 2;

            Pair<Line2D, Line2D> clearSecureLines = clearTools.getClearSecureLines(clearLengthLines.first(), clearLengthLines.second(), world.getClearRadius(), distance);
            MrlPersonalData.VIEWER_DATA.setPFClearAreaLines(world.getSelf().getID(), targetLine, clearSecureLines.first(), clearSecureLines.second());
            if (anyBlockadeIntersection(areasSeenInPath, targetLine, true) ||
                    anyBlockadeIntersection(areasSeenInPath, clearSecureLines.first(), false) ||
                    anyBlockadeIntersection(areasSeenInPath, clearSecureLines.second(), false)) {
                return targetLine.getEndPoint();
            } else {
//                return beforeClearPoint(targetLine);
            }
        }
        return null;
    }


    private Point2D anyBlockadeIntersection(GuideLine guideLine) {
        List<Area> areas = new ArrayList<Area>();
        for (StandardEntity entity : world.getEntities(guideLine.getAreas())) {
            areas.add((Area) entity);
        }
        Point2D nearestPoint = null;
        Point2D agentLocation = Util.getPoint(world.getSelfLocation());
        int minDist = Integer.MAX_VALUE;
        for (Area area : areas) {

            List<Point2D> intersects = clearTools.blockadesIntersections(area, guideLine);
            for (Point2D point2D : intersects) {
                int dist = Util.distance(point2D, agentLocation);
                if (dist < minDist) {
                    minDist = dist;
                    nearestPoint = point2D;
                }
            }
        }
        return nearestPoint;
    }

    private boolean anyBlockadeIntersection(Collection<Area> areasSeenInPath, Line2D targetLine, boolean secure) {
        Line2D line;
        if (secure) {
            double length = Util.lineLength(targetLine);
            double secureSize = 510 + SECURE_RANGE;
            if (length - secureSize <= 0) {
                world.printData("The clear line is too short.....");
                return false;
            }
            line = Util.improveLine(targetLine, -secureSize);
        } else {
            line = targetLine;
        }
        for (Area area : areasSeenInPath) {
            if (clearTools.anyBlockadeIntersection(area, line)) {
                return true;
            }
        }
        return false;
    }

    private enum PositionActState {
        MOVE_TO_GUIDELINE,
        IN_GUIDELINE,
        CLEAR_TO_GUIDELINE,
        KEEP_MOVING
    }

}
