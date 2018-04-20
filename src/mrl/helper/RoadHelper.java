package mrl.helper;

import javolution.util.FastMap;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.info.RoadInfo;
import mrl.police.MrlPoliceForce;
import mrl.world.MrlWorld;
import mrl.world.object.*;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.graph.MyEdge;
import mrl.world.routing.graph.Node;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 12:08:37 PM
 */
public class RoadHelper implements IHelper {
    protected MrlWorld world;
    protected Map<EntityID, RoadInfo> roadInfoMap = new FastMap<EntityID, RoadInfo>();

    public RoadHelper(MrlWorld world) {
        this.world = world;
    }

    public void init() {
        initRoadInfoMap();
    }

    public void update() {
    }

    private void initRoadInfoMap() {
        for (StandardEntity standardEntity : world.getRoads()) {
            Road road = (Road) standardEntity;
            roadInfoMap.put(road.getID(), new RoadInfo(getNeighbours(road), road.getEdges()));
        }
    }

    public void setPathId(EntityID roadId, EntityID pathId) {
        roadInfoMap.get(roadId).setPathId(pathId);
    }

    public void setRoadPassable(EntityID id, Boolean passable) {
        roadInfoMap.get(id).setPassable(passable);
    }

    public void setIsolated(EntityID id, Boolean isolated) {
        roadInfoMap.get(id).setIsolated(isolated);
    }

    public boolean canSendMessage(EntityID id, int time) {
        RoadInfo roadInfo = roadInfoMap.get(id);
        if (roadInfo.getLastMessageTime() + 5 < time) {
            roadInfo.setLastMessageTime(time);
            return true;
        } else if (roadInfo.getLastMessageTime() + 1 == time) {
            return true;
        }
        return false;
    }

    /**
     * agar 1 area hich edgi ke be edge digar rah dashte bashad nadashte bashad passable nist
     *
     * @param roadId
     * @return
     */
    public boolean isPassable(EntityID roadId) {
        MrlRoad mrlRoad = world.getMrlRoad(roadId);
        return mrlRoad.isPassable();
    }

//    public boolean isPassable(Area from, Area to) {
//        if (!from.getNeighboursByEdge().contains(to.getID())) {
//            System.err.println("");
//            return false;
//        }
//        if (to instanceof Road) {
//            MrlRoad road = world.getMrlRoad(to.getID());
//            for (MrlEdge mrlEdge : road.getOpenEdges()) {
//                if (mrlEdge.getNeighboursByEdge().second().equals(from.getID())) {
//                    return true;
//                }
//            }
//        } else {
//            if (from instanceof Road) {
//                MrlRoad road = world.getMrlRoad(from.getID());
//                for (MrlEdge mrlEdge : road.getOpenEdges()) {
//                    if (mrlEdge.getNeighboursByEdge().second().equals(to.getID())) {
//                        return true;
//                    }
//                }
//            }
//        }
//        return true;
//    }

    public Boolean isSeen(EntityID id) {
        return world.getMrlRoad(id).isSeen();
    }

    public EntityID getPathId(EntityID id) {
        return roadInfoMap.get(id).getPathId();
    }

    public Set<EntityID> getNeighbours(EntityID id) {
        return roadInfoMap.get(id).getNeighbours();
    }

    public Set<EntityID> getNeighbours(Road road) {
        Set<EntityID> neighbours = new HashSet<>();

        for (Edge next : road.getEdges()) {
            if (next.isPassable()) {
                neighbours.add(next.getNeighbour());
            }
        }

        return neighbours;
    }

    public ArrayList<Road> getConnectedRoads(EntityID id) {
        ArrayList<Road> neighbours = new ArrayList<Road>();

        for (EntityID entityID : getNeighbours(id)) {
            StandardEntity standardEntity = world.getEntity(entityID);
            if (standardEntity instanceof Road) {
                neighbours.add((Road) standardEntity);
            }
        }

        return neighbours;
    }

    /**
     * get neighbours of type Building
     *
     * @param world is disaster space
     * @param road  to find connected buildings
     * @return neighbour buildings
     */
    public static List<Building> getConnectedBuildings(MrlWorld world, Road road) {
        List<Building> buildings = new ArrayList<Building>();
        StandardEntity standardEntity;

        for (EntityID id : road.getNeighbours()) {
            standardEntity = world.getEntity(id);
            if ((standardEntity instanceof Building) && !buildings.contains((Building) standardEntity)) {
                buildings.add((Building) standardEntity);
            }
        }
        return buildings;
    }

    /**
     * in method hameye building-haye mottasel be ye entrance ro peida mikone.
     *
     * @param entrance : target entrance.
     * @return : list of buildings of this entrance.
     */
    public List<Building> getBuildingsOfThisEntrance(EntityID entrance) {
        int loop = 0;
        List<Building> buildings = new ArrayList<Building>();
        List<Area> neighbours = new ArrayList<Area>();
        Area tempArea;
        Area neighbour;
        neighbours.add((Area) world.getEntity(entrance));

        while (!neighbours.isEmpty() && loop < 20) {
            loop++;
            tempArea = neighbours.get(0);
            neighbours.remove(0);

            for (EntityID entityID : tempArea.getNeighbours()) {
                neighbour = (Area) world.getEntity(entityID);
                if (neighbour instanceof Building) {
                    if (!buildings.contains((Building) neighbour)) {
                        buildings.add((Building) neighbour);
                        neighbours.add(neighbour);
                    }
                }
            }
        }
        return buildings;
    }

    public boolean isSeenAndBlocked(EntityID buildingID, EntityID roadEntrance) {
        MrlRoad mrlRoad = world.getMrlRoad(roadEntrance);
        if (!mrlRoad.isSeen())
            return false;

        for (MrlEdge mrlEdge : mrlRoad.getMrlEdgesTo(buildingID)) {
            if (mrlRoad.getReachableEdges(mrlEdge) == null || mrlRoad.getReachableEdges(mrlEdge).size() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isOpenOrNotSeen(EntityID buildingID, EntityID roadEntrance) {
        MrlRoad mrlRoad = world.getMrlRoad(roadEntrance);
        MrlBuilding mrlBuilding = world.getMrlBuilding(buildingID);
        if (mrlBuilding == null || mrlRoad == null) {
            world.printData(buildingID + " is not an mrlBuilding or " + roadEntrance + " is not a Road Entrance...");
            return false;
        }
        if (!mrlRoad.isSeen())
            return true;

        HashSet<MrlEdge> mrlEdges;
        List<MrlEdge> toRemove = new ArrayList<MrlEdge>();
        for (MrlEdge mrlEdge : mrlRoad.getMrlEdgesTo(buildingID)) {
            mrlEdges = new HashSet<MrlEdge>(mrlRoad.getReachableEdges(mrlEdge));
            mrlEdges.removeAll(mrlRoad.getMrlEdgesTo(buildingID));
            for (Entrance entrance : mrlBuilding.getEntrances()) {
                for (MrlEdge neighbourEdge : mrlEdges) {
                    if (neighbourEdge.getNeighbours().second().equals(entrance.getNeighbour().getID()) || neighbourEdge.isOtherSideBlocked(world)) {
                        toRemove.add(neighbourEdge);
                    }
                }
            }
            mrlEdges.removeAll(toRemove);
            if (!mrlEdge.isBlocked() && mrlEdges.size() > 0) {
                return true;
            }
        }
        return false;
    }


    //Mahdi Taherian=========>>
    public void updatePassably(MrlRoad mrlRoad) {
        if (world.getPlatoonAgent() == null || world.getPlatoonAgent() instanceof MrlPoliceForce) {
            return;
        }

        for (int i = 0; i < mrlRoad.getMrlEdges().size() - 1; i++) {
            MrlEdge edge1 = mrlRoad.getMrlEdges().get(i);
            if (!edge1.isPassable()) {
                continue;
            }
            for (int j = i + 1; j < mrlRoad.getMrlEdges().size(); j++) {
                MrlEdge edge2 = mrlRoad.getMrlEdges().get(j);
                if (!edge2.isPassable()) {
                    continue;
                }
                setMyEdgePassably(mrlRoad, edge1, edge2, isPassable(mrlRoad, edge1, edge2, world.getPlatoonAgent().isHardWalking()));
            }
        }
    }

    /**
     * @param mrlRoad
     * @param from
     * @param to
     * @return
     */
    public boolean isPassable(MrlRoad mrlRoad, MrlEdge from, MrlEdge to, boolean hardWalk) {

//        if (!mrlRoad.isBlockadesDefined()) {
//            return true;
//        }
        if (!from.getNeighbours().first().equals(to.getNeighbours().first())) {
//            throw new IncorrectInputException("this 2 edge is not in a same area!!!");
            System.err.println("this 2 edge is not in a same area!!!");
            return false;
        }
        if (hardWalk ? from.isAbsolutelyBlocked() || to.isAbsolutelyBlocked() : from.isBlocked() || to.isBlocked())
            return false;
        Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween = getEdgesBetween(mrlRoad, from, to, false);

        int count = mrlRoad.getMrlBlockades().size();
        List<MrlEdge> blockedEdges = new ArrayList<MrlEdge>();
        if (count == 1) {
            blockedEdges.addAll(mrlRoad.getMrlBlockades().get(0).getBlockedEdges());
        } else if (count > 1) {
            for (int i = 0; i < count - 1; i++) {
                MrlBlockade block1 = mrlRoad.getMrlBlockades().get(i);
                for (int j = i + 1; j < count; j++) {
                    MrlBlockade block2 = mrlRoad.getMrlBlockades().get(j);
                    if (isBlockedTwoSides(block1, edgesBetween)) {
                        return false;
                    }
                    if (isBlockedTwoSides(block2, edgesBetween)) {
                        return false;
                    }
                    if (isInSameSide(block1, block2, edgesBetween)) {
                        continue;
                    }
//                    double distance = Util.distance(block1.getPolygon(), block2.getPolygon());
//                    if (distance < MRLConstants.AGENT_PASSING_THRESHOLD) {

                    if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), hardWalk ? MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD : MRLConstants.AGENT_PASSING_THRESHOLD)) {
                        blockedEdges.removeAll(block1.getBlockedEdges());
                        blockedEdges.addAll(block1.getBlockedEdges());
                        blockedEdges.removeAll(block2.getBlockedEdges());
                        blockedEdges.addAll(block2.getBlockedEdges());
                    }
                }
            }
        } else if (count == 0) {
            return !(from.isBlocked() || to.isBlocked());
        }
        return !(Util.containsEach(blockedEdges, edgesBetween.first()) && Util.containsEach(blockedEdges, edgesBetween.second()));
    }


    private boolean isInSameSide(MrlBlockade block1, MrlBlockade block2, Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween) {
        return edgesBetween.first().containsAll(block1.getBlockedEdges()) &&
                edgesBetween.first().containsAll(block2.getBlockedEdges()) ||
                edgesBetween.second().containsAll(block1.getBlockedEdges()) &&
                        edgesBetween.second().containsAll(block2.getBlockedEdges());

    }

    private boolean isBlockedTwoSides(MrlBlockade block1, Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween) {
        return Util.containsEach(edgesBetween.first(), block1.getBlockedEdges()) &&
                Util.containsEach(edgesBetween.second(), block1.getBlockedEdges());
    }

    private void setMyEdgePassably(MrlRoad road, MrlEdge edge1, MrlEdge edge2, boolean passably) {
        if (world.getPlatoonAgent() == null || !edge1.getNeighbours().first().equals(edge2.getNeighbours().first()))
            return;
        Graph graph = world.getPlatoonAgent().getPathPlanner().getGraph();
        Node node1 = graph.getNode(edge1.getMiddle());
        Node node2 = graph.getNode(edge2.getMiddle());
        MyEdge myEdge = graph.getMyEdge(road.getParent().getID(), new Pair<Node, Node>(node1, node2));
        if (passably) {
            road.addReachableEdges(edge1, edge2);
//            road.addReachableEdges(edge2, edge1);
        } else {
            road.removeReachableEdges(edge1, edge2);
//            road.removeReachableEdges(edge2, edge1);
        }
        myEdge.setPassable(passably);
    }

    /**
     * 1road va 2ta edge ke dakhele un hastand ro migire va
     * tamame edgehaei ke bein un 2ta edge va dakhele un road hastand ro be shekle 1 Pair bar migardoone.
     *
     * @param road           roade morede barresi
     * @param edge1          edge avali
     * @param edge2          edge 2vomi
     * @param justImPassable in parameter bara ine ke faghat edgehaye impassable ezafe she ya na.
     * @return 1pair az edge haye ye samte un 2edge va edge haye samte digeshoon
     */
    public Pair<List<MrlEdge>, List<MrlEdge>> getEdgesBetween(MrlRoad road, MrlEdge edge1, MrlEdge edge2, boolean justImPassable) {
        List<MrlEdge> leftSideEdges = new ArrayList<MrlEdge>();
        List<MrlEdge> rightSideEdges = new ArrayList<MrlEdge>();
        rescuecore2.misc.geometry.Point2D startPoint1 = edge1.getStart();
        rescuecore2.misc.geometry.Point2D endPoint1 = edge1.getEnd();
        rescuecore2.misc.geometry.Point2D startPoint2 = edge2.getStart();
        rescuecore2.misc.geometry.Point2D endPoint2 = edge2.getEnd();

        boolean finishedLeft = false;
        boolean finishedRight = false;
        for (MrlEdge edge : road.getMrlEdges()) {
            if (finishedLeft && finishedRight)
                break;
            for (MrlEdge ed : road.getMrlEdges()) {
                if (finishedLeft && finishedRight)
                    break;
                if (ed.equals(edge1) || ed.equals(edge2)) {
                    continue;
                }
                if (startPoint1.equals(startPoint2) || startPoint1.equals(endPoint2)) {
                    finishedLeft = true;
                }
                if (endPoint1.equals(startPoint2) || endPoint1.equals(endPoint2)) {
                    finishedRight = true;
                }

                if (ed.getStart().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
                    startPoint1 = ed.getEnd();
                    if (!justImPassable || !ed.isPassable())
                        leftSideEdges.add(ed);
                    continue;
                }
                if (ed.getEnd().equals(startPoint1) && !finishedLeft && !leftSideEdges.contains(ed)) {
                    startPoint1 = ed.getStart();
                    if (!justImPassable || !ed.isPassable())
                        leftSideEdges.add(ed);
                    continue;
                }
                if (ed.getStart().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
                    endPoint1 = ed.getEnd();
                    if (!justImPassable || !ed.isPassable())
                        rightSideEdges.add(ed);
                    continue;
                }
                if (ed.getEnd().equals(endPoint1) && !finishedRight && !rightSideEdges.contains(ed)) {
                    endPoint1 = ed.getStart();
                    if (!justImPassable || !ed.isPassable())
                        rightSideEdges.add(ed);
                    continue;
                }
            }
        }
        return new Pair<List<MrlEdge>, List<MrlEdge>>(leftSideEdges, rightSideEdges);
    }

    public Set<EntityID> getReachableNeighbours(Area area) {
        Set<EntityID> reachableNeighbours = new HashSet<EntityID>();
        if (area == null)
            return reachableNeighbours;
        if (area instanceof Road) {
            MrlRoad mrlRoad = world.getMrlRoad(area.getID());
            if (mrlRoad.getOpenEdges().isEmpty()) {
                return reachableNeighbours;
            }
        }
        for (EntityID neighbourID : area.getNeighbours()) {
            Area neighbour = world.getEntity(neighbourID, Area.class);
            if (neighbour instanceof Road) {
                MrlRoad road = world.getMrlRoad(neighbour.getID());
                for (MrlEdge mrlEdge : road.getMrlEdgesTo(area.getID())) {
                    if (!mrlEdge.isBlocked()) {
                        reachableNeighbours.add(neighbourID);
                        break;
                    }
                }
            } else {
                if (area instanceof Road) {
                    MrlRoad road = world.getMrlRoad(area.getID());
                    for (MrlEdge mrlEdge : road.getMrlEdgesTo(neighbourID)) {
                        if (!mrlEdge.isBlocked()) {
                            reachableNeighbours.add(neighbourID);
                            break;
                        }
                    }
                } else {
                    reachableNeighbours.add(neighbourID);
                }
            }
        }
        return reachableNeighbours;
    }
//

    /**
     * in method 1 list Edge haye beyne 2ta AREA ro bar migardoone
     *
     * @param area1 area_e avali
     * @param area2 areaei ke mikhaeim edge beine un va areae avali(area1) ro hesab konim
     * @return listi az edge haye beine in 2 (in edge ha baraye area1 hastand yani neighbour_shoon area2 hast)...
     */
    public Set<Edge> getEdgesBetween(Area area1, Area area2) {
        Set<Edge> edgesBetween = new HashSet<Edge>();
        if (!area1.getNeighbours().contains(area2.getID()))
            return edgesBetween;
        for (Edge edge : area1.getEdges()) {
            if (edge.isPassable() && edge.getNeighbour().equals(area2.getID())) {
                edgesBetween.add(edge);
            }
        }
        return edgesBetween;
    }

//    public void updateBlockadesValue(MrlRoad road , MrlEdge from , MrlEdge to){
//        if(!road.isBlockadesDefined()){
//            return;
//        }
//
//        Pair<List<MrlEdge>,List<MrlEdge>> edgesBetween = getEdgesBetween(road,from,to,false);
//        for(MrlBlockade blockade : road.getMrlBlockades()){
//            if(blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)){
//                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
//                continue;
//            }
//
//            if(Util.containsEach(blockade.getBlockedEdges(),edgesBetween.first()) &&
//                    Util.containsEach(blockade.getBlockedEdges(),edgesBetween.second())){
//                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
//            }
//        }
//
//        for(int i=0 ; i<road.getMrlBlockades().size()-1 ; i++){
//            List<MrlEdge> blockedEdges = new ArrayList<MrlEdge>();
//            MrlBlockade blockade1 = road.getMrlBlockades().get(i);
//            if(blockade1.getValue().equals(BlockadeValue.VERY_IMPORTANT))
//                continue;
//            blockedEdges.addAll(blockade1.getBlockedEdges());
//            for (int j=i+1;j<road.getMrlBlockades().size();j++){
//                MrlBlockade blockade2 = road.getMrlBlockades().get(j);
//                if(blockade2.getValue().equals(BlockadeValue.VERY_IMPORTANT))
//                    continue;
//                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if(Util.distance(blockade1.getPolygon(),blockade2.getPolygon())<MRLConstants.AGENT_PASSING_THRESHOLD){
//                    if(Util.containsEach(blockedEdges,edgesBetween.first()) &&
//                            Util.containsEach(blockedEdges,edgesBetween.second())){
//                        blockade1.setValue(BlockadeValue.IMPORTANT);
//                        blockade2.setValue(BlockadeValue.IMPORTANT);
//                    }
//                }
//            }
//        }
//
//        rescuecore2.misc.geometry.Line2D myEdgeLine = new rescuecore2.misc.geometry.Line2D(from.getMiddle(),to.getMiddle());
//        for(MrlBlockade blockade : road.getMrlBlockades()){
//            if(blockade.getValue().equals(BlockadeValue.WORTHLESS)){
//                if(Util.intersections(blockade.getPolygon(),myEdgeLine).size()>0){
//                    blockade.setValue(BlockadeValue.ORNERY);
//                }
//            }
//        }
//
//
//    }


}
