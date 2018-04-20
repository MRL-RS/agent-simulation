package mrl.mrlPersonal;

import javolution.util.FastSet;
import mrl.common.clustering.ConvexObject;
import mrl.common.clustering.FireCluster;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.partitioning.Partition;
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
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

public class EmptyViewerData implements IViewerData {

    @Override
    public MrlViewer viewerConstructor() {
        return null;
    }

    @Override
    public void addAmbulanceTarget(EntityID id, EntityID target) {

    }

    @Override
    public void setRescueRange(int rescueRange) {

    }

    @Override
    public void setHighValueBuildings(EntityID id, List<MrlBuilding> highValueBuildings) {

    }

    @Override
    public void setCenterPoint(EntityID id, Pair<Point, ConvexObject> pointConvexObjectPair) {

    }

    @Override
    public void setBuildingValue(EntityID id, double val) {

    }

    @Override
    public void setIgnoredBorderBuildings(EntityID id, List<StandardEntity> ignoredBorderBuildings) {

    }

    @Override
    public void setFireClusterCondition(EntityID id, FireCluster fireCluster) {

    }

    @Override
    public void setTargetPoint(EntityID id, ArrayList<Pair<Point, Double>> pairs) {

    }

    @Override
    public void setDirectionPolygons(EntityID id, List<DirectionObject> polygons) {

    }

    @Override
    public void setConvexIntersectLines(ConvexObject convexObject, Set<Line2D> lines) {

    }

    @Override
    public void setDirectionPolygon(ConvexObject convexObject, Polygon directionPolygon) {

    }

    @Override
    public void setConvexCenterPoint(ConvexObject convexObject, Point point) {

    }

    @Override
    public void setExtinguishRange(int maxExtinguishDistance) {

    }

    @Override
    public void setConnectedBuildings(EntityID id, List<EntityID> bIDs) {

    }

    @Override
    public void setBorderMapBuildings(EntityID id, Set<EntityID> borderBuildings) {

    }

    @Override
    public void setExploreBuildings(EntityID id, FastSet<MrlBuilding> mrlBuildings) {

    }

    @Override
    public void setBestPlaceToStand(EntityID id, Collection<StandardEntity> extinguishableFromAreas) {

    }

    @Override
    public void setStand(EntityID id, StandardEntity locationToGo) {

    }

    @Override
    public void setForbiddenLocations(EntityID id, List<EntityID> forbiddenLocations) {

    }

    @Override
    public void print(String s) {

    }

    @Override
    public void setMyTarget(EntityID id, Building selfBuilding) {

    }

    @Override
    public void setBorderDirectionBuildings(EntityID id, List<StandardEntity> borderDirectionBuildings) {

    }

    @Override
    public void setFireBrigadeData(EntityID id, MrlBuilding targetBuilding) {

    }

    @Override
    public void setBuildingValues(EntityID id, List<MrlBuilding> mrlBuildings) {

    }

    @Override
    public void setAreaVisibility(List<MrlRoad> mrlRoads, List<MrlBuilding> mrlBuildings) {

    }

    @Override
    public void setExtinguishData(Map<EntityID, List<EntityID>> extinguishableFromAreasMap, Map<EntityID, List<MrlBuilding>> buildingsInExtinguishRangeMap) {

    }

    @Override
    public void setPartitions(EntityID id, List<Partition> partitions, Partition humanPartition, Set<Partition> humanPartitionsMap) {

    }

    @Override
    public void setPartitionsData(EntityID id, List<Partition> partitions, Map<Human, Set<Partition>> humanPartitionsMap) {

    }

    @Override
    public void setLayerPut(EntityID id, ArrayList[] clusters, List<Point> clusterCenterPoints) {

    }

    @Override
    public void setPlatoonAgents(EntityID id, MrlPlatoonAgent tMrlPlatoonAgent) {

    }

    @Override
    public void setUnreachableBuildings(EntityID id, HashSet<EntityID> entityIDs) {

    }

    @Override
    public void setCivilianData(EntityID id, Set<EntityID> shouldDiscoverBuildings, EntityID civilianInProgress, EntityID buildingInProgress) {

    }

    @Override
    public void removeExplorePosition(EntityID id, StandardEntity entity) {

    }

    @Override
    public void setPossibleBuildings(EntityID id, List<EntityID> possibleBuildings) {

    }

    @Override
    public void setEmptyBuildings(EntityID id, List<EntityID> emptyBuildings) {

    }

    @Override
    public void setShouldCheckInsideBuildings(List<MrlBuilding> shouldCheckInsideBuildings) {

    }

    @Override
    public void setExplorePositions(EntityID id, Set<EntityID> areas, MrlWorld world) {

    }

    @Override
    public void setActionData(EntityID id, MrlZone zone) {

    }

    @Override
    public void setTargetsToGo(EntityID id, Task myTask) {

    }

    @Override
    public void setHeardPositions(EntityID civID, Pair<Integer, Integer> selfLocation) {

    }

    @Override
    public void setMrlBuildingsMap(MrlBuilding b) {

    }

    @Override
    public void setViewerBuildingsMap(EntityID id, List<MrlBuilding> mrlBuildings) {

    }

    @Override
    public void setViewRoadsMap(EntityID id, List<MrlRoad> mrlRoads) {

    }

    @Override
    public void setPathList(Paths paths) {

    }

    @Override
    public void setZones(MrlZones zones) {

    }

    @Override
    public void updateThisCycleData(MrlPlatoonAgent platoonAgent, MrlWorld world) {

    }

    @Override
    public void setBlockedBuildings(MrlPlatoonAgent platoonAgent, EntityID buildingId, boolean reachable) {

    }

    @Override
    public void setMrlRoadMap(MrlPlatoonAgent platoonAgent, EntityID id, MrlRoad mrlRoad) {

    }

    @Override
    public void setGraphEdges(MrlPlatoonAgent platoonAgent, EntityID id, ArrayList<MyEdge> thisAreaMyEdges) {

    }

    @Override
    public void setAllGrids(EntityID id, List<Grid> grids) {

    }

    @Override
    public void removeAreaGrids(EntityID id) {

    }

    @Override
    public void setBuildingAdvantageRatio(EntityID id, double advantageRatio) {

    }

    @Override
    public void setPFGuideline(EntityID id, GuideLine guideLine) {

    }

    @Override
    public void setPFClearAreaLines(EntityID id, rescuecore2.misc.geometry.Line2D targetLine, rescuecore2.misc.geometry.Line2D first, rescuecore2.misc.geometry.Line2D second) {

    }

    @Override
    public void setScaledBlockadeData(EntityID id, List<StandardEntity> obstacles, Polygon scaledBlockades, Map<rescuecore2.misc.geometry.Line2D, List<Point2D>> freePoints, List<Point2D> targetPoint) {

    }

    public void setObstacleBounds(EntityID id, List<rescuecore2.misc.geometry.Line2D> boundLines){

    }

    @Override
    public void setAllPartitionsMapData(EntityID id, Map<EntityID, Partition> partitionMap) {

    }

    @Override
    public void setPoliceFireTasksData(EntityID id, Partition nearestBurningPartition, Partition neighbourPartition) {

    }

    @Override
    public void printData(MrlPlatoonAgent platoonAgent, int time, String data) {

    }
}
