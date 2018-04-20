package mrl.mrlPersonal;

import javolution.util.FastSet;
import mrl.common.clustering.ConvexObject;
import mrl.common.clustering.FireCluster;
import mrl.geometry.CompositeConvexHull;
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
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

public  interface IViewerData {
    public MrlViewer viewerConstructor();

    public void addAmbulanceTarget(EntityID id, EntityID target);

    public void setRescueRange(int rescueRange);

    public void setHighValueBuildings(EntityID id, List<MrlBuilding> highValueBuildings);

    public void setCenterPoint(EntityID id, Pair<Point, ConvexObject> pointConvexObjectPair);

    public void setBuildingValue(EntityID id, double val);

    public void setIgnoredBorderBuildings(EntityID id, List<StandardEntity> ignoredBorderBuildings);

    public void setFireClusterCondition(EntityID id, FireCluster fireCluster);

    public void setTargetPoint(EntityID id, ArrayList<Pair<Point, Double>> pairs);

    public void setDirectionPolygons(EntityID id, List<DirectionObject> polygons);

    public void setConvexIntersectLines(ConvexObject convexObject, Set<Line2D> lines);

    public void setDirectionPolygon(ConvexObject convexObject, Polygon directionPolygon);

    public void setConvexCenterPoint(ConvexObject convexObject, Point point);

    public void setExtinguishRange(int maxExtinguishDistance);

    public void setConnectedBuildings(EntityID id, List<EntityID> bIDs);

    public void setBorderMapBuildings(EntityID id, Set<EntityID> borderBuildings);

    public void setExploreBuildings(EntityID id, FastSet<MrlBuilding> mrlBuildings);

    public void setBestPlaceToStand(EntityID id, Collection<StandardEntity> extinguishableFromAreas);

    public void setStand(EntityID id, StandardEntity locationToGo) ;

    public void setForbiddenLocations(EntityID id, List<EntityID> forbiddenLocations) ;

    public void print(String s) ;

    public void setMyTarget(EntityID id, Building selfBuilding) ;

    public void setBorderDirectionBuildings(EntityID id, List<StandardEntity> borderDirectionBuildings) ;

    public void setFireBrigadeData(EntityID id, MrlBuilding targetBuilding) ;

    public void setBuildingValues(EntityID id, List<MrlBuilding> mrlBuildings) ;

    public void setAreaVisibility(List<MrlRoad> mrlRoads, List<MrlBuilding> mrlBuildings);

    public void setExtinguishData(Map<EntityID, List<EntityID>> extinguishableFromAreasMap, Map<EntityID, List<MrlBuilding>> buildingsInExtinguishRangeMap);

    public void setPartitions(EntityID id, List<Partition> partitions, Partition humanPartition, Set<Partition> humanPartitionsMap) ;

    public void setPartitionsData(EntityID id, List<Partition> partitions, Map<Human, Set<Partition>> humanPartitionsMap) ;

    public void setLayerPut(EntityID id, ArrayList[] clusters, List<Point> clusterCenterPoints);

    public void setPlatoonAgents(EntityID id, MrlPlatoonAgent tMrlPlatoonAgent) ;

    public void setUnreachableBuildings(EntityID id, HashSet<EntityID> entityIDs) ;

    public void setCivilianData(EntityID id, Set<EntityID> shouldDiscoverBuildings, EntityID civilianInProgress, EntityID buildingInProgress) ;

    public void removeExplorePosition(EntityID id, StandardEntity entity) ;

    public void setPossibleBuildings(EntityID id, List<EntityID> possibleBuildings) ;

    public void setEmptyBuildings(EntityID id, List<EntityID> emptyBuildings) ;

    public void setShouldCheckInsideBuildings(List<MrlBuilding> shouldCheckInsideBuildings) ;

    public void setExplorePositions(EntityID id, Set<EntityID> areas, MrlWorld world) ;

    public void setActionData(EntityID id, MrlZone zone) ;

    public void setTargetsToGo(EntityID id, Task myTask) ;

    public void setHeardPositions(EntityID civID, Pair<Integer, Integer> selfLocation) ;

    public void setMrlBuildingsMap(MrlBuilding b) ;

    public void setViewerBuildingsMap(EntityID id, List<MrlBuilding> mrlBuildings) ;

    public void setViewRoadsMap(EntityID id, List<MrlRoad> mrlRoads) ;

    public void setPathList(Paths paths) ;

    public void setZones(MrlZones zones) ;

    public void updateThisCycleData(MrlPlatoonAgent platoonAgent, MrlWorld world) ;

    public void setBlockedBuildings(MrlPlatoonAgent platoonAgent, EntityID buildingId, boolean reachable) ;

    public void setMrlRoadMap(MrlPlatoonAgent platoonAgent, EntityID id, MrlRoad mrlRoad) ;

    public void setGraphEdges(MrlPlatoonAgent platoonAgent, EntityID id, ArrayList<MyEdge> thisAreaMyEdges) ;

    public void setAllGrids(EntityID id, List<Grid> grids);

    public void removeAreaGrids(EntityID id) ;

    public void setBuildingAdvantageRatio(EntityID id, double advantageRatio) ;

    public void setPFGuideline(EntityID id, GuideLine guideLine) ;

    public void setPFClearAreaLines(EntityID id, rescuecore2.misc.geometry.Line2D targetLine, rescuecore2.misc.geometry.Line2D first, rescuecore2.misc.geometry.Line2D second);

    public void setScaledBlockadeData(EntityID id ,List<StandardEntity> obstacles, Polygon scaledBlockades , Map<rescuecore2.misc.geometry.Line2D, List<Point2D>> freePoints , List<Point2D> targetPoint);

    public void setObstacleBounds(EntityID id, List<rescuecore2.misc.geometry.Line2D> boundLines);

    public void setAllPartitionsMapData(EntityID id, Map<EntityID, Partition> partitionMap);

    public void setPoliceFireTasksData(EntityID id , Partition nearestBurningPartition , Partition neighbourPartition);

    void printData(MrlPlatoonAgent platoonAgent, int time, String data);
}
