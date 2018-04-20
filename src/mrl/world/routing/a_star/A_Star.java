package mrl.world.routing.a_star;

import javolution.util.FastSet;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.world.MrlWorld;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.graph.MyEdge;
import mrl.world.routing.graph.Node;
import mrl.world.routing.pathPlanner.PathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Mostafa  Shabani.
 * Date: Sep 22, 2010
 * Time: 5:05:00 PM
 */
public class A_Star implements MRLConstants {

    private MrlWorld world;
    private Graph graph;

    private int maxSearchDistance;
    private int pathCost;
    private int tryToPassFromSmallEdge;
    private Area preSource;

    public A_Star(PathPlanner pathPlanner) {
        this.world = pathPlanner.getWorld();
        this.graph = pathPlanner.getGraph();
        maxSearchDistance = graph.getGraphSize();
    }

    public List<EntityID> getShortestPath(Area source, Area destination, boolean force) {

        List<EntityID> path = new ArrayList<EntityID>();

        // bedast avardane kotahtarin graph be target.
        path.addAll(getShortestGraphPath(source, destination, force));


        if (path.size() != 0) {

//            Node node = graph.getNode(path.get(path.size() - 1));
//            if (shouldBeRemoved(node)) {
////                path.remove(node.getId());
//                return new ArrayList<EntityID>();
//            }

            // baraye zamani ke masiri vojood nadashte bashad.
            path = getAreaPath(source.getID(), destination.getID(), path);
        }

        return path;
    }


    private List<EntityID> getValidPlanMove(List<EntityID> plan) {
        Edge edge;
        Area area;
        for (int i = 0; i < plan.size() - 1; i++) {//these lines are for test a_star validity.
            area = world.getEntity(plan.get(i), Area.class);
            edge = area.getEdgeTo(plan.get(i + 1));
            if (edge == null) {
//                MrlPersonalData.print(platoonAgent.getDebugString() + " Illegal move plan!!!..............");
                plan = plan.subList(0, i + 1);
                break;
            }
        }
        return plan;
    }

    private boolean shouldBeRemoved(Node node) {
        Road road;
        Blockade blockade;
        boolean shouldBeRemoved = false;
        for (EntityID entityID : node.getNeighbourAreaIds()) {
            if (world.getEntity(entityID) instanceof Road) {
                road = (Road) world.getEntity(entityID);
                if (world.getEntranceRoads().keySet().contains(road.getID())) {
                    if (road.isBlockadesDefined()) {
                        for (EntityID blockadeId : road.getBlockades()) {
                            blockade = (Blockade) world.getEntity(blockadeId);
                            if (blockade.getShape().contains(road.getX(), road.getY())
                                    || Util.findDistanceTo(blockade, road.getX(), road.getY()) <= 500) {
                                shouldBeRemoved = true;
                                break;
                            }
                        }
                        if (shouldBeRemoved) {
                            break;
                        }
                    }
                }
            }

        }
        return shouldBeRemoved;
    }

    public List<EntityID> getShortestGraphPath(Area source, Area destination, boolean force) {
        if (destination == null) {
            System.err.println("ERROR: " + world.getSelf() + " Destination is null......");
            return new ArrayList<EntityID>();
        }

        Node sourceNode = getNearestNode(source, world.getSelfLocation());
        Node destinationNode = getNearestNode(destination, destination.getLocation(world));
        int extraPathCost = 0;

        if (sourceNode == null || destinationNode == null) {
            return new ArrayList<EntityID>();
        }

        // this shit is commented by Pooya; These lines make method nondeterministic
//        if (sourceNode.isOnTooSmallEdge() && (preSource != null && preSource.equals(source))) {
//            tryToPassFromSmallEdge++;
//
//            if (tryToPassFromSmallEdge == 2) {
//                tryToPassFromSmallEdge = 0;
//                return new ArrayList<EntityID>();
//            }
//        }
        preSource = source;

        extraPathCost += Util.distance(source.getLocation(world), sourceNode.getPosition());
        extraPathCost += Util.distance(destination.getLocation(world), destinationNode.getPosition());

        // method-e aslie A* baraye peyda kardane path.
        boolean findPath = false;
        Set<Node> open = new FastSet<Node>();
        Set<EntityID> closed = new FastSet<EntityID>();
        Node current;
        sourceNode.setG(0);
        sourceNode.setCost(0);
        sourceNode.setDepth(0);
        sourceNode.setParent(null);
        destinationNode.setParent(null);
        open.add(sourceNode);

        if (sourceNode.equals(destinationNode)) {
//            if (world.getPlatoonAgent().getPathPlanner().getAreaPassably().isThisAreaBecamePassable(source)
//                    && world.getPlatoonAgent().getPathPlanner().getAreaPassably().isThisAreaBecamePassable(destination)) {
            pathCost = sourceNode.getCost();
            pathCost += extraPathCost;
            return getPath(destinationNode);
//            } else {
//                return new ArrayList<EntityID>();
//            }
        }

        int maxDepth = 0;
        pathCost = -1;

        while ((maxDepth < maxSearchDistance) && (open.size() != 0)) {

            current = Collections.min(open);

            if (current.equals(destinationNode)) {
                findPath = true;
                pathCost = current.getCost();
                pathCost += extraPathCost;
                break;
            }

            open.remove(current);
            closed.add(current.getId());

            for (Pair<EntityID, MyEdge> neighbour : current.getNeighbourNodes()) {
                MyEdge neighbourMyEdge = neighbour.second();
                Node neighbourNode = neighbourMyEdge.getOtherNode(current);

                if (!closed.contains(neighbourNode.getId()) && (neighbourMyEdge.isPassable() || force)) {
                    int neighbourG = neighbourMyEdge.getWeight() + current.getG(); // neighbour weight

//                    if (POLICE_STRATEGY) {
//                        Area area = world.getEntity(neighbourMyEdge.getAreaId(), Area.class);
//
//                        if (area != null && (world.getSelf() instanceof MrlPoliceForce)) {
//                            int totalRepairCost = AreaHelper.totalRepairCost(world, area);
//                            if (totalRepairCost < 1) {
//                                totalRepairCost = 1;
//                            }
//                            neighbourG *= totalRepairCost;
//                        }
//                    }
                    if (DO_NOT_MOVE_IN_BURNING_BUILDING) {
                        Area area = world.getEntity(neighbourMyEdge.getAreaId(), Area.class);
                        if (area != null && (area instanceof Building) && ((Building) area).isFierynessDefined()) {
                            int fieriness = ((Building) area).getFieryness();
                            if (fieriness > 0 && fieriness < 4) {
                                neighbourG *= 100;
                            }
                        }
                    }

                    if (!open.contains(neighbourNode)) {

                        neighbourNode.setParent(current.getId());
                        neighbourNode.setHeuristic(getHeuristicDistance(neighbourNode, destinationNode));
                        neighbourNode.setG(neighbourG);
                        neighbourNode.setCost(neighbourNode.getHeuristic() + neighbourG);
                        neighbourNode.setDepth(current.getDepth() + 1);

                        open.add(neighbourNode);

                        if (neighbourNode.getDepth() > maxDepth) {
                            maxDepth = neighbourNode.getDepth();
                        }

                    } else {

                        if (neighbourNode.getG() > neighbourG) {

                            neighbourNode.setParent(current.getId());
                            neighbourNode.setG(neighbourG);
                            neighbourNode.setCost(neighbourNode.getHeuristic() + neighbourG);
                            neighbourNode.setDepth(current.getDepth() + 1);

                            if (neighbourNode.getDepth() > maxDepth) {
                                maxDepth = neighbourNode.getDepth();
                            }
                        }
                    }
                }
            }
        }
        if (findPath) {
            return getPath(destinationNode);
        } else {
            return new ArrayList<EntityID>();
        }
    }

    private int getHeuristicDistance(Node node, Node destinationNode) {
        // mohasebe fasele heuristic.
//        return (Math.abs(node.getPosition().first() - destinationNode.getPosition().first())
//                + Math.abs(node.getPosition().second() - destinationNode.getPosition().second()));
        return Util.distance(node.getPosition(), destinationNode.getPosition());
    }

    public Node getNearestNode(Area area, Pair<Integer, Integer> XYPair) {
        // yaftane nazdik tarin node be agent(self x,y) ya markaze area(target) dar yek area.
        Node selected = null;
        int minDistance = Integer.MAX_VALUE;
        int distance;
        List<Node> areaNodes = new ArrayList<Node>(graph.getAreaNodes(area.getID()));

        for (Node node : areaNodes) {
            if (node.isPassable()) {
                distance = Util.distance(XYPair.first(), XYPair.second(), node.getPosition().first(), node.getPosition().second());
                if (distance < minDistance) {
                    minDistance = distance;
                    selected = node;
                }
            }
        }
        if (selected == null) {
            for (Node node : areaNodes) {
                distance = Util.distance(XYPair.first(), XYPair.second(), node.getPosition().first(), node.getPosition().second());
                if (distance < minDistance) {
                    minDistance = distance;
                    selected = node;
                }
            }
        }
        return selected;
    }

    private List<EntityID> getPath(Node destinationNode) {
        // methodi baraye bedast avardane path be komake destination va parent haye node ha.
        List<EntityID> path = new ArrayList<EntityID>();
        Node current = destinationNode;

        path.add(current.getId());

        while (current.getParent() != null) {
            path.add(current.getParent());
            current = graph.getNode(current.getParent());
        }
        return path;
    }

    public List<EntityID> getAreaPath(EntityID sourceArea, EntityID destinationArea, List<EntityID> path) {
        // ba dast avardane entityID area ha baraye pathe nahaei be komake node ha.
        Node node;
        List<EntityID> areaPath = new ArrayList<EntityID>();
        List<EntityID> tempAreaPathList = new ArrayList<EntityID>();
        /**
         * har area ke 2 bar add shavad bayad dar path gharar girad.
         * chon aan area bein do node moshtarak ast.
         * mesle inke yek edge dashte bashim.
         */
        areaPath.add(sourceArea);
        for (int i = path.size() - 1; i >= 0; i--) {
            node = graph.getNode(path.get(i));
            for (EntityID areaId : node.getNeighbourAreaIds()) {
                if (tempAreaPathList.contains(areaId)) {
                    if (!areaPath.contains(areaId)) {
                        areaPath.add(areaId);
                    }
                } else {
                    tempAreaPathList.add(areaId);
                }
            }
        }

        if (!areaPath.contains(destinationArea)) {
            areaPath.add(destinationArea);
        }

        if (!((Area) world.getEntity(destinationArea)).getNeighbours().contains(sourceArea)
                && areaPath.size() < 3) {
            return new ArrayList<EntityID>();
        }
//        removeExtraAreasFromPath((ArrayList<EntityID>) areaPath);
        if(world.getTime() > world.getIgnoreCommandTime()){
            areaPath = getValidPlanMove(areaPath);
        }
        return areaPath;
    }

//    private void removeExtraAreasFromPath(ArrayList<EntityID> path) {
//        // hazfe area haei ke area-e ghabli va baidie an ba ham hamsaye and.
//
//        ArrayList<EntityID> toRemove = new ArrayList<EntityID>();
//        for (int counter = 0; counter < path.size() - 2; counter++) {
//            for (EntityID eId : ((Area) world.getEntity(path.get(counter))).getNeighboursByEdge()) {
//                if (eId.equals(path.get(counter + 2))) {
//                    toRemove.add(path.get(counter + 1));
//                }
//            }
//        }
//        path.removeAll(toRemove);
//    }

    public int getPathCost() {
        return pathCost;
    }
}