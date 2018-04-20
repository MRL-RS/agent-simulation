package mrl.police;

import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.helper.RoadHelper;
import mrl.partitioning.Partition;
import mrl.police.clear.ClearTools;
import mrl.police.moa.Importance;
import mrl.police.moa.PoliceForceUtilities;
import mrl.police.moa.Target;
import mrl.task.Task;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.CheckAreaPassable;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 2/7/12
 * Time: 3:02 PM
 */
public class PoliceConditionChecker {

    private MrlWorld world;
    private PoliceForceUtilities utilities;
    private boolean isFirstTime;

    private int numberOfHealthyPoliceForces;
    private int numberOfHealthyCoworkers;
    private RoadHelper roadHelper;
    //    private ClearHelper clearHelper;
    private int minTimeToSplitPartitions = 10;
    private PoliceMessageHelper policeMessageHelper;
    private ClearTools clearTools;

    public PoliceConditionChecker(MrlWorld world, PoliceForceUtilities utilities, PoliceMessageHelper policeMessageHelper) {
        this.world = world;
        this.utilities = utilities;
        this.isFirstTime = true;
        this.policeMessageHelper = policeMessageHelper;
//        clearHelper = new ClearHelper(world);
        roadHelper = world.getHelper(RoadHelper.class);
        numberOfHealthyPoliceForces = 0;
        clearTools = new ClearTools(this.world);
    }


    /**
     * Is target reachable from a way?
     *
     * @param task the task that should be examined
     * @return true if target is reachable, otherwise false
     */
    public boolean isTaskDone(Task task) {

        if (task == null) {
            return false;
        }

        boolean cleared;
        cleared = isReachable(task.getTarget());

        return cleared;

    }

    private boolean isReachable(Target target) {


       List<EntityID> pathToGo;
        if (target.getImportanceType().equals(Importance.FIERY_BUILDING_1) || target.getImportanceType().equals(Importance.FIERY_BUILDING_2) || target.getImportanceType().equals(Importance.FIERY_BUILDING_3)) {
            pathToGo = new ArrayList<EntityID>(world.getPlatoonAgent().getPathPlanner().planMove((Area) world.getSelfPosition(), (Area) world.getEntity(target.getNearestRoadID()), 0, true));
        } else {
//            pathToGo = new ArrayList<EntityID>(world.getPlatoonAgent().getPathPlanner().planMove((Area) world.getSelfPosition(), utilities.getNearestRoad(target.getRoadsToMove()), 0, true));
            pathToGo = new ArrayList<EntityID>(world.getPlatoonAgent().getPathPlanner().planMove((Area) world.getSelfPosition(), (Area)world.getEntity(target.getPositionID()), 0, true));
        }
        StandardEntity entity;
        boolean isThereUnknownRoad = false;
        StandardEntity targetEntity = world.getEntity(target.getId());
        if (!pathToGo.isEmpty()) {
            for (EntityID areaID : pathToGo) {
                entity = world.getEntity(areaID);
                if (entity instanceof Road) {

                    if (!((Road) entity).isBlockadesDefined()) {
                        isThereUnknownRoad = true;
                        break;
                    }
                }
            }
        } else if (targetEntity instanceof Human) {
            if (world.getChanges().contains(target.getId())) {
                Human human = (Human) targetEntity;
                if (human.getPosition(world) instanceof Area) {
                    if (world.getSelfPosition().equals(human.getPosition(world))) {
                        pathToGo.add(world.getSelfPosition().getID());
                    } else {
                        pathToGo = world.getPlatoonAgent().getPathPlanner().planMove((Area) world.getSelfPosition(), (Area) human.getPosition(world), 0, true);
                    }
                }
            }
        } else if (targetEntity instanceof Building) {
            Building building = (Building) targetEntity;
            EntityID positionID = world.getSelfPosition().getID();
            if (building.getNeighbours().contains(positionID)) {
                pathToGo.add(positionID);
                pathToGo.add(building.getID());
            }
        }

        if (isThereUnknownRoad) {
            return false;
        } else {


            return !clearTools.shouldCheckForBlockadesOnWay(pathToGo, target.getId());
        }

    }


    private static class LineInfo {
        private Line2D line;
        private StandardEntity entity;
        private boolean blocking;

        public LineInfo(Line2D line, StandardEntity entity, boolean blocking) {
            this.line = line;
            this.entity = entity;
            this.blocking = blocking;
        }

        public Line2D getLine() {
            return line;
        }

        public StandardEntity getEntity() {
            return entity;
        }

        public boolean isBlocking() {
            return blocking;
        }
    }

    private Collection<LineInfo> getAllLines(Collection<StandardEntity> entities) {
        Collection<LineInfo> result = new HashSet<LineInfo>();
        for (StandardEntity next : entities) {
//            if (next instanceof Building) {
//                for (Edge edge : ((Building)next).getEdges()) {
//                    Line2D line = edge.getLine();
//                    result.add(new LineInfo(line, next, !edge.isPassable()));
//                }
//            }
            if (next instanceof Road) {
                for (Edge edge : ((Road) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, false));
                }
            } else if (next instanceof Blockade) {
                int[] apexes = ((Blockade) next).getApexes();
                List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
                List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
                for (Line2D line : lines) {
                    result.add(new LineInfo(line, next, false));
                }
            } else {
                continue;
            }
        }
        return result;
    }

    private Collection<LineInfo> getAllLines(StandardEntity entity) {
        Collection<LineInfo> result = new HashSet<LineInfo>();
//            if (next instanceof Building) {
//                for (Edge edge : ((Building)next).getEdges()) {
//                    Line2D line = edge.getLine();
//                    result.add(new LineInfo(line, next, !edge.isPassable()));
//                }
//            }
        if (entity instanceof Road) {
            for (Edge edge : ((Road) entity).getEdges()) {
                Line2D line = edge.getLine();
                result.add(new LineInfo(line, entity, false));
            }
        } else if (entity instanceof Blockade) {
            int[] apexes = ((Blockade) entity).getApexes();
            List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
            List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
            for (Line2D line : lines) {
                result.add(new LineInfo(line, entity, false));
            }
        }
        return result;
    }


    private boolean checkPassable(Task task) {
        //        Blockade blockade=clearHelper.getBlockadeToClear(task.getTarget(), task.getActionStyle());
        CheckAreaPassable checkAreaPassable = world.getPlatoonAgent().getPathPlanner().getAreaPassably();
//        ClearedRoadMessage clearedRoadMessage;
        List<EntityID> blockades;
        Road road;
        boolean isTargetDone = true;
        for (EntityID targetID : task.getTarget().getRoadsToMove().keySet()) {
            road = (Road) world.getEntity(targetID);
            blockades = checkAreaPassable.policeCheckPassably(road, null);
            if (roadHelper.isPassable(targetID)) {
                if (road.isBlockadesDefined() && road.getBlockades().isEmpty()) {
                    continue;
                } else {
                    isTargetDone = false;
                }
            } else if (!roadHelper.isPassable(targetID) || blockades != null && !blockades.isEmpty()) {
                isTargetDone = false;
            } else {
                // this road is passable
                roadHelper.setRoadPassable(targetID, true);
                task.getTarget().getRoadsToMove().put(targetID, true);
                policeMessageHelper.sendClearedRoadMessage(targetID);
            }
        }
        return isTargetDone;
    }

    private boolean amIOnTarget(Target target) {
        return world.getSelfPosition().getID().equals(target.getNearestRoadID());
    }

    public boolean isTimeToPartitionAssignment() {

        if (world.getTime() >= 12 && (isFirstTime || isNumberOfHealthyPolicesChanged())) {
            if (isFirstTime) {
                isFirstTime = false;
                numberOfHealthyPoliceForces = utilities.getHealthyPoliceForces().size();
                if (numberOfHealthyPoliceForces == 0) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }

    }

    public boolean isTimeToPartitionSplitting(Partition myOriginalPartition, Map<Human, Partition> humanPartitionMap) {

        if (world.getTime() < minTimeToSplitPartitions) {
            return false;
        }

        int i = 0;
        for (StandardEntity entity : utilities.getHealthyCoworkers()) {
            if (!entity.getID().equals(world.getSelf().getID()) && humanPartitionMap.get(entity).equals(myOriginalPartition)) {
                i++;
            }

        }
        return utilities.getHealthyCoworkers().contains(world.getSelfHuman()) && i > 0;
    }

    private boolean isNumberOfHealthyPolicesChanged() {

        if (utilities.getHealthyPoliceForces().size() == 0) {
            return false;
        }

        if (numberOfHealthyPoliceForces != utilities.getHealthyPoliceForces().size()) {
            numberOfHealthyPoliceForces = utilities.getHealthyPoliceForces().size();
            return true;
        } else {
            return false;
        }

    }


    public boolean isInBadBuilding() {
        if (world.getSelfPosition() instanceof Building) {
            Building building = (Building) world.getSelfPosition();
            if (building.isTemperatureDefined() && building.getTemperature() > 0) {
                return true;
            }
        }
        return false;
    }
}
