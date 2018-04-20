package mrl.world.routing.pathPlanner;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.MrlPoliceForce;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBlockade;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlRoad;
import mrl.world.routing.a_star.A_Star;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.pathPlanner.move.RayMoveActExecutor;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * Created by Mostafa Shabani.
 * Date: Sep 22, 2010
 * Time: 4:45:00 PM
 */
public class PathPlanner implements IPathPlanner, MRLConstants {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(PathPlanner.class);

    private MrlWorld world;
    private MrlPlatoonAgent platoonAgent;
    private A_Star a_star;
    private Graph graph;
    private CheckAreaPassable areaPassably;
    public EscapeHere escapeHere;
    public int lastMoveTime = 0;

    private List<EntityID> lastMovePlan = new ArrayList<EntityID>();
    private List<EntityID> previousPath = new ArrayList<EntityID>();
    private Area previousTarget = null;
    private Queue<EntityID> previousPositionsQueue = new LinkedList<EntityID>();
    private int continueMovingTime = 0;

    private int refugeNumberToGetNearest;
    private int hydrantNumberToGetNearest;
    private int nearestAreaPathCost;
    private int tryToReachTargetCount = 0;
    private EntityID prevTarget;
    private int illegalPlanCount;

    public PathPlanner(MrlWorld world) {
        this.world = world;
        this.graph = new Graph(world);
        this.a_star = new A_Star(this);
        prevTarget = null;
        if (world.getSelf() instanceof MrlPlatoonAgent) {
            this.platoonAgent = world.getPlatoonAgent();
            this.areaPassably = new CheckAreaPassable(this);
            this.escapeHere = new EscapeHere(this);
        }

        // baraye inke bebinim be chand refuge bayad A* bezanim baraye be dast avardane nazdiktarin ha.
        int refSize = world.getRefuges().size();
        int hydSize = world.getHydrants().size();
//        if ((refSize % 4) == 0) {
//            refugeNumberToGetNearest = (refSize / 4);
//        } else {
//            refugeNumberToGetNearest = (refSize / 4) + 1;
//        }
        refugeNumberToGetNearest = Math.min(refSize, 10);
        hydrantNumberToGetNearest = Math.min(hydSize, 10);
        illegalPlanCount = 0;
        rayMoveActExecutor = new RayMoveActExecutor(this.world);

    }

    public MrlWorld getWorld() {
        return world;
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public CheckAreaPassable getAreaPassably() {
        return areaPassably;
    }

    @Override
    public boolean move(Area target, int maxDistance, boolean force) throws CommandException {

        setPreviousPosition();
        List<EntityID> planMove = planMove(target, maxDistance, force);
//        if (escapeHere.amIMotionLess()) {
//            EscapeState state = escapeHere.escape(target);
//            if (state.equals(EscapeState.UNREACHABLE)) {
//                setUnreachableTarget(target);
//            }
//            if (state.equals(EscapeState.TRY_NEW_A_STAR)) {
//                tryToReachTargetCount++;
//                planMove = escapeHere.newPlanMove();
//            }
//        }
//        if (planMove.isEmpty() || planMove.size() == 1) {
//            return true;
//        }

        //TODO: added to prevent moving to blocked entrances


        moveOnPlan(planMove);
        return false;
    }

    @Override
    public boolean move(Collection<? extends Area> targets, int maxDistance, boolean force) throws CommandException {
        setPreviousPosition();
        /*if (escapeHere.amIMotionLess() && !targets.isEmpty())
            escapeHere.escape((new ArrayList<Area>(targets)).get(0));*/
        List<EntityID> planMove = planMove(targets, maxDistance, force);
        moveOnPlan(planMove);
        return false;    // if target impassable return false.
    }

    @Override
    public void moveToRefuge() throws CommandException {
        setPreviousPosition();
        Area selfPositionArea = (Area) world.getSelfPosition();
        List<EntityID> refugePath = new ArrayList<EntityID>(getRefugePath(selfPositionArea, false));
        moveOnPlan(refugePath);
    }

    @Override
    public void moveToHydrant() throws CommandException {
        setPreviousPosition();
        Area selfPositionArea = (Area) world.getSelfPosition();
        List<EntityID> hydrantPath = new ArrayList<EntityID>(getHydrantPath(selfPositionArea, false));
        moveOnPlan(hydrantPath);
    }

    private boolean thisCycleMoveToPoint;

    @Override
    public void moveToPoint(EntityID area, int destX, int destY) throws CommandException {

        if (world.getSelfPosition().getID().equals(area)) {
            List<EntityID> list = new ArrayList<EntityID>();
            list.add(area);

            platoonAgent.sendMoveAct(world.getTime(), list, destX, destY);
        } else {
            move((Area) world.getEntity(area), IN_TARGET, false);
        }
    }

    @Override
    public void update() {
    }

    @Override
    public List<EntityID> getLastMovePlan() {
        return lastMovePlan;
    }

    public EntityID getPreviousTarget() {
        return prevTarget;
    }


    @Override
    public int getPathCost() {
        return a_star.getPathCost();
    }

    public List<EntityID> planMove(Area target, int maxDistance, boolean force) {

        List<EntityID> planMove = planMove((Area) world.getSelfPosition(), target, maxDistance, force);

        if (!planMove.isEmpty() && !(platoonAgent instanceof MrlPoliceForce) && !force) {
            StandardEntity entity = world.getEntity(planMove.get(planMove.size() - 1));
            boolean shouldBeRemoved = false;
            if (entity instanceof Road) {
                MrlRoad road = world.getMrlRoad(entity.getID());
                if (world.getRoadsSeen().contains(road.getParent())) {
                    for (MrlBlockade mrlBlockade : road.getMrlBlockades()) {
                        if (mrlBlockade.getPolygon().contains(road.getParent().getX(), road.getParent().getY())
                                || Util.findDistanceTo(mrlBlockade.getParent(), road.getParent().getX(), road.getParent().getY()) <= AGENT_PASSING_THRESHOLD / 2) {
                            shouldBeRemoved = true;
//                            planMove.remove(road.getID());
                            break;
                        }
                    }
                }
            }
            if (shouldBeRemoved) {
                planMove = new ArrayList<EntityID>();
            }
        }


        // bedast avordane masir baraye move.
        return planMove;
    }

    @Override
    public List<EntityID> planMove(Area sourceArea, Area target, int maxDistance, boolean force) {

        List<EntityID> finalAreaPath = new ArrayList<EntityID>();

        if (target == null) {
            return finalAreaPath;
        }
        if (!target.getID().equals(prevTarget)) {
            prevTarget = target.getID();
            tryToReachTargetCount = 0;
        }
        if (sourceArea.equals(target)) {
            Logger.warn("Time:" + world.getTime() + " Already on target move =" + target);
            //finalAreaPath.add(sourceArea.getID());
            return finalAreaPath;
        }
        boolean repeatPlanning = repeatPlanning(target);
        boolean repeatAStar = !isPositionOnPreviousPath(sourceArea.getID());
        if (repeatAStar || repeatPlanning) {
            // tanha zamani ke niaz bashe dobare a* mizane ta masir peida kone.
            // yani vaghti yeki az sharayete bala true bashe.
            if (repeatAStar) {
                tryToReachTargetCount++;
            }
            Area nearestTarget = null;
            previousPath.clear();

            if (maxDistance != IN_TARGET) {
                // agar gharar nabood hatman dar mahale target gharar begirad.
                // ba estefade az maxDistance makan haei ke mitavanad dar aanha gharar begirad ra bedast miavarim.
                nearestTarget = getNearestArea(platoonAgent.getObjectsInRange(target, maxDistance), target);
            }
            if (nearestTarget == null) {
                nearestTarget = target;
            }

            // A* be ma te'dadi entityId mide ke marboot be area haei ke bahas agent azashoon oboor kone.
            finalAreaPath = a_star.getShortestPath(sourceArea, nearestTarget, force);

            previousTarget = nearestTarget;
            previousPath = finalAreaPath;

        } else if (previousTarget.equals(target)) {
            // baraye zamani ke meghdari az masir ra amade ast.
            // az pathe ghabli ta position alan ra hazf mikonim va pathe jadid bedast miayad.
            ArrayList<EntityID> temp = new ArrayList<EntityID>();

            for (EntityID aPreviousPath : previousPath) {
                if (!sourceArea.getID().equals(aPreviousPath)) {
                    temp.add(aPreviousPath);
                } else {
                    break;
                }
            }

            previousPath.removeAll(temp);
            finalAreaPath = previousPath;
        }
        return finalAreaPath;
    }

    @Override
    public int getNearestAreaPathCost() {
        return nearestAreaPathCost;
    }


    private List<EntityID> getAreaTypePath(Area sourceArea, int numberToGetNearest, Class<? extends StandardEntity> areaType, Collection<? extends StandardEntity> validAreas, boolean force) {
        /**
         * aval chand ta az nazdiktarin area haro peyda mikonim.
         * ba'd ba hame oon ha path mizanim.
         * pathe nakdiktarin ro barmigardunim.
         */
        List<EntityID> finalPath = new ArrayList<EntityID>();

        if (validAreas.isEmpty() || areaType.isInstance(sourceArea)) {
            // age area nadashte bashim ke hichi.
            return finalPath;
        }

        if (repeatPlanning(null) || !isPositionOnPreviousPath(sourceArea.getID())) {
            List<EntityID> bestAreaPath = new ArrayList<EntityID>();
            Area bestArea = null;
            int minimumCost = Integer.MAX_VALUE;
            List<EntityID> path;
            int cost;
            previousPath.clear();

            List<Area> nearestAreas;
            // te'dadi az area haye nazdik (az nazare fasele oghlidosi) ro migire.
            nearestAreas = getSomeNearAreaType(sourceArea.getX(), sourceArea.getY(), numberToGetNearest, validAreas);

            for (Area area : nearestAreas) {
                if (area == null) {
                    continue;
                }

                if (sourceArea.equals(area)) {
                    Logger.warn("Time:" + world.getTime() + " Already on target move =" + area);
                    return null;
                }

                // be hameye area haye nazdik path mizane va nazdiktarin ro entekhab mikone.
//                path = a_star.getShortestGraphPath(sourceArea, area, force);
                path = planMove(area, IN_TARGET, force);

                // kamtarin cost ro peyda mikonim.
                if (path.size() != 0) {
                    cost = a_star.getPathCost();

                    if (cost < minimumCost) {
                        minimumCost = cost;
                        bestArea = area;
                        bestAreaPath = new ArrayList<EntityID>(path);
                    }
                } else {
                    setUnreachableTarget(area);
                }
            }

            if (bestArea == null && !nearestAreas.isEmpty()) {
                // age area khubi peyda nakard avalin area ke az nazare oghlidosi nazdike ro entekhab mmikone.
                if (MRLConstants.DEBUG_PLANNER) {
                    world.printData(" true move....");
                }
                bestArea = nearestAreas.get(0);
//                bestAreaPath = a_star.getShortestGraphPath(sourceArea, bestArea, true);
                bestAreaPath = planMove(bestArea, IN_TARGET, force);

            }
            if (bestArea != null) {
                if (MRLConstants.DEBUG_PLANNER) {
                    world.printData("  best:" + bestArea + " -------- nearest area = " + nearestAreas);
                }
                previousTarget = bestArea;
                nearestAreaPathCost = minimumCost;
                previousPath = bestAreaPath;//a_star.getAreaPath(sourceArea.getID(), bestArea.getID(), bestAreaPath);

            }

        } else {
            // age hanooz be area nareside bood area haei ke ta inja umade az path hazf mikone va dobare edame mide.
            ArrayList<EntityID> toRemove = new ArrayList<EntityID>();

            for (EntityID aPreviousPath : previousPath) {
                if (!sourceArea.getID().equals(aPreviousPath)) {
                    toRemove.add(aPreviousPath);
                } else {
                    break;
                }
            }
            if (MRLConstants.DEBUG_PLANNER) {
                world.printData("  continue -------- ");
            }

            previousPath.removeAll(toRemove);
        }

        finalPath = previousPath;
        return finalPath;
    }

    @Override
    public List<EntityID> getRefugePath(Area sourceArea, boolean force) {
        return getAreaTypePath(sourceArea, refugeNumberToGetNearest, Refuge.class, world.getRefuges(), force);
    }

    public List<EntityID> getHydrantPath(Area sourceArea, boolean force) {
        return getAreaTypePath(sourceArea, hydrantNumberToGetNearest, Hydrant.class, world.getAvailableHydrants(), force);
    }

    @Override
    public List<EntityID> getNextPlan() {
        List<EntityID> path = new ArrayList<EntityID>();
        List<EntityID> toRemove = new ArrayList<EntityID>();
        EntityID selfPos = world.getSelfPosition().getID();
        path.addAll(previousPath);

        for (EntityID aPreviousPath : previousPath) {
            if (!selfPos.equals(aPreviousPath)) {
                toRemove.add(aPreviousPath);
            } else {
                break;
            }
        }
        path.removeAll(toRemove);
        return path;
    }

    public List<EntityID> planMove(Collection<? extends Area> targets, int maxDistance, boolean force) {
        /**
         * move kardan be yek collection az target ha.
         * nazdiktarin target az nazare oghlidosi ro bedast avorde.
         * ba;d behesh path mizanim.
         */
        List<EntityID> finalAreaPath = new ArrayList<EntityID>();
        Area selfPositionArea = (Area) world.getSelfPosition();

        if (targets.size() == 0) {
            return finalAreaPath;
        }

        if (previousTarget == null || !targets.contains(previousTarget)
                || !isPositionOnPreviousPath(selfPositionArea.getID())) {
            // tanha zamani ke niaz bashe dobare a* mizane ta masir peida kone.
            // yani vaghti yeki az sharayete bala true bashe.
            Area nearestTarget = null;
            int minimumCost = Integer.MAX_VALUE;
            int cost;

            previousPath.clear();

            for (Area target : targets) {
                //peyda kardane nazdiktarin target.
                if (target == null) {
                    continue;
                }

                if (selfPositionArea.equals(target)) {
                    Logger.warn("Time:" + world.getTime() + " Already on target move =" + target);
                    return finalAreaPath;
                }
                cost = world.getDistance(target, selfPositionArea);

                if (cost < minimumCost) {
                    minimumCost = cost;
                    nearestTarget = target;
                }
            }

            // planMove previousPath ra por mikonad.
            // pas ba estefade az aan mitavan path ra bargadand.
            previousPath = planMove(selfPositionArea, nearestTarget, maxDistance, force);
            if (force && previousPath.isEmpty()) {
                /**
                 * age be in target path vojood nadashte bashe va force bashe.
                 * oon ro az targets hazf karde. va vase baghie target ha edame midahim.
                 * ta inke belakhare be yeki az oon ha path peyda konim.
                 * agar ham peyda nashod ke be jahannam.
                 */
                targets.remove(nearestTarget);
                setUnreachableTarget(nearestTarget);
                planMove(targets, maxDistance, force);
            }

        } else if (targets.contains(previousTarget)) {
            // hazfe areahei ke az aanha oboor karde ast.
            ArrayList<EntityID> temp = new ArrayList<EntityID>();

            for (EntityID aPreviousPath : previousPath) {
                if (!selfPositionArea.getID().equals(aPreviousPath)) {
                    temp.add(aPreviousPath);
                } else {
                    break;
                }
            }
            previousPath.removeAll(temp);
        }

        finalAreaPath = previousPath;
        return finalAreaPath;
    }

    RayMoveActExecutor rayMoveActExecutor;

    @Override
    public void moveOnPlan(List<EntityID> plan) throws CommandException {
        // methodi ke act move ra be kernel ersal mikonad.
        if (plan == null) {
            return;
        }
        lastMovePlan.clear();
        lastMovePlan.addAll(plan);

//        if (plan.isEmpty()) {
//            return;
//        }
        Area target = null;
        if (!lastMovePlan.isEmpty()) {
            target = world.getEntity(lastMovePlan.get(lastMovePlan.size() - 1), Area.class);
        } else {
            target = world.getEntity(prevTarget, Area.class);
        }

//        target = world.getEntity(plan.get(plan.size() - 1), Area.class);
        if (/*!rayMoveActExecutor.acceptInterrupt()||*/escapeHere.amIMotionLess()) {
            EscapeState state = null;
            try {
                if (/*!rayMoveActExecutor.acceptInterrupt() ||*//*escapeHere.getTryCount() == 3*/true) {
//                    escapeHere.setTryCount(0);
                    rayMoveActExecutor.execute(plan);
//                    escapeHere.setTryCount(3);
                }
                state = escapeHere.escape(target);
                if (state.equals(EscapeState.UNREACHABLE)) {
                    setUnreachableTarget(target);
                    return;
                } else if (state.equals(EscapeState.ROLL_BACK)) {
                    setThisCycleMoveToPoint(false);
                    EntityID previousPosition = getPreviousPosition();
                    if (previousPosition != null) {
                        platoonAgent.sendMoveAct(world.getTime(), planMove(world.getEntity(previousPosition, Area.class), IN_TARGET, true));
                    }
                } else if (state.equals(EscapeState.TRY_NEW_A_STAR)) {
                    tryToReachTargetCount++;
                    plan = escapeHere.newPlanMove();
                } else if (state.equals(EscapeState.STUCK)) {
                    //MrlPersonalData.print(platoonAgent.getDebugString() + " RANDOM WALK : STUCK");
                    platoonAgent.randomWalk();
                    throw new CommandException("random walk");
                }
            } catch (TimeOutException e) {
                e.printStackTrace();


            }

        }
        lastMoveTime = world.getTime();
        if (plan.isEmpty()) {
            if (isMoveSuccess) {
                tryToMove = 0;
            }
            tryToMove++;
            if (tryToMove > 20) {
                if (MRLConstants.DEBUG_PLANNER) {
                    world.printData("I AM STUCK!!! Is It Right????????");
                }
//                platoonAgent.setStuck(true);
            }
            return;
        } else {
//            platoonAgent.setStuck(false);
            isMoveSuccess = true;
        }
        setThisCycleMoveToPoint(false);
        platoonAgent.sendMoveAct(world.getTime(), plan);
        isMoveSuccess = false;
    }

    private int tryToMove = 0;
    private boolean isMoveSuccess = false;

    /**
     * this property is used to prevent hard walking when agent in previous cycle had move to point action!
     *
     * @return true if this agent in previous cycle had move to point act
     */
    public boolean isThisCycleMoveToPoint() {
        return thisCycleMoveToPoint;
    }

    /**
     * this property is used to prevent hard walking when agent in previous cycle had move to point action!
     *
     * @param thisCycleMoveToPoint true means this agent in previous cycle had move to point act.
     */
    public void setThisCycleMoveToPoint(boolean thisCycleMoveToPoint) {
        this.thisCycleMoveToPoint = thisCycleMoveToPoint;
    }

    private void setUnreachableTarget(Area target) {
        if (target instanceof Building) {
            MrlBuilding mrlBuilding = world.getMrlBuilding(target.getID());
            mrlBuilding.setReachable(false);
        } else {
            MrlRoad mrlRoad = world.getMrlRoad(target.getID());
            mrlRoad.setReachable(false);
        }
    }

    protected Area getNearestArea(Collection entities, Area to) {
        // entekhabe yek area ke az hame nazdiktar ast baraye zamani ke be yek collection move mizanad.

        int distance = Integer.MAX_VALUE;
        Area nearestArea = null;

        for (Object o : entities) {
            StandardEntity next = (StandardEntity) o;
            if (!(next instanceof Road)) {
                continue;
            }
            int dis = world.getDistance(to, next);
            if (dis < distance) {
                distance = dis;
                nearestArea = (Area) next;
            }
        }
        return nearestArea;
    }

    private Pair<EntityID, List<EntityID>> getNearestRefugePath(EntityID selfPosition, boolean force) {
        if (selfPosition == null)
            return null;
        Area selfPositionArea = world.getEntity(selfPosition, Area.class);
        double minDistance = Double.MAX_VALUE;
        List<EntityID> nearestPath = new ArrayList<EntityID>();
        EntityID refugeID = null;
        for (StandardEntity refugeEntity : world.getRefuges()) {
            if (world.getMrlBuilding(refugeEntity.getID()).isReachable()) {
                if (world.getPlatoonAgent() != null) {

                    Pair<Double, List<EntityID>> distancePair = world.getPlatoonAgent().getAverageTools().distancePath(selfPositionArea, world.getEntity(refugeEntity.getID(), Area.class), force);
                    double distance = distancePair.first();
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestPath = distancePair.second();
                        refugeID = refugeEntity.getID();
                    }
                }
            }
        }
        nearestAreaPathCost = (int) minDistance;
        return new Pair<EntityID, List<EntityID>>(refugeID, nearestPath);
    }

    private List<Area> getSomeNearAreaType(int selfX, int selfY, int numberToGetNearest, Collection<? extends StandardEntity> areas) {
        // peyda kardane chand refuge-e nakdiz az nazare fasele oghlidosi.

        List<Area> nearAreas = new ArrayList<Area>();
        List<Area> allRefuges = new ArrayList<Area>();
        for (StandardEntity standardEntity : areas) {
            Area refuge = (Area) standardEntity;
            allRefuges.add(refuge);
        }
        Area selected;
        Area absoluteNearestArea = null;
        int absoluteMinDist = Integer.MAX_VALUE;
        int minDistance, distance, maxDistance = -1;

        while (nearAreas.size() < numberToGetNearest) {

            minDistance = Integer.MAX_VALUE;
            selected = null;

            for (Area area : allRefuges) {
                distance = Util.distance(selfX, selfY, area.getX(), area.getY());
                if (distance < minDistance && (area instanceof Building && world.getMrlBuilding(area.getID()).isOneEntranceOpen(world))) {
                    minDistance = distance;
                    selected = area;
                    absoluteNearestArea = area;
                }
                if (absoluteMinDist > distance) {
                    absoluteMinDist = distance;
                    absoluteNearestArea = area;
                }
            }
            if (world.getSelf() instanceof MrlPoliceForce && absoluteNearestArea != null) {
                break;
            }
            if (maxDistance == -1) {
                maxDistance = minDistance * 2;
            } else if (maxDistance < minDistance) {
                // age distance kheili ziad beshe dige edame nemidim.
                break;
            }
            if (selected != null) {
                allRefuges.remove(selected);
                nearAreas.add(selected);
            }
        }
        if (nearAreas.isEmpty()) {
            nearAreas.add(absoluteNearestArea);
            if (MRLConstants.DEBUG_PLANNER) {
                System.out.println("absoluteNearestArea: " + absoluteNearestArea);
            }
        }
        return nearAreas;
    }

    private List<Refuge> getSomeNearRefuge(int selfX, int selfY) {
        // peyda kardane chand refuge-e nakdiz az nazare fasele oghlidosi.

        List<Refuge> nearRefuges = new ArrayList<Refuge>();
        List<Refuge> allRefuges = new ArrayList<Refuge>();
        for (StandardEntity standardEntity : world.getRefuges()) {
            Refuge refuge = (Refuge) standardEntity;
            allRefuges.add(refuge);
        }
        Refuge selected;
        Refuge absoluteNearestRefuge = null;
        int absoluteMinDist = Integer.MAX_VALUE;
        int minDistance, distance, maxDistance = -1;

        while (nearRefuges.size() < refugeNumberToGetNearest) {

            minDistance = Integer.MAX_VALUE;
            selected = null;

            for (Refuge refuge : allRefuges) {
                distance = Util.distance(selfX, selfY, refuge.getX(), refuge.getY());
                if (distance < minDistance && world.getMrlBuilding(refuge.getID()).isOneEntranceOpen(world)) {
                    minDistance = distance;
                    selected = refuge;
                    absoluteNearestRefuge = refuge;
                }
                if (absoluteMinDist > distance) {
                    absoluteMinDist = distance;
                    absoluteNearestRefuge = refuge;
                }
            }
            if (world.getSelf() instanceof MrlPoliceForce && absoluteNearestRefuge != null) {
                break;
            }
            if (maxDistance == -1) {
                maxDistance = minDistance * 2;
            } else if (maxDistance < minDistance) {
                // age distance kheili ziad beshe dige edame nemidim.
                break;
            }
            if (selected != null) {
                allRefuges.remove(selected);
                nearRefuges.add(selected);
            }
        }
        if (nearRefuges.isEmpty()) {
            nearRefuges.add(absoluteNearestRefuge);
            if (MRLConstants.DEBUG_PLANNER) {
                System.out.println("absoluteNearestRefuge: " + absoluteNearestRefuge);
            }
        }
        return nearRefuges;
    }

    private void setPreviousPosition() {
        EntityID selfPositionId = world.getSelfPosition().getID();
        if (previousPath == null || !previousPath.contains(selfPositionId)) {
            return;
        }
        for (EntityID aPreviousPath : previousPath) {
            if (!selfPositionId.equals(aPreviousPath)) {
                if (previousPositionsQueue.size() > 3) {
                    previousPositionsQueue.remove();
                }
                previousPositionsQueue.add(aPreviousPath);
            } else {
                break;
            }
        }
    }

    public EntityID getPreviousPosition() {
        if (previousPath.isEmpty()) {
            return null;
        }
        return previousPath.get(0);
//        return previousPositionsQueue.peek();//TODO this was replace by mahdi (May not work in previous usages)
//        if (!previousPositionsQueue.isEmpty()) {
//            return previousPositionsQueue.remove();
//        }
//        return null;
    }

    private boolean isPositionOnPreviousPath(EntityID position) {
        return previousPath.contains(position);
    }

    private boolean repeatPlanning(Area target) {
        if (previousTarget == null || !previousTarget.equals(target) || continueMovingTime > 1) {
            continueMovingTime = 0;
            return true;
        } else {
            continueMovingTime++;
            return false;
        }
    }

    /**
     * be ma mige ke che tedad talash baraye residan be 1 target khas az masirhaye motefavet anjam shode
     * (jame tedad A_Star haei ke be vaseteye baste boodane masir ya baz shodane masire jadid zade shod)
     *
     * @return try to reach one target
     */
    public int tryToReachTargetCount() {
        return tryToReachTargetCount;
    }
}