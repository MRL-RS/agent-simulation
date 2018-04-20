package mrl.world.object;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.firebrigade.tools.MrlRay;
import mrl.helper.EdgeHelper;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlCentre;
import mrl.police.MrlPoliceForce;
import mrl.util.PolygonUtil;
import mrl.world.MrlWorld;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.graph.MyEdge;
import mrl.world.routing.graph.Node;
import mrl.world.routing.path.Path;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by: Mahdi Taherian
 * User: mrl
 * Date: 5/17/12
 * Time: 8:25 PM
 */
public class MrlRoad {

    private Map<EntityID, List<Polygon>> buildingVisitableParts;
    private Map<MrlEdge, HashSet<MrlEdge>> reachableEdges;
    private Set<MrlBlockade> veryImportantBlockades;
    private Set<MrlBlockade> importantBlockades;
    private List<MrlBlockade> mrlBlockades;
    private List<MrlEdge> passableMrlEdges;
    private Set<MrlEdge> blockedEdges;
    private Polygon transformedPolygon;
    private HashSet<MrlEdge> openEdges;
    private List<Point2D> apexPoints;
    private List<MrlRoad> childRoads;
    private List<MrlEdge> mrlEdges;
    private Set<MrlBlockade> farNeighbourBlockades;
    private boolean isReachable;
    private int totalRepairCost;
    private boolean isPassable;
    private int lastSeenTime;
    private int lastUpdateTime;
    private int lastResetTime;
    private List<Edge> edges;
    private List<Path> paths;
    private Polygon polygon;
    private int groundArea;
    private boolean highway;
    private boolean freeway;
    private MrlWorld world;
    private boolean isSeen;
    private int repairTime;
    private Road parent;
    private Set<EntityID> visibleFrom;
    private List<EntityID> observableAreas;
    private List<MrlRay> lineOfSight;
    private List<MrlBuilding> buildingsInExtinguishRange;

    public MrlRoad(Road road, MrlWorld world, List<MrlEdge> mrlEdges) {
        initialize(road, world, mrlEdges);
    }

    public MrlRoad(Road road, MrlWorld world) {
        this.parent = road;
        initialize(road, world, createMrlEdges(road.getEdges()));
    }

    private void initialize(Road road, MrlWorld world, List<MrlEdge> mrlEdges) {
        setVisibleFrom(new FastSet<EntityID>());
        setObservableAreas(new ArrayList<EntityID>());
        this.highway = false;
        this.freeway = false;
        this.parent = road;
        this.world = world;
        paths = new ArrayList<Path>();
        this.edges = new ArrayList<Edge>(road.getEdges());
        this.apexPoints = new ArrayList<Point2D>();
        this.mrlBlockades = new ArrayList<MrlBlockade>();
        this.childRoads = new ArrayList<MrlRoad>();
        this.blockedEdges = new HashSet<MrlEdge>();
        this.reachableEdges = new FastMap<MrlEdge, HashSet<MrlEdge>>();
        this.isPassable = true;
        this.isReachable = true;
        this.lastSeenTime = 0;
        this.totalRepairCost = 0;
        this.repairTime = 0;
        lastResetTime = 0;
        farNeighbourBlockades = new HashSet<MrlBlockade>();
        lastUpdateTime = 0;
        this.importantBlockades = new HashSet<MrlBlockade>();
        this.veryImportantBlockades = new HashSet<MrlBlockade>();
        passableMrlEdges = new ArrayList<MrlEdge>();
        this.buildingVisitableParts = new HashMap<EntityID, List<Polygon>>();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable()) {
                passableMrlEdges.add(mrlEdge);
            }
        }
        setSeen(false);
        for (Path p : world.getPaths()) {
            if (p.contains(road)) {
                paths.add(p);
            }
        }
        setMrlEdges(mrlEdges);
        this.openEdges = new HashSet<MrlEdge>(mrlEdges);
        resetReachableEdges();

        MrlPersonalData.VIEWER_DATA.setMrlRoadMap(world.getPlatoonAgent(), road.getID(), this);
    }

    public void update() {
        reset();
        setMrlBlockades();
//        setBlockadesFromViewer();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable()) {
//                if (mrlEdge.isTooSmall()) {
//                    mrlEdge.setBlocked(true);
//                }
                mrlEdge.setOpenPart(mrlEdge.getLine());
                List<MrlBlockade> blockedStart = new ArrayList<MrlBlockade>();
                List<MrlBlockade> blockedEnd = new ArrayList<MrlBlockade>();
                for (MrlBlockade mrlBlockade : mrlBlockades) {
                    if (world.getPlatoonAgent() != null && world.getPlatoonAgent().isHardWalking()) {
                        if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getStart()) < MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
                            blockedStart.add(mrlBlockade);
                        }
                        if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getEnd()) < MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
                            blockedEnd.add(mrlBlockade);
                        }
                    } else {
                        if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getStart()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                            blockedStart.add(mrlBlockade);
                        }
                        if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getEnd()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                            blockedEnd.add(mrlBlockade);
                        }
                    }
//                    setMrlEdgeOpenPart(mrlEdge, mrlBlockade);
                }
                setMrlEdgeOpenPart(mrlEdge);
                if (mrlBlockades.size() == 1) {
                    if (Util.containsEach(blockedEnd, blockedStart)) {
                        mrlBlockades.get(0).addBlockedEdges(mrlEdge);
                        mrlEdge.setBlocked(true);
                        mrlEdge.setAbsolutelyBlocked(true);
                    }
                } else {
                    for (MrlBlockade block1 : blockedStart) {
                        for (MrlBlockade block2 : blockedEnd) {
//                            double distance = Util.distance(block1.getPolygon(), block2.getPolygon());
                            if (world.getPlatoonAgent() != null && world.getPlatoonAgent().isHardWalking()) {
                                if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD)) {
                                    mrlEdge.setAbsolutelyBlocked(true);
                                    block1.addBlockedEdges(mrlEdge);
                                    block2.addBlockedEdges(mrlEdge);
                                }
                            } else {
                                if (Util.isPassable(block1.getPolygon(), block2.getPolygon(), MRLConstants.AGENT_PASSING_THRESHOLD)) {
                                    mrlEdge.setBlocked(true);
                                    block1.addBlockedEdges(mrlEdge);
                                    block2.addBlockedEdges(mrlEdge);
                                }
                            }
                        }
                    }
                }
                if (mrlEdge.isBlocked()) {
                    blockedEdges.add(mrlEdge);
                }
                isPassable = getReachableEdges(mrlEdge) != null && !getReachableEdges(mrlEdge).isEmpty();
            } else {
                for (MrlBlockade mrlBlockade : mrlBlockades) {
                    double distance = Util.distance(mrlEdge.getLine(), mrlBlockade.getPolygon());
                    if (world.getPlatoonAgent() != null && world.getPlatoonAgent().isHardWalking()) {
                        if (distance < MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD) {
                            mrlEdge.setAbsolutelyBlocked(true);
                            mrlBlockade.addBlockedEdges(mrlEdge);
                        }
                    } else {
                        if (world.getPlatoonAgent() != null && world.getPlatoonAgent().isHardWalking() ? distance < MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD : distance < MRLConstants.AGENT_PASSING_THRESHOLD) {
                            mrlEdge.setBlocked(true);
                            mrlBlockade.addBlockedEdges(mrlEdge);
                        }
                    }
                }
            }
//            boolean isOtherSideBlocked = mrlEdge.isOtherSideBlocked(world);
//            if (isOtherSideBlocked) {
//                mrlEdge.setBlocked(true);
//            }
        }

        //check too small edge passably
        checkTooSmallEdgesPassably();
        if (world.getPlatoonAgent() != null) {
            for (MrlEdge mrlEdge : passableMrlEdges) {
                //for edges that not blocked each side separately but each side blocked other one.
                if (!mrlEdge.isBlocked() && !mrlEdge.isTooSmall()) {
                    if (Util.lineLength(mrlEdge.getOpenPart()) < (world.getPlatoonAgent().isHardWalking() ? MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD : MRLConstants.AGENT_PASSING_THRESHOLD)) {
                        blockedEdges.add(mrlEdge);
                        mrlEdge.setBlocked(true);
                    }
                }
            }
        }

        updateRepairCost();
        openEdges.removeAll(blockedEdges);
        if (world.getPlatoonAgent() != null && world.getPlatoonAgent() instanceof MrlPoliceForce) {
//            updateBlockadesValue();
        } else {
            updateNodesPassably();
        }
        lastUpdateTime = world.getTime();
    }

    private void checkTooSmallEdgesPassably() {

        for (MrlEdge mrlEdge : passableMrlEdges) {
            if (mrlEdge.isTooSmall()) {
                Set<EntityID> neighbours = new HashSet<EntityID>(parent.getNeighbours());
                neighbours.addAll(world.getEntity(mrlEdge.getNeighbours().second(), Area.class).getNeighbours());
                MrlRoad mrlRoad;
                FOR1:
                for (EntityID neighbourID : neighbours) {
                    mrlRoad = world.getMrlRoad(neighbourID);
                    if (mrlRoad != null) {
                        for (MrlBlockade mrlBlockade : mrlRoad.getMrlBlockades()) {
                            if (Util.distance(mrlBlockade.getPolygon(), mrlEdge.getMiddle()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                                blockedEdges.add(mrlEdge);
                                mrlEdge.setBlocked(true);
                                break FOR1;
                            }
                        }
                    }
                }
            }
        }
    }

    private Set<MrlEdge> getConnectedEdges(MrlEdge mrlEdge) {
        MrlRoad ownerRoad = world.getMrlRoad(mrlEdge.getNeighbours().first());
        MrlRoad neighbourRoad = world.getMrlRoad(mrlEdge.getNeighbours().second());
        Set<MrlEdge> connectedEdges = new HashSet<MrlEdge>();
        for (MrlEdge edge : ownerRoad.getMrlEdges()) {
            if (mrlEdge.getStart().equals(edge.getStart()) ||
                    mrlEdge.getStart().equals(edge.getEnd()) ||
                    mrlEdge.getEnd().equals(edge.getStart()) ||
                    mrlEdge.getEnd().equals(edge.getEnd())) {
                connectedEdges.add(edge);
            }
        }

        if (neighbourRoad != null) {//if neighbour instance of building...
            for (MrlEdge edge : neighbourRoad.getMrlEdges()) {
                if (mrlEdge.getStart().equals(edge.getStart()) ||
                        mrlEdge.getStart().equals(edge.getEnd()) ||
                        mrlEdge.getEnd().equals(edge.getStart()) ||
                        mrlEdge.getEnd().equals(edge.getEnd())) {
                    connectedEdges.add(edge);
                }
            }
        }

        //todo add other neighbours connected edges..........
        return connectedEdges;
    }


    private void updateNodesPassably() {
        if (world.getPlatoonAgent() == null)
            return;
        Graph graph = world.getPlatoonAgent().getPathPlanner().getGraph();
        for (MrlEdge mrlEdge : passableMrlEdges) {
            Node node = graph.getNode(mrlEdge.getMiddle());
            if (node == null) {
                continue;
            }

            if (mrlEdge.isBlocked() || mrlEdge.isOtherSideBlocked(world)) {
                node.setPassable(false, world.getTime());
            } else {
                node.setPassable(true, world.getTime());
            }
        }
    }

    public void addBuildingVisitableParts(EntityID buildingID, Polygon visitablePartsPolygon) {
        if (!buildingVisitableParts.containsKey(buildingID)) {
            buildingVisitableParts.put(buildingID, new ArrayList<Polygon>());
        }
        buildingVisitableParts.get(buildingID).add(visitablePartsPolygon);
    }

    private void setMrlEdgeOpenPart(MrlEdge mrlEdge) {
        Point2D p1 = null, p2 = null;
        int d1 = 0, d2 = 0;
        for (MrlBlockade mrlBlockade : mrlBlockades) {
            List<Point2D> pointList = Util.getPoint2DList(mrlBlockade.getPolygon().xpoints, mrlBlockade.getPolygon().ypoints);
            List<Point2D> centerPoints = new ArrayList<Point2D>();
            boolean isBlockedStart = false, isBlockedEnd = false;
            for (Point2D point : pointList) {
                if (Util.contains(mrlEdge.getLine(), point, 100)) {
                    if (Util.distance(point, mrlEdge.getLine().getOrigin()) <= 10/*point.equals(mrlEdge.getLine().getOrigin())*/) {
                        isBlockedStart = true;
                    } else if (Util.distance(point, mrlEdge.getLine().getEndPoint()) <= 10/*point.equals(mrlEdge.getLine().getEndPoint())*/) {
                        isBlockedEnd = true;
                    } else {
                        centerPoints.add(point);
                    }
                }
            }

            for (Point2D centerPoint : centerPoints) {
                if (isBlockedEnd && isBlockedStart) {
                    p1 = mrlEdge.getMiddle();
                    p2 = mrlEdge.getMiddle();
                    break;
                } else if (isBlockedEnd) {
                    int dist = Util.distance(centerPoint, mrlEdge.getLine().getEndPoint());
                    if (dist > d2) {
                        p2 = centerPoint;
                        d2 = dist;
                    }
                } else if (isBlockedStart) {
                    int dist = Util.distance(centerPoint, mrlEdge.getLine().getOrigin());
                    if (dist > d1) {
                        p1 = centerPoint;
                        d1 = dist;
                    }
                }
            }
        }
        if (p1 == null) {
            p1 = mrlEdge.getStart();
        }
        if (p2 == null) {
            p2 = mrlEdge.getEnd();
        }
        MrlEdge otherSide = mrlEdge.getOtherSideEdge(world);
        Line2D openPart = new Line2D(p1, p2);
        if (otherSide != null) {
            MrlRoad neighbour = world.getMrlRoad(otherSide.getNeighbours().first());
            if (neighbour.getLastUpdateTime() >= this.lastUpdateTime) {
                Line2D otherSideOpenPart = otherSide.getOpenPart();
                if (Util.lineLength(openPart) < Util.lineLength(otherSideOpenPart)) {
                    mrlEdge.setOpenPart(openPart);
                    otherSide.setOpenPart(openPart);
                } else {
                    mrlEdge.setOpenPart(otherSideOpenPart);
                    otherSide.setOpenPart(otherSideOpenPart);
                }
            } else {
                mrlEdge.setOpenPart(openPart);
            }
        } else {
            mrlEdge.setOpenPart(openPart);
        }
    }

    public HashSet<MrlEdge> getReachableEdges(MrlEdge from) {
        return reachableEdges.get(from);
    }

    public void addReachableEdges(MrlEdge from, MrlEdge to) {
        reachableEdges.get(from).add(to);
        reachableEdges.get(to).add(from);
    }

    public void removeReachableEdges(MrlEdge from, MrlEdge to) {
        reachableEdges.get(from).remove(to);
        reachableEdges.get(to).remove(from);
    }

    private void setApexPoint() {
        apexPoints.clear();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge == null) {
                System.out.println("(MrlRoad.class ==> mrlEdge == null)");
                continue;
            }
            if (!apexPoints.contains(mrlEdge.getStart()))
                apexPoints.add(mrlEdge.getStart());
            else if (!apexPoints.contains(mrlEdge.getEnd()))
                apexPoints.add(mrlEdge.getEnd());
        }
        createPolygon();
        computeGroundArea();
    }

    public List<MrlEdge> getMrlEdgesTo(EntityID neighbourID) {
        List<MrlEdge> mrlEdgeList = new ArrayList<MrlEdge>();
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable() && mrlEdge.getNeighbours().second().equals(neighbourID)) {
                mrlEdgeList.add(mrlEdge);
            }
        }
        return mrlEdgeList;
    }

    private void createPolygon() {
        int count = apexPoints.size();
        int xs[] = new int[count];
        int ys[] = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = (int) apexPoints.get(i).getX();
            ys[i] = (int) apexPoints.get(i).getY();
        }
        polygon = new Polygon(xs, ys, count);
    }

    private void computeGroundArea() {
        double area = GeometryTools2D.computeArea(apexPoints) * MRLConstants.SQ_MM_TO_SQ_M;
        groundArea = (int) Math.abs(area);
    }

    /**
     * ye polygon migirim bad khat be kahte polygon ro ba in road moghayese mikonim(baraye mohasebeye passably)
     * MrlEdge har khat ro migirim va dakhele yek list mirizim
     * hala ba in liste edge ha ye mrlroad misazim ba parente hamin road...
     *
     * @param polygon polygon on the road that we want to convert to another road(useful for split)
     * @return new mrlRoad
     */
    private MrlRoad convertToRoad(Polygon polygon) {
        List<MrlEdge> mrlEdgeList = new ArrayList<MrlEdge>();
        for (int i = 0; i < polygon.npoints; i++) {
            int j = (i + 1) % polygon.npoints;
            Point2D p1 = new Point2D(polygon.xpoints[i], polygon.ypoints[i]);
            Point2D p2 = new Point2D(polygon.xpoints[j], polygon.ypoints[j]);
            Point2D m1 = Util.getMiddle(p1, p2);
            MrlEdge mrlEdgeTemp = getEdgeInPoint(m1);
            MrlEdge mrlEdge;
            if (mrlEdgeTemp != null)
                mrlEdge = new MrlEdge(mrlEdgeTemp.isPassable(), p1, p2);
            else
                mrlEdge = new MrlEdge(true, p1, p2);
            mrlEdgeList.add(mrlEdge);
        }
        return new MrlRoad(parent, world, mrlEdgeList);
    }

    private void resetReachableEdges() {
        HashSet<MrlEdge> edgesInstead;
        for (MrlEdge mrlEdge : mrlEdges) {
            if (mrlEdge.isPassable()) {
                edgesInstead = new HashSet<MrlEdge>(passableMrlEdges);
                edgesInstead.remove(mrlEdge);
                reachableEdges.put(mrlEdge, edgesInstead);
            }
        }
    }

    /**
     * yek point migirim va beine edge haye roademoon iterate mikonim ta bebinim kodoom MrlEdge in noghte ro dare
     * age hich kodoom in noghte ro nadashtand null bar migardoonim
     *
     * @param point point that we want found edge on it
     * @return MrlEdge which point on it.
     */
    public MrlEdge getEdgeInPoint(Point2D point) {
        for (MrlEdge mrlEdge : mrlEdges) {
            if (Util.contains(mrlEdge.getLine(), point, 1.0)) {
                return mrlEdge;
            }
        }
        return null;
    }

    public MrlEdge getMrlEdge(Edge edge) {
        Point2D middle = Util.getPoint(EdgeHelper.getEdgeMiddle(edge));
        return getEdgeInPoint(middle);
    }

    /**
     * aval polygone road ro migirim bad be vasileye tabe split mikonim nesbat be khat
     * hala 2ta polygon darim
     * har polygon ro tabdil be road mikonim va return mikonim
     *
     * @param line2D khate monas'sef
     * @return 2 mrl roade jadid ba parent_e hamin road
     */
    public Pair<MrlRoad, MrlRoad> splitRoad(rescuecore2.misc.geometry.Line2D line2D) {
        Pair<Polygon, Polygon> splitPolygon = PolygonUtil.split(parent.getApexList(), line2D);
        MrlRoad road1 = convertToRoad(splitPolygon.first());
        MrlRoad road2 = convertToRoad(splitPolygon.second());
        childRoads.add(road1);
        childRoads.add(road2);
        return new Pair<MrlRoad, MrlRoad>(road1, road2);
    }

    private void setMrlEdges(List<MrlEdge> edges) {
        mrlEdges = new ArrayList<MrlEdge>();
        mrlEdges.addAll(edges);
        setApexPoint();
    }

    private List<MrlEdge> createMrlEdges(List<Edge> edges) {
        List<MrlEdge> mrlEdges = new ArrayList<MrlEdge>();
        for (Edge edge : edges) {
            mrlEdges.add(new MrlEdge(edge, parent.getID()));
        }
        return mrlEdges;
    }

    public Polygon getPolygon() {
        return polygon;
    }

    public int getGroundArea() {
        return groundArea;
    }

    private void addBlockade(MrlBlockade blockade) {
        mrlBlockades.add(blockade);
    }


    private void setMrlBlockades() {
        totalRepairCost = 0;
        mrlBlockades.clear();
        if (!parent.isBlockadesDefined()) {
            return;
        }
        try {
            StandardEntity entity;
            for (EntityID blockID : parent.getBlockades()) {
                entity = world.getEntity(blockID);
                if (!(entity instanceof Blockade)) {
                    MrlPersonalData.VIEWER_DATA.print(world.getPlatoonAgent().getDebugString() + entity + " is not blockade!!!!!");
                    continue;
                }
                Blockade blockade = (Blockade) entity;
                Polygon blockPolygon = PolygonUtil.retainPolygon(getPolygon(), Util.getPolygon(blockade.getApexes()));
                if (blockPolygon == null) {
                    continue;
                }
                MrlBlockade newBlockade = new MrlBlockade(this, blockade, blockPolygon);
                addBlockade(newBlockade);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //TODO: handle it
//            System.err.println("null exception");
        }
    }

    private void updateRepairCost() {
        totalRepairCost = 0;
        for (MrlBlockade mrlBlockade : mrlBlockades) {
            totalRepairCost += mrlBlockade.getRepairCost();
        }
        int repairRate = world.getConfig().getIntValue("clear.repair.rate");
        repairTime = (int) Math.ceil(totalRepairCost / repairRate);
    }

    private void updateBlockadesValue() {
        if (this.getMrlBlockades().size() == 0) {
            return;
        }
        importantBlockades.clear();
        for (int e1 = 0; e1 < passableMrlEdges.size() - 1; e1++) {
            MrlEdge from = getMrlEdges().get(e1);
            for (int e2 = e1; e2 < passableMrlEdges.size(); e2++) {
                MrlEdge to = getMrlEdges().get(e2);
                updateBlockadesValue(from, to);
            }
        }

    }

    private void updateBlockadesValue(MrlEdge from, MrlEdge to) {
        Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween = world.getHelper(RoadHelper.class).getEdgesBetween(this, from, to, false);
        for (MrlBlockade blockade : this.getMrlBlockades()) {
            blockade.setValue(BlockadeValue.WORTHLESS);
            if (blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)) {
                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
                importantBlockades.add(blockade);
                veryImportantBlockades.add(blockade);
                continue;
            }

            if (Util.containsEach(blockade.getBlockedEdges(), edgesBetween.first()) &&
                    Util.containsEach(blockade.getBlockedEdges(), edgesBetween.second())) {
                blockade.setValue(BlockadeValue.VERY_IMPORTANT);
                veryImportantBlockades.add(blockade);
                importantBlockades.add(blockade);
            }
        }

        for (int i = 0; i < this.getMrlBlockades().size() - 1; i++) {
            List<MrlEdge> blockedEdges = new ArrayList<MrlEdge>();
            MrlBlockade blockade1 = this.getMrlBlockades().get(i);
            if (blockade1.getValue().equals(BlockadeValue.VERY_IMPORTANT))
                continue;
            blockedEdges.addAll(blockade1.getBlockedEdges());
            for (int j = i + 1; j < this.getMrlBlockades().size(); j++) {
                MrlBlockade blockade2 = this.getMrlBlockades().get(j);
                if (blockade2.getValue().equals(BlockadeValue.VERY_IMPORTANT))
                    continue;
                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if (Util.distance(blockade1.getPolygon(), blockade2.getPolygon()) < MRLConstants.AGENT_PASSING_THRESHOLD) {
                if (Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), MRLConstants.AGENT_PASSING_THRESHOLD)) {
                    if (Util.containsEach(blockedEdges, edgesBetween.first()) &&
                            Util.containsEach(blockedEdges, edgesBetween.second())) {
                        if (blockade1.getRepairCost() > blockade2.getRepairCost()) {
                            importantBlockades.add(blockade2);
                            blockade1.setValue(BlockadeValue.IMPORTANT_WITH_HIGH_REPAIR_COST);
                            blockade2.setValue(BlockadeValue.IMPORTANT_WITH_LOW_REPAIR_COST);
                        } else {
                            importantBlockades.add(blockade1);
                            blockade1.setValue(BlockadeValue.IMPORTANT_WITH_LOW_REPAIR_COST);
                            blockade2.setValue(BlockadeValue.IMPORTANT_WITH_HIGH_REPAIR_COST);
                        }
                    }
                }
            }
        }

        rescuecore2.misc.geometry.Line2D myEdgeLine = new rescuecore2.misc.geometry.Line2D(from.getMiddle(), to.getMiddle());
        for (MrlBlockade blockade : this.getMrlBlockades()) {
            if (blockade.getValue().equals(BlockadeValue.WORTHLESS)) {
                if (Util.intersections(blockade.getPolygon(), myEdgeLine).size() > 0) {
                    blockade.setValue(BlockadeValue.ORNERY);
                }
            }
        }
    }

    public Set<MrlBlockade> getObstacles(MrlEdge from, MrlEdge to) {
        Set<MrlBlockade> obstacles = new HashSet<MrlBlockade>();
        Pair<List<MrlEdge>, List<MrlEdge>> edgesBetween = world.getHelper(RoadHelper.class).getEdgesBetween(this, from, to, false);
        for (MrlBlockade blockade : this.getMrlBlockades()) {
            if (blockade.getBlockedEdges().contains(from) || blockade.getBlockedEdges().contains(to)) {
                obstacles.add(blockade);
                continue;
            }

            if (Util.containsEach(blockade.getBlockedEdges(), edgesBetween.first()) &&
                    Util.containsEach(blockade.getBlockedEdges(), edgesBetween.second())) {
                obstacles.add(blockade);
            }
        }

        for (int i = 0; i < this.getMrlBlockades().size() - 1; i++) {
            List<MrlEdge> blockedEdges = new ArrayList<MrlEdge>();
            MrlBlockade blockade1 = this.getMrlBlockades().get(i);
            blockedEdges.addAll(blockade1.getBlockedEdges());
            for (int j = i + 1; j < this.getMrlBlockades().size(); j++) {
                MrlBlockade blockade2 = this.getMrlBlockades().get(j);
                blockedEdges.addAll(blockade2.getBlockedEdges());
//                if (Util.distance(blockade1.getPolygon(), blockade2.getPolygon()) < MRLConstants.AGENT_PASSING_THRESHOLD) {

                if (world.getPlatoonAgent() != null && world.getPlatoonAgent().isHardWalking() ?
                        Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), MRLConstants.AGENT_MINIMUM_PASSING_THRESHOLD) :
                        Util.isPassable(blockade1.getPolygon(), blockade2.getPolygon(), MRLConstants.AGENT_PASSING_THRESHOLD)) {
                    if (Util.containsEach(blockedEdges, edgesBetween.first()) &&
                            Util.containsEach(blockedEdges, edgesBetween.second())) {
                        if (blockade1.getRepairCost() > blockade2.getRepairCost()) {
                            obstacles.add(blockade2);
                        } else {
                            obstacles.add(blockade1);
                        }
                    }
                }
            }
        }
        return obstacles;
    }

    /**
     * agar 1 road 1 modat zamane khassi 2bare dide nashod ya payami dar ertebat ba un naresid reset mishe
     * be in soorat ke tamame yalhaye dakhele un + node haye un passable mishand va
     * edge haye un azx halate block kharej mishand
     * <p/>
     * zamane reset shodan bayad az meghdare repairCost/repair_rate (meghdar zamini ke bara pak kardane road lazeme) +
     * yek meghdare threshold baraye etminan be dast miad
     */
    public void resetOldPassably() {
        if (!isSeen() || world.getPlatoonAgent() == null || world.getPlatoonAgent() instanceof MrlPoliceForce || lastResetTime > lastUpdateTime) {
            return;
        }
        if (isTimeToReset()) {
            reset();
        }
    }

    private boolean isTimeToReset() {
        int resetTime = getRepairTime();
        if (world.isMapHuge()) {
            resetTime += MRLConstants.ROAD_PASSABLY_RESET_TIME_IN_HUGE_MAP;
        } else if (world.isMapMedium()) {
            resetTime += MRLConstants.ROAD_PASSABLY_RESET_TIME_IN_MEDIUM_MAP;
        } else if (world.isMapSmall()) {
            resetTime += MRLConstants.ROAD_PASSABLY_RESET_TIME_IN_SMALL_MAP;

        }
        return lastResetTime <= lastUpdateTime + resetTime && world.getTime() - lastSeenTime > resetTime;
    }

    public void reset() {
        blockedEdges.clear();
        openEdges.addAll(mrlEdges);
        mrlBlockades.clear();
        isPassable = true;
        isReachable = true;
        farNeighbourBlockades.clear();
        if (world.getPlatoonAgent() == null) {
            return;
        }
        Graph graph = world.getPlatoonAgent().getPathPlanner().getGraph();
        for (MrlEdge mrlEdge : mrlEdges) {
            mrlEdge.setBlocked(false);
            mrlEdge.setAbsolutelyBlocked(false);
            MrlEdge otherEdge = mrlEdge.getOtherSideEdge(world);
            mrlEdge.setOpenPart(mrlEdge.getLine());
            if (otherEdge != null) {
                MrlRoad mrlRoad = world.getMrlRoad(mrlEdge.getNeighbours().second());
                if (mrlRoad.getLastUpdateTime() < lastUpdateTime) {
                    //mrlRoad.update();
                    otherEdge.setOpenPart(otherEdge.getLine());
                }
            }
            Area neighbour = world.getEntity(mrlEdge.getNeighbours().second(), Area.class);
            if (mrlEdge.isPassable()) {
                Node node = graph.getNode((mrlEdge.getMiddle()));
                if (node == null) {
                    System.out.println("node == null in " + this.getID());
                    continue;
                }
                if (neighbour instanceof Road) {
                    MrlRoad mrlRoad = world.getMrlRoad(neighbour.getID());
                    MrlEdge neighbourEdge = mrlRoad.getEdgeInPoint(mrlEdge.getMiddle());
                    if (neighbourEdge != null && !neighbourEdge.isBlocked()) {
                        node.setPassable(true, world.getTime());
                    }
                } else {
                    node.setPassable(true, world.getTime());
                }
            }
        }
        resetReachableEdges();
        for (MyEdge myEdge : graph.getMyEdgesInArea(getID())) {
            myEdge.setPassable(true);
        }
        lastResetTime = world.getTime();
    }

    public List<MrlBlockade> getMrlBlockades() {
        return mrlBlockades;
    }

    public boolean isNeedUpdate() {
        if (world.getPlatoonAgent() == null || world.getSelf() instanceof MrlCentre) {
            return false;
        }
        if(!parent.isBlockadesDefined()){
            return true;
        }
        if (world.getPlatoonAgent() instanceof MrlPoliceForce ||
                (parent.getBlockades().size() != getMrlBlockades().size()) ||
                world.getPlatoonAgent().isHardWalking() ||
                lastSeenTime == 0 ||
                lastSeenTime < world.getTime() - 10) {
            return true;
        }
        Blockade blockade;
        for (MrlBlockade mrlBlockade : getMrlBlockades()) {
            blockade = mrlBlockade.getParent();
            if (blockade == null || !parent.getBlockades().contains(mrlBlockade.getParent().getID()) || blockade.getRepairCost() != mrlBlockade.getRepairCost()) {
                return true;
            }
        }
        return false;
    }

    public boolean isHighway() {
        return highway;
    }

    public void setHighway(boolean highway) {
        this.highway = highway;
    }

    /**
     * free way is a kind of road which had a very long passable edge (more than 95% )
     *
     * @return if this road is a freeway return true , otherwise return false
     */
    public boolean isFreeway() {
        return freeway;
    }

    public void setFreeway(boolean isFreeway) {
        freeway = isFreeway;
    }

    public List<Path> getPaths() {
        return paths;
    }

    public void addPath(Path path) {
        this.paths.add(path);
    }

    public List<MrlEdge> getMrlEdges() {
        return mrlEdges;
    }

    public void addNeighboursBlockades() {
        MrlRoad neighbour;
        for (EntityID nID : parent.getNeighbours()) {
            neighbour = world.getMrlRoad(nID);
            if (neighbour != null) {
                for (MrlBlockade mrlBlockade : neighbour.getMrlBlockades()) {
                    if (!farNeighbourBlockades.contains(mrlBlockade)) {
                        if (Util.isPassable(mrlBlockade.getPolygon(), this.getPolygon(), MRLConstants.AGENT_SIZE)) {
                            addBlockade(mrlBlockade);
                        } else {
                            farNeighbourBlockades.add(mrlBlockade);
                        }
                    }
                }
            }
        }
    }

    public Road getParent() {
        return parent;
    }

    public int getLastUpdateTime() {
        return lastUpdateTime;
    }

    public int getLastResetTime() {
        return lastResetTime;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<MrlRoad> getChildRoads() {
        return childRoads;
    }

    public boolean isPassable() {
        return isPassable;
    }

    public Set<MrlEdge> getBlockedEdges() {
        return blockedEdges;
    }

    public HashSet<MrlEdge> getOpenEdges() {
        return openEdges;
    }

    public EntityID getID() {
        return parent.getID();
    }

    public boolean isReachable() {
        return isReachable;
    }

    public void setReachable(boolean reachable) {
        isReachable = reachable;
    }

    public void setSeen(boolean seen) {
        this.isSeen = seen;
    }

    public boolean isSeen() {
        return isSeen;
    }

    public void setLastSeenTime(int lastSeenTime) {
        this.lastSeenTime = lastSeenTime;
    }

    public int getLastSeenTime() {
        return lastSeenTime;
    }

    public int getRepairTime() {
        return repairTime;
    }

    public Set<MrlBlockade> getImportantBlockades() {
        return importantBlockades;
    }

    public Set<MrlBlockade> getVeryImportantBlockades() {
        return veryImportantBlockades;
    }

    /**
     * @return transformed polygon just for viewer
     */
    public Polygon getTransformedPolygon() {
        return transformedPolygon;
    }

    /**
     * transform polygon for viewer
     *
     * @param t viewer screen transform
     */
    public void createTransformedPolygon(ScreenTransform t) {

        int count = apexPoints.size();
        int xs[] = new int[count];
        int ys[] = new int[count];
        int i = 0;
        for (Point2D point2D : apexPoints) {
            xs[i] = t.xToScreen(point2D.getX());
            ys[i] = t.yToScreen(point2D.getY());
            i++;
        }
        transformedPolygon = new Polygon(xs, ys, count);
    }

    public Set<EntityID> getVisibleFrom() {
        return visibleFrom;
    }

    public void setVisibleFrom(Set<EntityID> visibleFrom) {
        this.visibleFrom = visibleFrom;
    }

    public List<EntityID> getObservableAreas() {
        return observableAreas;
    }

    public void setObservableAreas(List<EntityID> observableAreas) {
        this.observableAreas = observableAreas;
    }

    public List<MrlRay> getLineOfSight() {
        return lineOfSight;
    }

    public void setLineOfSight(List<MrlRay> lineOfSight) {
        this.lineOfSight = lineOfSight;
    }

    public List<MrlBuilding> getBuildingsInExtinguishRange() {
        return buildingsInExtinguishRange;
    }

    public void setBuildingsInExtinguishRange(List<MrlBuilding> buildingsInExtinguishRange) {
        this.buildingsInExtinguishRange = buildingsInExtinguishRange;
    }
}
