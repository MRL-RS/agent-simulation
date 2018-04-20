package mrl.world.routing.pathPlanner;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForce;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBlockade;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlEdge;
import mrl.world.object.MrlRoad;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : Mahdi Taherian
 *         Date: 5/12/12
 *         Time: 12:26 PM
 */
public class EscapeHere implements MRLConstants {

    private MrlWorld world;
    private PathPlanner pathPlanner;
    private RoadHelper roadHelper;
    private MrlPlatoonAgent platoonAgent;
    private EntityID lastPositionID;
    private Point lastPositionCoordinate;
    private int stuckThreshold;
    private Area target;
    private EntityID nextArea;
    private EntityID position;
    private List<EntityID> planMove;
    private List<Pair<Integer, Integer>> escapeCoordination;
    private EscapeState escapeState = EscapeState.DEFAULT;
    private List<EntityID> newPlanMove;
    private EntityID lastStuckAreaID;
    private EntityID lastStuckRoad;
    private int tryCount;
    private EntityID lastTarget;
    private int lastStuckTime;
    int cf = 1;
    private int moveDistance;


    public EscapeHere(PathPlanner pathPlanner) {
        this.pathPlanner = pathPlanner;
        this.world = pathPlanner.getWorld();
        this.roadHelper = this.world.getHelper(RoadHelper.class);
        this.platoonAgent = world.getPlatoonAgent();
        this.lastPositionID = world.getSelfPosition().getID();
        this.lastPositionCoordinate = new Point(world.getSelfLocation().first(), world.getSelfLocation().second());
        this.stuckThreshold = 2000;
        this.planMove = new ArrayList<EntityID>();
        this.escapeCoordination = new ArrayList<Pair<Integer, Integer>>();
        this.lastStuckAreaID = new EntityID(0);
        this.lastStuckRoad = new EntityID(0);
        tryCount = 0;
        lastStuckTime = -1;
        lastTarget = null;
    }

    private boolean isLockedByBlockade() {
        boolean isLocked = false;
        if (world.getSelfPosition() instanceof Building) {
            return false;
        }
        if (Util.isOnBlockade(world) /*|| isNearBlockade()*/)
            isLocked = true;

        return isLocked;
    }

    public boolean amIMotionLess() {
        position = world.getSelfPosition().getID();

        return setBuried() ||
                (!pathPlanner.getLastMovePlan().isEmpty() && !pathPlanner.getLastMovePlan().contains(position)) ||
                escapeState.equals(EscapeState.ESCAPE_BY_COORDINATION) ||
                stuckCondition();
    }

    /**
     * moshakhas mikonad ke aya agent bi harekat boode ya kheir
     *
     * @return true age agent bi harekat bashe...
     */
    private boolean stuckCondition() {
        if (pathPlanner.lastMoveTime < 2)
            return false;
        if (pathPlanner.isThisCycleMoveToPoint()) {
            return false;
        }
        if (pathPlanner.getLastMovePlan().size() > 0) {
            EntityID target = pathPlanner.getLastMovePlan().get(pathPlanner.getLastMovePlan().size() - 1);
            if (target.equals(position)) {
                return false;
            }
        }

        Point positionCoordinate = new Point(world.getSelfLocation().first(), world.getSelfLocation().second());
        moveDistance = Util.distance(lastPositionCoordinate, positionCoordinate);
        if (moveDistance <= stuckThreshold) {
            return true;
        }
        lastPositionID = position;
        lastPositionCoordinate = positionCoordinate;
        return false;
    }

    public EscapeState escape(Area target) throws CommandException, TimeOutException {
        world.getPlatoonAgent().isThinkTimeOver("escape(Area target)");
        position = world.getSelfPosition().getID();
        if (target == null) {
            return EscapeState.DEFAULT;
        }
        if (escapeState.equals(EscapeState.BURIED)) {
            return escapeState;
        }
        StandardEntity positionEntity = world.getSelfPosition();
        if (escapeCoordination.size() > 0) {
            moveOnCoordination();
        }
        if (!canIEscapeHere() || tryCount > 10) {
            if (DEBUG_PLANNER) {
                world.printData("I AM STUCK HERE: " + position + " (EscapeHere.class)");
            }
//            platoonAgent.setStuck(true);
            if (!lastStuckAreaID.equals(position)) {
                tryCount = 0;
            }
            escapeState = EscapeState.STUCK;
            return escapeState;
        }
        if (world.getTime() > lastStuckTime && world.getTime() - lastStuckTime < 5 && lastStuckAreaID.equals(position) || moveDistance <= stuckThreshold) {
            tryCount++;
        } else {
            lastTarget = target.getID();
            tryCount = 0;
            cf = 1;
            lastStuckAreaID = position;
        }
        lastStuckTime = world.getTime();
        escapeState = EscapeState.DEFAULT;
        this.target = target;
        if (tryCount == 3 || tryCount == 4) {
            MrlRoad mrlRoad;
            for (Road road : world.getRoadsSeen()) {
                if (planMove.contains(road.getID())) {
                    mrlRoad = world.getMrlRoad(road.getID());
                    mrlRoad.update();
                    mrlRoad.addNeighboursBlockades();
                    roadHelper.updatePassably(mrlRoad);
                }
            }
        }

        if (tryCount >= 8) {
            EntityID previousPosition = pathPlanner.getPreviousPosition();
            if (tryCount < 10 && previousPosition != null) {
                escapeState = EscapeState.ROLL_BACK;
                return escapeState;
            }
            if (tryCount == 10) {
                platoonAgent.setHardWalking(true);
                escapeState = EscapeState.HARD_WALKING;
                return escapeState;
            }
        }

        platoonAgent.setHardWalking(false);
        planMove = pathPlanner.getLastMovePlan();
        newPlanMove = pathPlanner.planMove(target, IN_TARGET, platoonAgent instanceof MrlPoliceForce);
        if (newPlanMove.isEmpty()) {
            return EscapeState.UNREACHABLE;
        }

        if ((planMove.isEmpty() || (newPlanMove.size() != planMove.size() || !planMove.containsAll(newPlanMove))) && planMove.contains(position)) {
            return EscapeState.TRY_NEW_A_STAR;
        }

        nextArea = getNextPosition();
        if (positionEntity instanceof Road) {
            escape((Road) positionEntity);
        } else {
            if (positionEntity instanceof Building) {
                escape((Building) positionEntity);
            }
        }
        if (escapeState.equals(EscapeState.ESCAPE_BY_COORDINATION)) {
            moveOnCoordination();
        }

        if (tryCount >= 2) {
            //try to move to target that have no escape result...
            return EscapeState.UNREACHABLE;
        }

        return EscapeState.FAILED;
    }

    public List<EntityID> newPlanMove() {
        return newPlanMove;
    }

    /**
     * This method shows the number of failed tris to reach a specific target
     *
     * @return number of fails to move
     */
    public int getTryCount() {
        return tryCount;
    }

    private EntityID getNextPosition() {

        if (newPlanMove.size() == 0)
            return null;
        int posIndex = newPlanMove.indexOf(position);
        if (posIndex + 1 == newPlanMove.size()) {
            return newPlanMove.get(posIndex);
        }
        return newPlanMove.get(posIndex + 1);
    }

    /**
     * agar agent dar dakhele yek building gir karde bood bayad aval be markaze building move bezane bad be noghteei kharej az entrance.
     *
     * @param building building that agent is stuck in it.
     */
    private void escape(Building building) {
        escapeCoordination.clear();
        MrlBuilding mrlBuilding = world.getMrlBuilding(building.getID());
        if (!mrlBuilding.isOneEntranceOpen(world)) {
            escapeState = EscapeState.STUCK;
            return;
        }
        if (nextArea != null && world.getEntity(position, Area.class).getNeighbours().contains(nextArea)) {
            //Building Center Coordination
            Point2D escapePoint = Util.getPoint(world.getEntity(position, Area.class).getLocation(world));
//            escapeCoordination.add(new Pair<Integer, Integer>((int) escapePoint.getX(), (int) escapePoint.getY()));

            //Add Building Entrance Coordination
            Point2D edgeMiddle = null;
            if (world.getEntity(nextArea, Area.class) instanceof Road) {
                MrlRoad mrlRoad = world.getMrlRoad(nextArea);
                for (MrlEdge mrlEdge : mrlRoad.getMrlEdgesTo(position)) {
                    if (mrlEdge.isPassable()
                            && !mrlEdge.isBlocked()
                            && (platoonAgent.isHardWalking() ? Util.lineLength(mrlEdge.getOpenPart()) > AGENT_MINIMUM_PASSING_THRESHOLD :
                            Util.lineLength(mrlEdge.getOpenPart()) > AGENT_PASSING_THRESHOLD)) {
                        edgeMiddle = Util.getMiddle(mrlEdge.getOpenPart());
                        break;
                    }
                }
            }
            if (edgeMiddle == null) {
                Edge edge = world.getEntity(position, Area.class).getEdgeTo(nextArea);
                edgeMiddle = Util.getMiddle(edge.getLine());
            }
            if (lastStuckAreaID.equals(position)) {
                cf += 2;
            }
//            world.printData("Escape Coefficient = " + cf);
            Line2D escapeLine = Util.improveLine(new Line2D(escapePoint, edgeMiddle), -MRLConstants.AGENT_SIZE);
            escapePoint = escapeLine.getEndPoint();
            escapeCoordination.add(new Pair<Integer, Integer>((int) escapePoint.getX(), (int) escapePoint.getY()));
            escapeLine = Util.improveLine(new Line2D(escapePoint, edgeMiddle), MRLConstants.AGENT_SIZE * cf);
            escapePoint = escapeLine.getEndPoint();
            escapeCoordination.add(new Pair<Integer, Integer>((int) escapePoint.getX(), (int) escapePoint.getY()));

            escapeState = EscapeState.ESCAPE_BY_COORDINATION;
        }
    }

    /**
     * agar target dashtim va rahi ham baraye residan be un dashtim bayad betoonim be un beresim
     * hal age blockadi mane az residan be target shod bayad azash oboor konim
     * be in soorat ke ghesmate baze masir ro peida mikonim bad be markaze un move mizanim
     *
     * @param road the road that agent is stuck in it.
     */
    private void escape(Road road) {
        MrlRoad mrlRoad = world.getMrlRoad(road.getID());
        escapeCoordination.clear();
        int index = newPlanMove.indexOf(mrlRoad.getID());
        if (index < 0 || newPlanMove.size() < 2) {
            world.printData("nextArea == null || index < 0 || newPlanMove.size() < 2");
            return;
        }
        EntityID destinationID = newPlanMove.get(index + 1);
        Pair<Integer, Integer> pointInOpenPart = getPointInOpenPart(mrlRoad, destinationID);
        if (pointInOpenPart != null) {
            escapeCoordination.add(pointInOpenPart);
            escapeState = EscapeState.ESCAPE_BY_COORDINATION;
        } else {
            if (DEBUG_PLANNER) {
                world.printData("pointInOpenPart == null");
            }
        }
        if (escapeCoordination.isEmpty()) {
            Point2D targetPoint = getEscapeBlockadePoint();
            if (targetPoint != null) {
                escapeCoordination.add(new Pair<Integer, Integer>((int) targetPoint.getX(), (int) targetPoint.getY()));
            } else {
                if (DEBUG_PLANNER) {
                    world.printData("targetPoint == null");
                }
            }
        }
        lastStuckRoad = position;
    }

    private Pair<Integer, Integer> getPointInOpenPart(MrlRoad mrlRoad, EntityID destinationID) {
        for (MrlEdge mrlEdge : mrlRoad.getMrlEdgesTo(destinationID)) {
//            Node node = graph.getNode(mrlEdge.getMiddle());
            if (!mrlEdge.isPassable() || mrlEdge.isBlocked() /*|| node == null || !node.isPassable()*/) {
                continue;
            }
            //be center ghesmate baze Edge + agent size be samte jolo move to point mizanim..
            Point2D escapePoint = Util.getMiddle(mrlEdge.getOpenPart());
            Point2D location = Util.getPoint(world.getSelfLocation());

            double m = Util.slope(mrlEdge.getOpenPart());// edge slope
            double x = 0;
            double y = 0;
            if (Double.isInfinite(m) || m >= Double.MAX_VALUE / 2) {// if line slope is infinity or close it
                x = location.getX(); // line equation (default y ~> 0)
            } else {
                y = location.getY() - m * location.getX(); // line equation (default x ~> 0)
            }
            Point2D p1 = Util.closestPoint(new Line2D(location, new Point2D(x, y)), new Point2D(escapePoint.getX(), escapePoint.getY()));
            Point2D parallelPoint = new Point2D(p1.getX(), p1.getY());
            Line2D improved = Util.improveLine(new Line2D(parallelPoint, escapePoint), 500);

//            Line2D improved = Util.improveLine(new Line2D(Util.getPoint(world.getSelfLocation()), escapePoint), 400 * (cf - 1));
//            cf += 1;
            return new Pair<Integer, Integer>((int) improved.getEndPoint().getX(), (int) improved.getEndPoint().getY());
        }
        return null;
    }

    /**
     * vaghti yek listi az mokhtasat baraye move dashtim in tabe doone doone az aval beheshoon move mizane
     *
     * @throws CommandException move command may throw an command exception
     */
    private void moveOnCoordination() throws CommandException {
        Pair<Integer, Integer> escape = escapeCoordination.remove(0);
        if (escapeCoordination.size() == 0) {
            escapeState = EscapeState.DEFAULT;
        }
        pathPlanner.moveToPoint(position, escape.first(), escape.second());
    }

    /**
     * age agente gheire police buried bood ya rooye blockade oftade bood ya tamame neighbour haye un
     * edge passableshoon be positione feli baste bood, nemitoonim kharej shim...
     *
     * @return true agar ma ghableiate kharej shodan az positione felimoon ro dashte bashim...
     */
    private boolean canIEscapeHere() {
        return platoonAgent instanceof MrlPoliceForce || !isLockedByBlockade() && !isIsolated();
    }

    /**
     * hesab mikonim ke aya in positioni ke toosh hastim neighbour hash baste hast ya kheir
     * age hameye neighbour hash baste boodand migim in position isole hastesh...
     * agar khode road ham tamame edge hash baste bashe isole mishe
     *
     * @return true agar neighboure ghabele oboori dashte bashim
     */
    private boolean isIsolated() {
        return roadHelper.getReachableNeighbours(world.getEntity(position, Area.class)).size() == 0;
    }

    private boolean setBuried() {
        boolean isBuried = (world.getSelfHuman().isBuriednessDefined() && world.getSelfHuman().getBuriedness() > 0);
        if (isBuried)
            escapeState = EscapeState.BURIED;
        return isBuried;
    }

    /**
     * this function calculate and return point helping agent to escaping blockade obstacle.
     *
     * @return a rescueCore2 point2d for move agent in it.
     */
    private Point2D getEscapeBlockadePoint() {
        Area positionArea = world.getEntity(position, Area.class);
        Area nextArea = world.getEntity(this.nextArea, Area.class);
        if ((positionArea instanceof Road) && !positionArea.getBlockades().isEmpty()) {
            MrlRoad mrlRoad = world.getMrlRoad(position);
            Point2D p1 = Util.getPoint(world.getSelfLocation());
            Point2D p2 = Util.getMiddle(mrlRoad.getParent().getEdgeTo(nextArea.getID()).getLine());
            p2 = mrlRoad.getEdgeInPoint(p2).getMiddle();
            Line2D line2D = new Line2D(p1, p2);
            Point2D escapePoint = getPointToEscapeBlockade(mrlRoad, line2D);
            if (escapePoint == null) {
                world.printData("nearestLine is null. it means no obstacle blocked my way! so whats the problem???? Road:" + positionArea + " next area:" + nextArea);
            }
            return escapePoint;
        } else if ((nextArea instanceof Road)) {
            MrlRoad mrlRoad = world.getMrlRoad(nextArea.getID());
            Edge edge = mrlRoad.getParent().getEdgeTo(position);
            if (edge == null) {
                world.printData("between " + position + " and " + nextArea.getID() + " is no edge!!!!");
                return null;
            }
            Point2D p1 = Util.getPoint(world.getSelfLocation());
            Point2D p2 = Util.getMiddle(edge.getLine());
            p2 = mrlRoad.getEdgeInPoint(p2).getMiddle();
            Line2D line2D = new Line2D(p1, p2);
            Point2D escapePoint = getPointToEscapeBlockade(mrlRoad, line2D);
            if (escapePoint == null) {
                world.printData("nearestLine is null. it means no obstacle blocked my way! so whats the problem???? Road:" + positionArea + " next area:" + nextArea);
            }
            return escapePoint;
        }
        return null;
    }

    private Point2D getPointToEscapeBlockade(MrlRoad mrlRoad, Line2D line2D) {
        Line2D nearestLine = null;
        List<Line2D> intersectLines;
        double minDistance = Double.MAX_VALUE;
        for (MrlBlockade mrlBlockade : mrlRoad.getMrlBlockades()) {
            intersectLines = Util.getIntersectionLines(mrlBlockade.getPolygon(), line2D);
            for (Line2D line : intersectLines) {
                double distance = java.awt.geom.Line2D.ptSegDist(line.getOrigin().getX(), line.getOrigin().getY(), line.getEndPoint().getX(), line.getEndPoint().getY(), line2D.getOrigin().getX(), line2D.getOrigin().getY());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestLine = line;
                }
            }
        }
        if (nearestLine != null) {
            Line2D improvedLine = Util.improveLine(new Line2D(line2D.getOrigin(), nearestLine.getOrigin()), MRLConstants.AGENT_SIZE / 2);
            return improvedLine.getEndPoint();
        } else {
            return Util.improveLine(line2D, MRLConstants.AGENT_SIZE / 2).getEndPoint();
        }
    }

    public void setTryCount(int tryCount) {
        this.tryCount = tryCount;
    }
}
