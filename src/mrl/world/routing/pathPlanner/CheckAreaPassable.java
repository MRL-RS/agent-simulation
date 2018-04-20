package mrl.world.routing.pathPlanner;

import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.common.comparator.ConstantComparators;
import mrl.helper.EdgeHelper;
import mrl.helper.PropertyHelper;
import mrl.helper.RoadHelper;
import mrl.world.MrlWorld;
import mrl.world.object.MrlRoad;
import mrl.world.routing.a_star.A_StarForBlockedArea;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.graph.MyEdge;
import mrl.world.routing.graph.Node;
import mrl.world.routing.grid.AreaGrids;
import mrl.world.routing.grid.Grid;
import mrl.world.routing.grid.UpdateGridPassable;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by Mostafa  Shabani.
 * Date: Sep 22, 2010
 * Time: 5:35:00 PM
 */
public class CheckAreaPassable implements MRLConstants {

    private A_StarForBlockedArea a_starForBlockedArea;
    private MrlWorld world;
    private Graph graph;
    private UpdateGridPassable updateGridPassable;
    private Deque<Pair<EntityID, AreaGrids>> visitedAreasGrids = new ArrayDeque<Pair<EntityID, AreaGrids>>();
    private int maxSaveAreaGrid = 10;
    private Map<Pair<EntityID, EntityID>, List<Pair<Integer, Integer>>> hardWalkPaths = new FastMap<Pair<EntityID, EntityID>, List<Pair<Integer, Integer>>>();
    private Map<EntityID, Integer> lastUpdateTimeMap = new FastMap<EntityID, Integer>();

    public CheckAreaPassable(PathPlanner pathPlanner) {
        this.world = pathPlanner.getWorld();
        this.graph = pathPlanner.getGraph();
        this.a_starForBlockedArea = new A_StarForBlockedArea();
        this.updateGridPassable = new UpdateGridPassable(this);

        // maximum te'dade area grid haei ke yek agent mitavanad dar hafezeye khod zakhire konad.
        this.maxSaveAreaGrid = (world.getRoads().size() / world.getAgents().size());
    }

    public MrlWorld getWorld() {
        return world;
    }

    public Integer getLastUpdateTime(EntityID id) {
        Integer updateTime = lastUpdateTimeMap.get(id);
        if (updateTime == null) {
            updateTime = 0;
        }
        return updateTime;
    }

    public void resetPreviousVisitedRoad(int time) {
        // passable kardane edge haei ke hade aghal 15 cycle pish dide shode.
        int newTime = time - 4;
        time -= 18;
        for (EntityID id : lastUpdateTimeMap.keySet()) {
            if (lastUpdateTimeMap.get(id) < time) {
                for (MyEdge edge : graph.getMyEdgesInArea(id)) {
                    edge.setPassable(true);
                }
                lastUpdateTimeMap.put(id, newTime);
            }
        }
    }

    /**
     * in method check mikone ke aya yek area be area haye dige rah dare ya na.
     * be in sorat ke az har edge passable be edge passable dige grid path mizane.
     * albate vase har dota edge faghat yebar path mizane.
     * yani ya A2B or B2A.
     * age natoonest vase hich dota edgi path peida kone area impassable mishe.
     *
     * @param area check area
     */
    public void checkAreaPassable(Area area) {
        int time = world.getTime();
        Integer lastUpdateTime = lastUpdateTimeMap.get(area.getID());

        if ((lastUpdateTime != null && lastUpdateTime == time) || !area.isBlockadesDefined()) {
            return;
        }

        if (area.getBlockades().isEmpty()) {
            // age ye area blickade nadasht hame nodhash passable mishe. va grid hash hazf mishe.

            List<MyEdge> myEdges = graph.getMyEdgesInArea(area.getID());

            for (MyEdge myEdge : myEdges) {
                myEdge.setPassable(true);
            }
            for (Node node : graph.getAreaNodes(area.getID())) {
                node.setPassable(true, time);
            }

            removeAreaGrid(area.getID());
            world.getHelper(RoadHelper.class).setRoadPassable(area.getID(), true);
            lastUpdateTimeMap.put(area.getID(), time);

            return;
        }
        // hala age area blockade dasht. va ama ba'd...
        Blockade blockade;
        boolean check = false;

        for (EntityID blockadeId : area.getBlockades()) {
            blockade = (Blockade) world.getEntity(blockadeId);

            if ((time - world.getHelper(PropertyHelper.class).getEntityLastUpdateTime(blockade)) < 5) {
                // age update time blockade ha kamtar az 5 cycle bashe area ro check mikone.
                check = true;
                break;
            }
        }

        if (check) {
            lastUpdateTimeMap.put(area.getID(), time);

            AreaGrids areaGrids = getAreaGrids(area.getID());

            if (areaGrids == null) {
                areaGrids = new AreaGrids(area, updateGridPassable.getGridSize());
                addVisitedAreaGrid(area, areaGrids);
            }

            if (!areaIsPassable(area, areaGrids)) {
                world.getHelper(RoadHelper.class).setRoadPassable(area.getID(), false);
            } else {
                world.getHelper(RoadHelper.class).setRoadPassable(area.getID(), true);
            }
        }
    }

    /**
     * dar in method vase har edge-e area grid path mizanim.
     * va age vojood nadasht node marboot be oon edge ro impassable mikonim.
     * vase har edgi natoone grid peyda kone node marboot be oon edge ro impassable mikone.
     * age path natoone bezane node marboot be edge destination ro impassable mikone.
     *
     * @param area      for check area
     * @param areaGrids all grids for this area
     * @return area passably
     */
    private boolean areaIsPassable(Area area, AreaGrids areaGrids) {

        updateGridPassable.update(area, areaGrids, true);

        int time = world.getTime();
        List<Edge> edgeList = new ArrayList<Edge>();

        for (Edge edge : area.getEdges()) {
            // bedast avordane edge haye passable.
            if (edge.isPassable()) {
                edgeList.add(edge);
            }
        }
        for (Node node : graph.getAreaNodes(area.getID())) {
            node.setPassable(true, time);
        }

        // inja sortesh mikonim ke hamishe source az destination bozorgtar bashe.
        Collections.sort(edgeList, ConstantComparators.EDGE_LENGTH_COMPARATOR);

        List<Edge> impassableEdges = new ArrayList<Edge>();
        List<EntityID> passableNeighbours = new ArrayList<EntityID>();
        boolean isPassable = true;
        boolean isIsolated = true;
        boolean nodeIsPassable;
        EntityID sourceAreaId;
        Grid sourceGrid;
        int size = edgeList.size();
        int i = 0;
        Node node1;
        Node node2;

        for (Edge edge1 : edgeList) {
            i++;
            nodeIsPassable = false;
            node1 = graph.getNode(EdgeHelper.getEdgeMiddle(edge1));

            if (node1 == null) {
                impassableEdges.add(edge1);
            } else if (!impassableEdges.contains(edge1)) {
                boolean flag = false;
                EntityID destinationAreaId;
                sourceAreaId = edge1.getNeighbour();
                // bedast avordane source grid.
                sourceGrid = getBestGrid(areaGrids.getGrids(), node1.getPosition(), sourceAreaId);

                if (sourceGrid == null) {
                    setPassablyAllMyEdgeForNode(area.getID(), node1.getId(), false, time);
                    // in edge ro too impassable edges add mikonim ta dobare checkesh nakonim.(vasash donbale grid nagardim)
                    impassableEdges.add(edge1);
                    isPassable = false;
                } else {

                    for (int j = i; j < size; j++) {
                        // hala az khode edge be ba'd edame midim.
                        Edge edge2 = edgeList.get(j);
                        node2 = graph.getNode(EdgeHelper.getEdgeMiddle(edge2));

                        if (node2 == null) {
                            impassableEdges.add(edge2);
                        } else if (!impassableEdges.contains(edge2)) {
                            flag = true;

                            destinationAreaId = edge2.getNeighbour();
                            // bedast avordane destination grid.
                            Grid destinationGrid = getBestGrid(areaGrids.getGrids(), node2.getPosition(), destinationAreaId);

                            if (destinationGrid == null) {
                                setPassablyAllMyEdgeForNode(area.getID(), node2.getId(), false, time);
                                // bazam age grid nadasht too impassable edges add mishe. ke...
                                impassableEdges.add(edge2);
                                isPassable = false;
                            } else {
                                List<Pair<Integer, Integer>> pairPath = new ArrayList<Pair<Integer, Integer>>();

                                if (sourceGrid.equals(destinationGrid)) {
                                    // age source o dest yeki bashe faghat yek point too hard walk path-e marboote mizarim.
                                    pairPath.add(sourceGrid.getPosition());
                                    addHardWalkPath(sourceAreaId, destinationAreaId, pairPath);
                                    isIsolated = false;
                                    nodeIsPassable = true;
                                    if (!passableNeighbours.contains(sourceAreaId)) {
                                        passableNeighbours.add(sourceAreaId);
                                    } else if (!passableNeighbours.contains(destinationAreaId)) {
                                        passableNeighbours.add(destinationAreaId);
                                    }
                                    setPassablyCommonMyEdge(area.getID(), node1, node2, true);

                                    continue;
                                }
                                // bedast avordane pair path.
                                pairPath = getHardWalkPairPath(areaGrids, sourceGrid, destinationGrid);

                                if (pairPath.size() < 2) {
                                    // vase in bozorgtar kamtar az 2 chon dar har soorat destination pair ro barmigardoone.
                                    setPassablyCommonMyEdge(area.getID(), node1, node2, false);
                                    isPassable = false;
                                } else {
                                    addHardWalkPath(sourceAreaId, destinationAreaId, pairPath);
                                    isIsolated = false;
                                    nodeIsPassable = true;
                                    if (!passableNeighbours.contains(sourceAreaId)) {
                                        passableNeighbours.add(sourceAreaId);
                                    } else if (!passableNeighbours.contains(destinationAreaId)) {
                                        passableNeighbours.add(destinationAreaId);
                                    }
                                    setPassablyCommonMyEdge(area.getID(), node1, node2, true);
                                }
                            }
                        }
                    }
                }
                if (!nodeIsPassable && flag) {
                    node1.setPassable(false, time);
                }
            }
        }
        if (isIsolated) {
            world.getHelper(RoadHelper.class).setIsolated(area.getID(), true);
        } else {
            world.getHelper(RoadHelper.class).setIsolated(area.getID(), false);
        }
        return isPassable || (area.getNeighbours().size() == passableNeighbours.size());
    }

    /**
     * bedast avordane nazdiktarin grid-e passable too in area.
     * age next area null bashe yani areaNeighbour-e grid vasamoon mohem nist.
     * vagarna hatman bayad grid hamsayeye nextArea bashe.
     *
     * @param gridList   list of area grids
     * @param xYPair     target x,y
     * @param nextAreaId target area id
     * @return a best grid for start or finish
     */
    public Grid getBestGrid(List<Grid> gridList, Pair<Integer, Integer> xYPair, EntityID nextAreaId) {

        int distanceToEdgeCenter;
        int minDistance = Integer.MAX_VALUE;
        Grid selectedGrid = null;
        boolean flag = false;
        int minDistanceNew = Integer.MAX_VALUE;
        Grid selectedGridNew = null;

        if (nextAreaId != null) {
            for (Grid grid : gridList) {

                distanceToEdgeCenter = Util.distance(grid.getPosition(), xYPair);

                if (grid.getNeighbourAreaIds().contains(nextAreaId)) {
                    flag = true;
                    if (grid.isPassable() && distanceToEdgeCenter < minDistance) {
                        selectedGrid = grid;
                        minDistance = distanceToEdgeCenter;
                    }
                }
                if (!flag) {
                    if (distanceToEdgeCenter < minDistanceNew) {
                        selectedGridNew = grid;
                        minDistanceNew = distanceToEdgeCenter;
                    }
                }
            }
        } else {
            flag = true;
            for (Grid grid : gridList) {

                distanceToEdgeCenter = Util.distance(grid.getPosition(), xYPair);

                if (/*grid.isPassable() && */distanceToEdgeCenter < minDistance) {
                    selectedGrid = grid;
                    minDistance = distanceToEdgeCenter;
                }
            }
        }
        if (flag) {
            return selectedGrid;
        } else {
            if (selectedGridNew != null && selectedGridNew.isPassable()) {
                return selectedGridNew;
            } else {
                return null;
            }
        }
    }

    /**
     * bedast avordane ye list az x,y ha vase obur az ye area.
     *
     * @param areaGrids       grid list in this area
     * @param sourceGrid      source
     * @param destinationGrid destination
     * @return list of points
     */
    public List<Pair<Integer, Integer>> getHardWalkPairPath(AreaGrids areaGrids, Grid sourceGrid, Grid destinationGrid) {
        /**
         * age source o destination yeki nabashan pair path peida mikone.
         */
        List<Pair<Integer, Integer>> finalPath = new ArrayList<Pair<Integer, Integer>>();

        if (!sourceGrid.equals(destinationGrid)) {

            List<Grid> gridPath = a_starForBlockedArea.getShortestPathForBlockadeRoad(areaGrids, sourceGrid, destinationGrid, false);
            for (Grid grid : gridPath) {
                finalPath.add(grid.getPosition());
            }
        }

        return finalPath;
    }


    /**
     * bedast avordane ye list az grid-ha vase vase check kardane passably va blockade vase clear.
     * in method faghat vase police-e vase inke be passable boodane grid ha ahamiat nemide,
     * vali mitune behtarin blockade-ha ro peida kone ke police ba chanta clear kardan raho baz kone.
     *
     * @param areaGrids       grid list in this area
     * @param sourceGrid      source
     * @param destinationGrid destination
     * @return list of points
     */
    public List<Grid> getGridPath(AreaGrids areaGrids, Grid sourceGrid, Grid destinationGrid) {
        /**
         * age source o destination yeki nabashan pair path peida mikone.
         */
        List<Grid> gridPath = new ArrayList<Grid>();

        if (!sourceGrid.equals(destinationGrid)) {

            gridPath = a_starForBlockedArea.getShortestPathForBlockadeRoad(areaGrids, sourceGrid, destinationGrid, true);
        }

        return gridPath;
    }


    public void setPassablyCommonMyEdge(EntityID areaId, Node sourceNodeId, Node destinationNodeId, boolean passably) {
        /**
         * in method MyEdge-e marboot be dota area ro passably-sh ro avaz mikone.
         * albate faghat baraye agent haye gheire police.
         */
        if (destinationNodeId == null || sourceNodeId == null) {
            return;
        }
        if (!sourceNodeId.equals(destinationNodeId)) {
            graph.getMyEdge(areaId, new Pair<Node, Node>(sourceNodeId, destinationNodeId)).setPassable(passably);
        }
    }

    public void setPassablyAllMyEdgeForNode(EntityID areaId, EntityID nodeId, boolean passably, int time) {
        /**
         * this method set passably for all MyEdges in a area.
         * albate faghat baraye agent haye gheire police.
         */

        List<MyEdge> areaMyEdge;
        areaMyEdge = graph.getMyEdgesInArea(areaId);
        graph.getNode(nodeId).setPassable(passably, time);

        for (MyEdge myEdge : areaMyEdge) {
            if (nodeId.equals(myEdge.getNodes().first().getId()) || nodeId.equals(myEdge.getNodes().second().getId())) {
                myEdge.setPassable(passably);
            }
        }
    }

    private void addHardWalkPath(EntityID sourceId, EntityID destinationId, List<Pair<Integer, Integer>> gridPath) {
        this.hardWalkPaths.put(new Pair<EntityID, EntityID>(sourceId, destinationId), gridPath);
    }

    public void addVisitedAreaGrid(Area area, AreaGrids areaGrids) {
        /**
         * ye te'dadi az AreaGrid ha ro zakhire mikonim.
         * mesle catch.
         */
        if (visitedAreasGrids.size() > maxSaveAreaGrid) {
            visitedAreasGrids.removeFirst();

            MrlPersonalData.VIEWER_DATA.removeAreaGrids(visitedAreasGrids.getFirst().first());

        }
        visitedAreasGrids.add(new Pair<EntityID, AreaGrids>(area.getID(), areaGrids));
    }

    public void removeAreaGrid(EntityID id) {

        Pair<EntityID, AreaGrids> toRemove = null;

        for (Pair<EntityID, AreaGrids> pair : visitedAreasGrids) {
            if (pair.first().equals(id)) {
                toRemove = pair;
                break;
            }
        }
        visitedAreasGrids.remove(toRemove);
    }

    public List<Pair<Integer, Integer>> getHardWalkPath(EntityID sourceId, EntityID destinationId) {
        /**
         * hard walk pathe marboot be ye area ro barmigardoone.
         * albate age ghablan mihasebe shode bashe.
         */

        if (sourceId == null || destinationId == null) {
            return null;
        }

        List<Pair<Integer, Integer>> pairPath;
        pairPath = hardWalkPaths.get(new Pair<EntityID, EntityID>(sourceId, destinationId));

        if (pairPath == null) {
            List<Pair<Integer, Integer>> tempPath = hardWalkPaths.get(new Pair<EntityID, EntityID>(destinationId, sourceId));
            if (tempPath != null) {
                pairPath = new ArrayList<Pair<Integer, Integer>>();
                for (int i = tempPath.size() - 1; i >= 0; i--) {
                    pairPath.add(tempPath.get(i));
                }
            }
        }
        return pairPath;
    }

    public AreaGrids getAreaGrids(EntityID id) {

        Pair<EntityID, AreaGrids> selected = null;

        for (Pair<EntityID, AreaGrids> pair : visitedAreasGrids) {
            if (pair.first().equals(id)) {
                selected = pair;
                break;
            }
        }

        if (selected != null) {
            // faghat vase inke first nabashe ke zood hazf beshe. (akhe ghadimi ha hazf mishan)
            visitedAreasGrids.remove(selected);
            visitedAreasGrids.add(selected);
            return selected.second();
        }
        // age Ghablan nadide bood create va ezafe mikone.
        Area area = world.getEntity(id, Area.class);
        AreaGrids areaGrids = new AreaGrids(area, updateGridPassable.getGridSize());
        addVisitedAreaGrid(area, areaGrids);
        return areaGrids;
    }

    public boolean isThisAreaBecamePassable(Area area) {
        if (area instanceof Road) {
            MrlRoad road = world.getMrlRoad(area.getID());
            return road == null || road.isPassable();
        }
        return true;
    }

    /**
     * in method faghat vase police va blockade ha ro vase clear kardan behesh mide.
     * hamchenin ckeck mikone ke ye area passable hast ya na.
     *
     * @param area               checking area
     * @param sourceAndDestEdges edges for check
     * @return list of blockades for clear
     */
    public List<EntityID> policeCheckPassably(Area area, Pair<Edge, Edge> sourceAndDestEdges) {
        List<EntityID> blockades = new ArrayList<EntityID>();

        if (!area.isBlockadesDefined()) {
            return null;
        }

        if (area.getBlockades().isEmpty()) {
            // age ye area blickade nadasht hame nodhash passable mishe. va grid hash hazf mishe.

            removeAreaGrid(area.getID());
            world.getHelper(RoadHelper.class).setRoadPassable(area.getID(), true);

            return blockades;
        }
        // hala age area blockade dasht. va ama ba'd...

        AreaGrids areaGrids = getAreaGrids(area.getID());

        if (areaGrids == null) {
            areaGrids = new AreaGrids(area, updateGridPassable.getGridSize());
            addVisitedAreaGrid(area, areaGrids);
        }

        updateGridPassable.update(area, areaGrids, true);

        int time = world.getTime();
        List<Edge> edgeList = new ArrayList<Edge>();

        if (sourceAndDestEdges == null) {
            for (Edge edge : area.getEdges()) {
                // bedast avordane edge haye passable.
                if (edge.isPassable()) {
                    edgeList.add(edge);
                }
            }
        } else {
            edgeList.add(sourceAndDestEdges.first());
            edgeList.add(sourceAndDestEdges.second());
        }
        for (Node node : graph.getAreaNodes(area.getID())) {
            node.setPassable(true, time);
        }

        // inja sortesh mikonim ke hamishe source az destination bozorgtar bashe.
        Collections.sort(edgeList, ConstantComparators.EDGE_LENGTH_COMPARATOR);

        List<Edge> impassableEdges = new ArrayList<Edge>();
//        boolean isIsolated = true;
        EntityID sourceAreaId;
        Grid sourceGrid;
        int size = edgeList.size();
        int i = 0;
        Node node1;
        Node node2;

        for (Edge edge1 : edgeList) {
            i++;
            node1 = graph.getNode(EdgeHelper.getEdgeMiddle(edge1));

            if (node1 == null) {
                impassableEdges.add(edge1);
            } else if (!impassableEdges.contains(edge1)) {
                EntityID destinationAreaId;
                sourceAreaId = edge1.getNeighbour();
                // bedast avordane source grid.
                sourceGrid = getBestGridForPolice(areaGrids.getGrids(), node1.getPosition(), sourceAreaId);

                if (sourceGrid == null) {
                    // in edge ro too impassable edges add mikonim ta dobare checkesh nakonim.(vasash donbale grid nagardim)
                    impassableEdges.add(edge1);
                } else {

                    for (int j = i; j < size; j++) {
                        // hala az khode edge be ba'd edame midim.
                        Edge edge2 = edgeList.get(j);
                        node2 = graph.getNode(EdgeHelper.getEdgeMiddle(edge2));

                        if (node2 == null) {
                            impassableEdges.add(edge2);
                        } else if (!impassableEdges.contains(edge2)) {

                            destinationAreaId = edge2.getNeighbour();
                            // bedast avordane destination grid.
                            Grid destinationGrid = getBestGridForPolice(areaGrids.getGrids(), node2.getPosition(), destinationAreaId);

                            if (destinationGrid == null) {
                                // bazam age grid nadasht too impassable edges add mishe. ke...
                                impassableEdges.add(edge2);
                            } else {
                                List<Grid> gridPath = new ArrayList<Grid>();

                                if (sourceGrid.equals(destinationGrid)) {
                                    // age source o dest yeki bashe faghat yek point too hard walk path-e marboote mizarim.
                                    gridPath.add(sourceGrid);
//                                    isIsolated = false;
                                    blockades.addAll(sourceGrid.getBlockades());
                                    continue;
                                }
                                // bedast avordane pair path.
                                gridPath = getGridPath(areaGrids, sourceGrid, destinationGrid);

                                if (gridPath.size() < 2) {
                                    throw new RuntimeException("ERRORRRRR ---- Time:" + world.getTime() + " agent:" + world.getSelfHuman() + " Position:" + world.getSelfPosition() + " source:" + sourceAreaId + " dest:" + destinationAreaId);
                                } else {
//                                    isIsolated = false;
                                    for (Grid grid : gridPath) {
                                        blockades.addAll(grid.getBlockades());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        if (isIsolated) {
//            world.getHelper(RoadHelper.class).setIsolated(area.getID(), true);
//        } else {
//            world.getHelper(RoadHelper.class).setIsolated(area.getID(), false);
//        }

        if (blockades.isEmpty()) {
            world.getHelper(RoadHelper.class).setRoadPassable(area.getID(), true);
        } else {
            world.getHelper(RoadHelper.class).setRoadPassable(area.getID(), false);
        }
        return blockades;
    }

    public Grid getBestGridForPolice(List<Grid> gridList, Pair<Integer, Integer> xYPair, EntityID nextAreaId) {

        int distanceToEdgeCenter;
        int minDistance = Integer.MAX_VALUE;
        Grid selectedGrid = null;

        for (Grid grid : gridList) {

            distanceToEdgeCenter = Util.distance(grid.getPosition(), xYPair);
            distanceToEdgeCenter *= grid.selfValue;

            if (grid.getNeighbourAreaIds().contains(nextAreaId)) {
                if (distanceToEdgeCenter < minDistance) {
                    selectedGrid = grid;
                    minDistance = distanceToEdgeCenter;
                }
            }
        }
        return selectedGrid;
    }
}