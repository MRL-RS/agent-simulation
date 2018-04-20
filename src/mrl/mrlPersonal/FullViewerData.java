package mrl.mrlPersonal;

import javolution.util.FastSet;
import mrl.common.Util;
import mrl.common.clustering.ConvexObject;
import mrl.common.clustering.FireCluster;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.mrlPersonal.viewer.layers.*;
import mrl.partitioning.Partition;
import mrl.partitioning.voronoi.ArraySet;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.clear.GuideLine;
import mrl.task.Task;
import mrl.world.MrlWorld;
import mrl.world.object.DirectionObject;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.object.mrlZoneEntity.MrlZones;
import mrl.world.routing.graph.MyEdge;
import mrl.world.routing.grid.Grid;
import mrl.world.routing.path.Paths;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Created by Rescue Agent on 6/20/14.
 */
public class FullViewerData implements IViewerData {
    @Override
    public MrlViewer viewerConstructor() {
        return new MrlViewer();
    }

    @Override
    public void addAmbulanceTarget(EntityID id, EntityID target) {
        MrlHumanLayer.AMBULANCE_TARGETS_TO_GO_MAP.put(id, target);
    }

    @Override
    public void setRescueRange(int rescueRange) {
        MrlHumanLayer.RESCUE_RANGE = rescueRange;
    }

    @Override
    public void setHighValueBuildings(EntityID id, List<MrlBuilding> highValueBuildings) {
        MrlConvexHullLayer.HIGH_VALUE_BUILDINGS.put(id, highValueBuildings);
    }

    @Override
    public void setCenterPoint(EntityID id, Pair<Point, ConvexObject> pointConvexObjectPair) {
        MrlConvexHullLayer.CENTER_POINT.put(id, pointConvexObjectPair);
    }

    @Override
    public void setBuildingValue(EntityID id, double val) {
        MrlBuildingLayer.BUILDING_VALUE.put(id, val);
    }

    @Override
    public void setIgnoredBorderBuildings(EntityID id, List<StandardEntity> ignoredBorderBuildings) {
        MrlConvexHullLayer.IGNORED_BORDER_BUILDINGS.put(id, ignoredBorderBuildings);
    }

    @Override
    public void setFireClusterCondition(EntityID id, FireCluster fireCluster) {
        List<Pair<Point2D, String>> clusterConditions = new ArrayList<Pair<Point2D, String>>();
        clusterConditions.add(new Pair<Point2D, String>(fireCluster.getCenter(), fireCluster.getCondition().toString()));
        MrlConvexHullLayer.FIRE_CLUSTER_CONDITIONS.put(id, clusterConditions);
    }

    @Override
    public void setTargetPoint(EntityID id, ArrayList<Pair<Point, Double>> pairs) {
        MrlTargetPointsLayer.TARGET_POINTS.put(id, pairs);
    }

    @Override
    public void setDirectionPolygons(EntityID id, List<DirectionObject> polygons) {
        MrlConvexHullLayer.DIRECTION_POLYGONS.put(id, polygons);
    }

    @Override
    public void setConvexIntersectLines(ConvexObject convexObject, Set<Line2D> lines) {
        convexObject.CONVEX_INTERSECT_LINES = lines;
    }

    @Override
    public void setDirectionPolygon(ConvexObject convexObject, Polygon directionPolygon) {
        convexObject.DIRECTION_POLYGON = directionPolygon;
    }

    @Override
    public void setConvexCenterPoint(ConvexObject convexObject, Point point) {
        convexObject.CENTER_POINT = point;
    }

    @Override
    public void setExtinguishRange(int maxExtinguishDistance) {
        MrlConvexHullLayer.EXTINGUISH_RANGE = maxExtinguishDistance;
    }

    @Override
    public void setConnectedBuildings(EntityID id, List<EntityID> bIDs) {
        MrlConnectedBuildingsLayer.CONNECTED_BUILDINGS.put(id, bIDs);
    }

    @Override
    public void setBorderMapBuildings(EntityID id, Set<EntityID> borderBuildings) {
        MrlConvexHullLayer.BORDER_MAP_BUILDINGS.put(id, borderBuildings);
    }

    @Override
    public void setExploreBuildings(EntityID id, FastSet<MrlBuilding> mrlBuildings) {
        MrlConvexHullLayer.EXPLORE_BUILDINGS.put(id, mrlBuildings);
    }

    @Override
    public void setBestPlaceToStand(EntityID id, Collection<StandardEntity> extinguishableFromAreas) {
        MrlConvexHullLayer.BEST_PLACE_TO_STAND.put(id, new HashSet<StandardEntity>(extinguishableFromAreas));
    }

    @Override
    public void setStand(EntityID id, StandardEntity locationToGo) {
        List<StandardEntity> stands = new ArrayList<StandardEntity>();
        stands.add(locationToGo);
        MrlConvexHullLayer.STAND.put(id, stands);
    }

    @Override
    public void setForbiddenLocations(EntityID id, List<EntityID> forbiddenLocations) {
        MrlForbiddenLocationsLayer.FORBIDDEN_LOCATIONS.put(id, forbiddenLocations);
    }

    @Override
    public void print(String s) {
        System.out.println(s);
    }

    @Override
    public void setMyTarget(EntityID id, Building selfBuilding) {
//        MrlConvexHullLayer.MY_TARGET.remove(id);
        List<Building> target = new ArrayList<Building>();
        target.add(selfBuilding);
        MrlConvexHullLayer.MY_TARGET.put(id, target);
    }

    @Override
    public void setBorderDirectionBuildings(EntityID id, List<StandardEntity> borderDirectionBuildings) {
        MrlConvexHullLayer.BORDER_DIRECTION_BUILDINGS.put(id, borderDirectionBuildings);
    }

    @Override
    public void setFireBrigadeData(EntityID id, MrlBuilding targetBuilding) {
        if (targetBuilding != null) {

            MrlHumanLayer.TARGETS_TO_GO_MAP.put(id, targetBuilding.getID());
            MrlHumanLayer.FIRE_BRIGADE_TARGETS_TO_GO_MAP.put(id, targetBuilding.getID());

            MrlConvexHullLayer.MY_TARGET.remove(id);
            List<Building> buildings = new ArrayList<Building>();
            buildings.add(targetBuilding.getSelfBuilding());
            MrlConvexHullLayer.MY_TARGET.put(id, buildings);
        } else {
            MrlHumanLayer.TARGETS_TO_GO_MAP.put(id, null);
            MrlHumanLayer.FIRE_BRIGADE_TARGETS_TO_GO_MAP.put(id, null);

        }
    }

    @Override
    public void setBuildingValues(EntityID id, List<MrlBuilding> mrlBuildings) {
        MrlBuildingValuesLayer.BUILDING_VALUES.put(id, mrlBuildings);
    }

    @Override
    public void setAreaVisibility(List<MrlRoad> mrlRoads, List<MrlBuilding> mrlBuildings) {
        for (MrlRoad road : mrlRoads) {
            //fill visible areas from roads for viewer
            MrlAreaVisibilityLayer.VISIBLE_FROM_AREAS.put(road.getID(), road.getVisibleFrom());
            //fill observableAreas from roads for viewer
            MrlAreaVisibilityLayer.OBSERVABLE_AREAS.put(road.getID(), road.getObservableAreas());

            MrlAreaVisibilityLayer.LINE_OF_SIGHT_RAYS.put(road.getID(), road.getLineOfSight());
        }

        for (MrlBuilding building : mrlBuildings) {
            //fill visible areas from buildings for viewer
            MrlAreaVisibilityLayer.VISIBLE_FROM_AREAS.put(building.getID(), building.getVisibleFrom());
            //fill observable areas from buildings for viewer
            MrlAreaVisibilityLayer.OBSERVABLE_AREAS.put(building.getID(), building.getObservableAreas());

            MrlAreaVisibilityLayer.LINE_OF_SIGHT_RAYS.put(building.getID(), building.getLineOfSight());
        }
    }

    @Override
    public void setExtinguishData(Map<EntityID, List<EntityID>> extinguishableFromAreasMap, Map<EntityID, List<MrlBuilding>> buildingsInExtinguishRangeMap) {
        MrlExtinguishableFromLayer.EXTINGUISHABLE_FROM.putAll(extinguishableFromAreasMap);
        MrlExtinguishableFromLayer.BUILDINGS_IN_EXTINGUISH_RANGE.putAll(buildingsInExtinguishRangeMap);
    }

    @Override
    public void setPartitions(EntityID id, List<Partition> partitions, Partition humanPartition, Set<Partition> humanPartitionsMap) {
        for (Partition partition : partitions) {
            if (partition.getPolygon() == null) {
                partition.computeConvexHull();
            }
        }

        MrlClusterLayer.PARTITIONS_MAP.put(id, partitions);
        MrlClusterLayer.HUMAN_PARTITION_MAP.put(id, humanPartition);
        MrlClusterLayer.HUMAN_PARTITIONS_MAP.put(id, humanPartitionsMap);
        if (humanPartition == null) {
            if (humanPartition == null) {
                System.out.println(id + " I have no partitions in time :" + 0);
            } else {

                System.out.println(id + " my Partition is:" + humanPartition.getId()
                        + " NumberOfNeeded:" + humanPartition.getNumberOfNeededPFs());
            }
        }
        MrlSubClusterLayer.Partitions = partitions;
        MrlPartitionsLayer.PARTITIONS = partitions;
        MrlRendezvousLayer.partitions = partitions;
    }

    @Override
    public void setPartitionsData(EntityID id, List<Partition> partitions, Map<Human, Set<Partition>> humanPartitionsMap) {
        for (Partition partition : partitions) {
            if (partition.getPolygon() == null) {
                partition.computeConvexHull();
            }
        }


        MrlPoliceTargetClustersLayer.PARTITIONS_MAP.put(id, partitions);
        MrlPoliceTargetClustersLayer.HUMAN_PARTITIONS_MAP.put(id, humanPartitionsMap);
    }

    @Override
    public void setLayerPut(EntityID id, ArrayList[] clusters, List<Point> clusterCenterPoints) {
        List<List<Point>> points = new ArrayList<List<Point>>();

        for (ArrayList cluster : clusters) {
            List<Point> pointList = new ArrayList<Point>();
            for (Object aCluster : cluster) {
                Double[] elem = (Double[]) aCluster;
                pointList.add(new Point(elem[0].intValue(), elem[1].intValue()));
            }
            points.add(pointList);
        }

        MrlKmeansLayer.CENTER_POINTS.put(id, clusterCenterPoints);
        MrlKmeansLayer.COMMON_POINTS.put(id, points);
    }

    @Override
    public void setPlatoonAgents(EntityID id, MrlPlatoonAgent tMrlPlatoonAgent) {
        MrlViewer.PLATOON_AGENTS_FOR_VIEWER.put(id, tMrlPlatoonAgent);
    }

    @Override
    public void setUnreachableBuildings(EntityID id, HashSet<EntityID> entityIDs) {
        MrlBuildingLayer.UNREACHABLE_POSSIBLE_BUILDINGS.put(id, entityIDs);
    }

    @Override
    public void setCivilianData(EntityID id, Set<EntityID> shouldDiscoverBuildings, EntityID civilianInProgress, EntityID buildingInProgress) {
        if (!MrlBuildingLayer.POSSIBLE_BUILDINGS.containsKey(id)) {
            MrlBuildingLayer.POSSIBLE_BUILDINGS.put(id, new ArraySet<EntityID>());
            MrlViewer.AGENT_TARGET.put(id, "Civilian[" + civilianInProgress + "],Building[" + buildingInProgress + "]");
        }
        MrlBuildingLayer.POSSIBLE_BUILDINGS.put(id, new ArraySet<EntityID>(shouldDiscoverBuildings));
    }

    @Override
    public void removeExplorePosition(EntityID id, StandardEntity entity) {
        MrlConvexHullLayer.EXPLORE_POSITIONS.get(id).remove((Area) entity);
    }

    @Override
    public void setPossibleBuildings(EntityID id, List<EntityID> possibleBuildings) {
        try {
            if (!MrlBuildingLayer.POSSIBLE_BUILDINGS.containsKey(id)) {
                MrlBuildingLayer.POSSIBLE_BUILDINGS.put(id, new ArraySet<EntityID>());
            }
            MrlBuildingLayer.POSSIBLE_BUILDINGS.get(id).clear();
            MrlBuildingLayer.POSSIBLE_BUILDINGS.get(id).addAll(possibleBuildings);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setEmptyBuildings(EntityID id, List<EntityID> emptyBuildings) {
        try {
            if (!MrlBuildingLayer.EMPTY_BUILDINGS.containsKey(id)) {
                MrlBuildingLayer.EMPTY_BUILDINGS.put(id, new ArraySet<EntityID>());
            }
            MrlBuildingLayer.EMPTY_BUILDINGS.get(id).clear();
            MrlBuildingLayer.EMPTY_BUILDINGS.get(id).addAll(emptyBuildings);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setShouldCheckInsideBuildings(List<MrlBuilding> shouldCheckInsideBuildings) {
        MrlShouldCheckInsideBuildingsLayer.SHOULD_CHECK_INSIDE_BUILDINGS = new ArrayList<MrlBuilding>(shouldCheckInsideBuildings);
    }

    @Override
    public void setExplorePositions(EntityID id, Set<EntityID> areas, MrlWorld world) {
        Set<Area> ar = new FastSet<Area>();
        for (EntityID area : areas) {
            ar.add((Area) world.getEntity(area));
        }
        MrlConvexHullLayer.EXPLORE_POSITIONS.put(id, ar);
    }

    @Override
    public void setActionData(EntityID id, MrlZone zone) {
        MrlZIOLayer.SELECTED_ZONE_MAP.put(id, zone);
    }

    @Override
    public void setTargetsToGo(EntityID id, Task myTask) {
        if (myTask == null) {
            MrlHumanLayer.TARGETS_TO_GO_MAP.put(id, null);
        } else {
            MrlHumanLayer.TARGETS_TO_GO_MAP.put(id, myTask.getTarget().getPositionID());
        }
    }

    @Override
    public void setHeardPositions(EntityID civID, Pair<Integer, Integer> selfLocation) {
        if (!MrlHumanLayer.HEARD_POSITIONS.containsKey(civID)) {
            MrlHumanLayer.HEARD_POSITIONS.put(civID, new HashSet<Pair<Integer, Integer>>());
        }
        MrlHumanLayer.HEARD_POSITIONS.get(civID).add(selfLocation);
    }

    @Override
    public void setMrlBuildingsMap(MrlBuilding b) {
        MrlBuildingLayer.MRL_BUILDINGS_MAP.put(b.getID(), b);
    }

    @Override
    public void setViewerBuildingsMap(EntityID id, List<MrlBuilding> mrlBuildings) {
        Map<EntityID, MrlBuilding> buildingMap = new HashMap<EntityID, MrlBuilding>();
        for (MrlBuilding building1 : mrlBuildings) {
            buildingMap.put(building1.getID(), building1);
        }
        MrlViewer.VIEWER_BUILDINGS_MAP.put(id, buildingMap);
    }

    @Override
    public void setViewRoadsMap(EntityID id, List<MrlRoad> mrlRoads) {
        Map<EntityID, MrlRoad> roadMap = new HashMap<EntityID, MrlRoad>();
        for (MrlRoad road : mrlRoads) {
            roadMap.put(road.getID(), road);
        }
        MrlViewer.VIEWER_ROADS_MAP.put(id, roadMap);
    }

    @Override
    public void setPathList(Paths paths) {
        MrlPathLayer.PATHLIST = paths;
    }

    @Override
    public void setZones(MrlZones zones) {
        MrlZonePolygonLayer.ZONES = zones;
    }

    @Override
    public void updateThisCycleData(MrlPlatoonAgent platoonAgent, MrlWorld world) {
        if (platoonAgent == null) {
            return;
        }
        EntityID id = platoonAgent.getID();
        MrlHumanLayer.DEFINED_POSITION_CIVILIANS.put(id, new HashSet<EntityID>());
        MrlHumanLayer.UNDEFINED_POSITION_CIVILIANS.put(id, new HashSet<EntityID>());
        Civilian civilian;
        for (StandardEntity civ : world.getCivilians()) {
            civilian = (Civilian) civ;
            if (civilian.isPositionDefined()) {
                MrlHumanLayer.DEFINED_POSITION_CIVILIANS.get(id).add(civilian.getID());
            } else {
                MrlHumanLayer.UNDEFINED_POSITION_CIVILIANS.get(id).add(civilian.getID());
            }
        }

        try {
            MrlUnvisitedBuildingLayer.UNVISITED_BUILDINGS_MAP.put(id, world.getUnvisitedBuildings());
            MrlBuildingLayer.EMPTY_BUILDINGS.put(id, world.getEmptyBuildings());

//            MrlAmbulanceImprtantBuildingsLayer.PARTITION_VISITED_BUILDINGS_MAP.put(id, world.getViewerPartitionVisitedBuildings());
//            MrlAmbulanceImprtantBuildingsLayer.IS_MERGED_VISITED_BUILDINGS_MAP.put(id, world.getViewerIsMergedPartitionVisitedBuildings());
//            MrlAmbulanceImprtantBuildingsLayer.VICTIM_BUILDINGS_MAP.put(id, world.getViewerPartitionVictimBuildings());
            MrlUnvisitedFireBasedBuildingLayer.UNVISITED_FIRE_BASED_BUILDINGS_MAP.put(id, world.getBuildingSeen());
            MrlBurningBuildingLayer.BURNING_BUILDINGS_MAP.put(id, world.getMrlBuildings());
            MrlEstimatedLayer.BURNING_BUILDINGS_MAP.put(id, world.getMrlBuildings());
            MrlObjectsValueLayer.ZONE_VALUE_MAP.put(id, world.getZones());
            MrlObjectsValueLayer.VISITED_CIVILIAN_MAP.put(id, world.getCivilians());


            {//update Clusters
                List<FireCluster> convexObjectsForViewer = new ArrayList<FireCluster>();
                List<Polygon> bigBorderHulls = new ArrayList<Polygon>();
                List<Polygon> smallBorderHulls = new ArrayList<Polygon>();
                List<StandardEntity> borders = new ArrayList<StandardEntity>();

                MrlConvexHullLayer.BIG_BORDER_HULLS.clear();
                MrlConvexHullLayer.SMALL_BORDER_HULLS.clear();
                if (world.getFireClusterManager() != null) {
                    for (Object obj : world.getFireClusterManager().getClusters()) {
                        FireCluster cluster = (FireCluster) obj;
                        convexObjectsForViewer.add(cluster);
                        borders.addAll(cluster.getBorderEntities());
                        MrlConvexHullLayer.BORDER_BUILDINGS.put(world.getSelf().getID(), borders);
                        bigBorderHulls.add(cluster.getBigBorderPolygon());
                        smallBorderHulls.add(cluster.getSmallBorderPolygon());
                    }
                }
                MrlConvexHullLayer.CONVEX_HULLS_MAP.put(world.getSelf().getID(), convexObjectsForViewer);
                MrlConvexHullLayer.BIG_BORDER_HULLS.addAll(bigBorderHulls);
                MrlConvexHullLayer.SMALL_BORDER_HULLS.addAll(smallBorderHulls);
            }

//                getCivilianClusterManager().updateConvexHullsForViewer();
//                policeTargetClusterManager.updateConvexHullsForViewer();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void setBlockedBuildings(MrlPlatoonAgent platoonAgent, EntityID buildingId, boolean reachable) {
        try {
            if (platoonAgent != null) {
                if (!MrlBuildingLayer.BLOCKED_BUILDINGS.containsKey(platoonAgent.getID())) {
                    MrlBuildingLayer.BLOCKED_BUILDINGS.put(platoonAgent.getID(), new HashSet<EntityID>());
                }
                if (reachable) {
                    MrlBuildingLayer.BLOCKED_BUILDINGS.get(platoonAgent.getID()).remove(buildingId);
                } else {
                    MrlBuildingLayer.BLOCKED_BUILDINGS.get(platoonAgent.getID()).add(buildingId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMrlRoadMap(MrlPlatoonAgent platoonAgent, EntityID id, MrlRoad mrlRoad) {
        if (platoonAgent != null) {
            if (!MrlRoadLayer.MRL_ROADS_MAP.containsKey(platoonAgent.getID())) {
                MrlRoadLayer.MRL_ROADS_MAP.put(platoonAgent.getID(), new HashMap<EntityID, MrlRoad>());
            }
            MrlRoadLayer.MRL_ROADS_MAP.get(platoonAgent.getID()).put(id, mrlRoad);
        }
    }

    @Override
    public void setGraphEdges(MrlPlatoonAgent platoonAgent, EntityID id, ArrayList<MyEdge> thisAreaMyEdges) {
        if (platoonAgent != null) {
            if (!MrlGraphLayer.GRAPH_EDGES.containsKey(platoonAgent.getID())) {
                MrlGraphLayer.GRAPH_EDGES.put(platoonAgent.getID(), new HashMap<EntityID, ArrayList<MyEdge>>());
            }
            MrlGraphLayer.GRAPH_EDGES.get(platoonAgent.getID()).put(id, thisAreaMyEdges);
        }
    }

    @Override
    public void setAllGrids(EntityID id, List<Grid> grids) {
        MrlGridsLayer.ALL_GRIDS.put(id, grids);
    }

    @Override
    public void removeAreaGrids(EntityID id) {
        MrlGridsLayer.ALL_GRIDS.remove(id);
    }

    @Override
    public void setBuildingAdvantageRatio(EntityID id, double advantageRatio) {
        MrlAdvantageRatioLayer.BUILDING_ADVANTAGE_RATIO.put(id, advantageRatio);
    }

    @Override
    public void setPFGuideline(EntityID id, GuideLine guideLine) {
        MrlHumanLayer.AGENT_GUIDELINE_MAP.put(id, guideLine);
    }

    @Override
    public void setPFClearAreaLines(EntityID id, rescuecore2.misc.geometry.Line2D targetLine, rescuecore2.misc.geometry.Line2D first, rescuecore2.misc.geometry.Line2D second) {
        List<rescuecore2.misc.geometry.Line2D> lines = new ArrayList<rescuecore2.misc.geometry.Line2D>();
        lines.add(targetLine);
        lines.add(first);
        lines.add(second);
        MrlHumanLayer.AGENT_CLEARLINES_MAP.put(id, lines);
    }

    @Override
    public void setScaledBlockadeData(EntityID id, List<StandardEntity> obstacles, Polygon scaledBlockades, Map<rescuecore2.misc.geometry.Line2D, List<rescuecore2.misc.geometry.Point2D>> freePoints, List<rescuecore2.misc.geometry.Point2D> movePoints) {
        if (scaledBlockades != null) {
            MrlRayMoveLayer.SCALED_BLOCKADE_MAP.put(id, scaledBlockades);
        }
        List<Polygon> polygons = new ArrayList<>();
        if (obstacles != null) {
            for (StandardEntity entity : obstacles) {

                if (entity instanceof Blockade) {
                    Blockade blockade = (Blockade) entity;
                    polygons.add(Util.getPolygon(blockade.getApexes()));
                }

            }
            MrlRayMoveLayer.OBSTACLES_MAP.put(id, polygons);
        }

        if (freePoints != null) {
            List<rescuecore2.misc.geometry.Point2D> escapePoints = new ArrayList<>();
            for (Map.Entry<rescuecore2.misc.geometry.Line2D, List<rescuecore2.misc.geometry.Point2D>> entry : freePoints.entrySet()) {

                escapePoints.addAll(entry.getValue());
            }
            MrlRayMoveLayer.ESCAPE_POINTS_MAP.put(id, escapePoints);
            MrlRayMoveLayer.MOVE_POINTS_MAP.put(id, new ArrayList<>(movePoints));
        }

    }

    public void setObstacleBounds(EntityID id, List<rescuecore2.misc.geometry.Line2D> boundLines){
        if(id==null || boundLines==null){
            return;
        }
        MrlRayMoveLayer.BOUND_MAP.put(id,boundLines);
    }

    @Override
    public void setAllPartitionsMapData(EntityID id, Map<EntityID, Partition> partitionMap) {
        MrlClusterLayer.ALL_PARTITIONS_MAP.put(id, partitionMap);

    }

    public void setPoliceFireTasksData(EntityID id , Partition nearestBurningPartition , Partition neighbourPartition){
        MrlClusterLayer.PF_FIRE_TASK_PARTITION.put(id,new Pair<>(nearestBurningPartition,neighbourPartition));
    }

    @Override
    public void printData(MrlPlatoonAgent platoonAgent, int time, String data) {
        System.out.println("Time:" + time + " Me:" + platoonAgent + " \t- " + data);
    }
}
