package mrl.police;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.communication2013.helper.PoliceMessageHelper;
import mrl.helper.AreaHelper;
import mrl.helper.RoadHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.police.moa.Target;
import mrl.task.PoliceActionStyle;
import mrl.util.PolygonUtil;
import mrl.world.MrlWorld;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.path.Path;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Mostafa
 * Date: Mar 29, 2011
 */
public class ClearHereHelper {
    private MrlWorld world;
    private MrlPlatoonAgent platoonAgent;
    private IPathPlanner pathPlanner;
    private int clearDistance;
    private List<Blockade> importantBlockades;
    private List<Blockade> blockadesToClear;
    private Pair<Integer, Integer> targetPositionPair;
    private int aroundRange = 1000;
    private PoliceMessageHelper policeMessageHelper;

    public ClearHereHelper(MrlWorld world, PoliceMessageHelper policeMessageHelper) {
        this.world = world;
        this.platoonAgent = world.getPlatoonAgent();
        this.pathPlanner = platoonAgent.getPathPlanner();
        this.clearDistance = world.getClearDistance();
        this.importantBlockades = new ArrayList<Blockade>();
        this.blockadesToClear = new ArrayList<Blockade>();
        this.policeMessageHelper = policeMessageHelper;

    }

    public void clear(Road targetRoad, PoliceActionStyle clearType) throws CommandException {
        Blockade targetBlockade;
        targetBlockade = getBlockade(targetRoad, clearType);

        if (targetBlockade == null) {
            return;
        }
        if (MRLConstants.DEBUG_POLICE_FORCE_CLEAR) {
            world.printData(" TARGET BLOCKADE: " + targetBlockade.getID().getValue() + "  IN :" + targetBlockade.getPosition().getValue());
        }
        platoonAgent.sendClearAct(world.getTime(), targetBlockade.getID());
    }


    public void clear(Target target, PoliceActionStyle clearType) throws CommandException {
        Blockade targetBlockade;
        targetBlockade = getBlockadeToClear(target, clearType);

        if (targetBlockade == null) {
            return;
        }
        if (MRLConstants.DEBUG_POLICE_FORCE_CLEAR) {
            world.printData(" TARGET BLOCKADE: " + targetBlockade.getID().getValue() + "  IN :" + targetBlockade.getPosition().getValue());
        }
        platoonAgent.sendClearAct(world.getTime(), targetBlockade.getID());
    }

    public void clear(Target target, PoliceActionStyle clearType, Blockade targetBlockade) throws CommandException {

        if (targetBlockade == null) {
            return;
        }
        if (MRLConstants.DEBUG_POLICE_FORCE_CLEAR) {
            world.printData(" TARGET BLOCKADE: " + targetBlockade.getID().getValue() + "  IN :" + targetBlockade.getPosition().getValue());
        }
        platoonAgent.sendClearAct(world.getTime(), targetBlockade.getID());
    }

    public Blockade getBlockadeToClear(Target target, PoliceActionStyle clearType) throws CommandException {

        refreshBlockadesToClear(blockadesToClear, world.getBlockadeSeen());
        if (!blockadesToClear.isEmpty()) {
            Blockade nearestBlockade = getNearestBlockade(blockadesToClear);
            moveToBlockadeIfIsAway(nearestBlockade);
            if (nearestBlockade != null) {
                return nearestBlockade;
            }
        }

        List<Road> fullClearRoads = new ArrayList<Road>();
        List<Road> simpleClearRoads = new ArrayList<Road>();

        List<Road> roadsToConsider;

        roadsToConsider = getDefaultRoadsToConsider();

        if (target != null && clearType != null) {
            findSpecificRoadsByClearingType(target, clearType, fullClearRoads, simpleClearRoads, roadsToConsider);
        }

        fillingBlockadesToClearList(fullClearRoads, simpleClearRoads, roadsToConsider);

        if (!blockadesToClear.isEmpty()) {
            Blockade nearestBlockade = getNearestBlockade(blockadesToClear);
            moveToBlockadeIfIsAway(nearestBlockade);
            if (nearestBlockade != null) {
                return nearestBlockade;
            }
        }

        return null;

    }

    private void fillingBlockadesToClearList(List<Road> fullClearRoads, List<Road> simpleClearRoads, List<Road> roadsToConsider) {
        Set<EntityID> selectedBlockades = new FastSet<EntityID>();
        for (Road roadToClear : roadsToConsider) {
            if (roadToClear.isBlockadesDefined()) {
                if (fullClearRoads.contains(roadToClear)) {
                    selectedBlockades.addAll(roadToClear.getBlockades());
                } else if (simpleClearRoads.contains(roadToClear)) {
                    selectedBlockades.addAll(getBlockadesToClear(roadToClear, targetPositionPair));
                } else {
                    //for default clearing
                    selectedBlockades.addAll(getBlockadesToClear(roadToClear, null));
                }
            }
        }

        addOtherBlockadeAround(roadsToConsider, selectedBlockades);


        for (EntityID blockadeID : selectedBlockades) {
            blockadesToClear.add((Blockade) world.getEntity(blockadeID));
        }
    }

    // gets other blockade which are in a specific distance to me
    private void addOtherBlockadeAround(List<Road> roadsToConsider, Set<EntityID> selectedBlockades) {

        Road road;
        Blockade blockade;
        for (StandardEntity entity : world.getObjectsInRange(world.getSelfLocation().first(), world.getSelfLocation().second(), aroundRange)) {
            if (entity instanceof Blockade) {
                blockade = (Blockade) entity;
                road = (Road) world.getEntity(blockade.getPosition());
                if (!roadsToConsider.contains(road)) {
                    selectedBlockades.add(blockade.getID());
                }
            }
        }

    }

    private void findSpecificRoadsByClearingType(Target target, PoliceActionStyle clearType, List<Road> fullClearRoads, List<Road> simpleClearRoads, List<Road> roadsToConsider) {

        Road nearestRoad = (Road) world.getEntity(target.getNearestRoadID());
        targetPositionPair = null;
        // import specific roads for clearing based on each target clear strategy
        if (isTimeToCheckSpecificRoads(roadsToConsider, target)) {

            // if last target is a human at a Road, so its road should be clear to human point
            if (clearType.equals(PoliceActionStyle.CLEAR_HUMAN)) {
                targetPositionPair = world.getEntity(target.getId()).getLocation(world);

            } else if (clearType.equals(PoliceActionStyle.CLEAR_ALL)) {
                fullClearRoads.add(nearestRoad);
            } else {
                for (EntityID roadID : target.getRoadsToMove().keySet()) {
                    simpleClearRoads.add((Road) world.getEntity(roadID));
                }
            }
        }
    }

    private boolean isTimeToCheckSpecificRoads(List<Road> roadsToConsider, Target target) {
        for (Road road : roadsToConsider) {
            if (target.getRoadsToMove().keySet().contains(road.getID())) {
                return true;
            }
        }
        return false;
    }

    private List<Road> getDefaultRoadsToConsider() {
        List<Road> roadsToConsider = new ArrayList<Road>();
        List<EntityID> nextMovePlan;
        Road road;
        nextMovePlan = pathPlanner.getNextPlan();
        // if I am in a building and can not see the entrance, so consider that entrance for clearing
        if (world.getSelfPosition() instanceof Building) {
            MrlBuilding building = world.getMrlBuilding(world.getSelfPosition().getID());
            for (Entrance entrance : building.getEntrances()) {
                if ((world.getEntity(entrance.getID()) instanceof Road) && (nextMovePlan.contains(entrance.getID()))) {
                    roadsToConsider.add((Road) world.getEntity(entrance.getID()));
                }
            }

        }

        // iterate on next move plan entities and consider entities for clearing which are Road and are in view range
        for (EntityID id : nextMovePlan) {
            StandardEntity e = world.getEntity(id);
            if (e instanceof Road) {
                road = (Road) e;
                if (world.getRoadsSeen().contains(road)) {
                    roadsToConsider.add(road);
                }
            }
        }

        return roadsToConsider;

    }


    //TODO : edit it by vahid's code
    private Set<EntityID> getBlockadesToClear(Road road, Pair<Integer, Integer> targetPositionPair) {
        Set<EntityID> blockadesToClear = new FastSet<EntityID>();


        if (targetPositionPair == null) {
            for (EntityID blockadeID : road.getBlockades()) {
                blockadesToClear.add(blockadeID);
            }
//            blockadesToClear.addAll(get_KhodetBezar(road));
        } else {
            for (EntityID blockadeID : road.getBlockades()) {
                blockadesToClear.add(blockadeID);
            }
//            blockadesToClear.addAll(get_KhodetBezar(road));
        }

        return blockadesToClear;

    }

    private List<EntityID> get_KhodetBezar(Road road) {
        List<EntityID> blockades = new FastList<EntityID>();
        Polygon polygon = new Polygon();
        for (Edge edge : road.getEdges()) {
            polygon.addPoint(edge.getStartX(), edge.getStartY());
            polygon.addPoint(edge.getEndX(), edge.getEndY());
        }
        Polygon scalePolygon = PolygonUtil.scalePolygon(polygon, 0.75);

        Blockade blockade;
        for (EntityID id : road.getBlockades()) {
            blockade = (Blockade) world.getEntity(id);
            if (scalePolygon.intersects(blockade.getShape().getBounds2D())) {
                blockades.add(id);
            }
        }
        return blockades;
    }

    private Blockade getBlockade(Road targetRoad, PoliceActionStyle clearType) throws CommandException {


        refreshImportantBlockadeList_BlockadeSeen(world.getBlockadeSeen());

        if (!importantBlockades.isEmpty()) {
            return importantBlockades.remove(0);
        }

        StandardEntity entity;
        int distance;
        Set<EntityID> allBlockades;
        Set<Road> roads = new FastSet<Road>();
        List<Blockade> awayBlockades = new ArrayList<Blockade>();

        if (!clearType.equals(PoliceActionStyle.CLEAR_NORMAL)) {
            /**
             * road-haei ke rahe police ra baraye obur baste bashad.
             */
            for (EntityID id : pathPlanner.getNextPlan()) {
                StandardEntity e = world.getEntity(id);
                if (e instanceof Road) {
                    roads.add((Road) e);
                }
            }
            if (targetRoad != null) {
                roads.add(targetRoad);
            }
            /**
             * add important roads seen.
             */
            if (clearType.equals(PoliceActionStyle.CLEAR_IMPORTANT)) {
                for (Road r : world.getRoadsSeen()) {
                    if (isOnImportantEntrance(r.getID())) {
                        roads.add(r);
                    }
                }
            }
        } else {
            roads = world.getRoadsSeen();
        }
        allBlockades = getBlockadesList(roads, clearType);

        /**
         * get near and away blockades.
         */
        for (EntityID id : allBlockades) {

            entity = world.getEntity(id);
            if (entity instanceof Blockade) {
                distance = Util.distance(entity.getLocation(world), world.getSelfLocation());
                if (distance <= clearDistance) {
                    importantBlockades.add((Blockade) entity);
                } else if (!clearType.equals(PoliceActionStyle.CLEAR_TARGET) || inSelfPositionOrNeighbours((Blockade) entity)) {
                    awayBlockades.add((Blockade) entity);
                }
            }
        }

        refreshImportantBlockadeList_AwayBlockades(awayBlockades);


        if (!importantBlockades.isEmpty()) {
            return importantBlockades.remove(0);
        }

        if (!clearType.equals(PoliceActionStyle.CLEAR_TARGET) || world.getSelfPosition().equals(targetRoad)) {
            moveToImportantBlockades(awayBlockades);
        }

        return null;
    }

    private void refreshImportantBlockadeList_BlockadeSeen(Set<Blockade> blockadeList) {
        //remove away blockades from importantBlockades
        List<Blockade> toRemove = new ArrayList<Blockade>();
        for (Blockade blockade : importantBlockades) {
            if (!blockadeList.contains(blockade)) {
                toRemove.add(blockade);
            }
        }
        importantBlockades.removeAll(toRemove);
    }


    /**
     * remove cleared blockades from blockadesToClear set
     *
     * @param blockadesToClear set of blockades to clear
     * @param seenBlockades    set of seen blockades in this cycle by change set
     */
    private void refreshBlockadesToClear(List<Blockade> blockadesToClear, Set<Blockade> seenBlockades) {

        List<Blockade> toRemove = new ArrayList<Blockade>();
        for (Blockade blockade : blockadesToClear) {
            if (!seenBlockades.contains(blockade)) {
                toRemove.add(blockade);
            }
        }
        blockadesToClear.removeAll(toRemove);
    }


    private void refreshImportantBlockadeList_AwayBlockades(List<Blockade> blockadeList) {
        //remove away blockades from importantBlockades
        List<Blockade> toRemove = new ArrayList<Blockade>();
        for (Blockade blockade : importantBlockades) {
            if (blockadeList.contains(blockade)) {
                toRemove.add(blockade);
            }
        }
        importantBlockades.removeAll(toRemove);
    }

    /**
     * get important blockades in these areas.
     *
     * @param roads     list of roads.
     * @param clearType type of clear
     * @return list of blockades.
     */
    private Set<EntityID> getBlockadesList(Set<Road> roads, PoliceActionStyle clearType) {
        Blockade blockade;
        Path path;
        List<Path> sentPathMessage = new ArrayList<Path>();
        List<EntityID> thisAreaBlockades;
        Set<EntityID> allBlockades = new FastSet<EntityID>();

        Area preArea = (Area) world.getSelfPosition();
        int counter = 0;
        Object objects[] = roads.toArray();

// TODO       ---tartibe road ha barresi shavad
//        ---agar tartib ghalat bood, bana be masire darhale gozar morattab shavand
//        ---blockade hayee ke dar dakhele mostatile be arze andazeye agent va toole faseleye agent ta sare road gharar darad be onvane blockade haye moorede nazar
//                bargardande shavad
        for (Road road : roads) {
            counter++;

            if (!world.getRoadsSeen().contains(road)) {
                continue;
            }

            if (road != null) {

                Pair<Edge, Edge> goEdges = null;
                if (clearType.equals(PoliceActionStyle.CLEAR_TARGET)) {
                    Edge edge1;
                    Edge edge2 = null;

                    if (preArea.equals(road)) {
                        EntityID preId = null;
                        for (EntityID id : pathPlanner.getLastMovePlan()) {
                            if (id.equals(world.getSelfPosition().getID())) {
                                break;
                            } else {
                                preId = id;
                            }
                        }
                        if (preId != null) {
                            preArea = (Area) world.getEntity(preId);
                        }
                    }

                    edge1 = preArea.getEdgeTo(road.getID());

                    if (counter < roads.size()) {
                        edge2 = road.getEdgeTo(((Road) objects[counter]).getID());
                    }

                    if (edge1 != null && edge2 != null) {
                        goEdges = new Pair<Edge, Edge>(edge1, edge2);
                    }
                    preArea = road;
                }

                thisAreaBlockades = pathPlanner.getAreaPassably().policeCheckPassably(road, goEdges);

                /**
                 * send clear message...
                 */
                if ((AreaHelper.totalRepairCost(world, road) == 0 || (thisAreaBlockades != null && thisAreaBlockades.isEmpty()))) {
                    world.getHelper(RoadHelper.class).setRoadPassable(road.getID(), true);
                    //todo: review this part for can send message, what is this thing???
                    if (world.getHelper(RoadHelper.class).canSendMessage(road.getID(), world.getTime())) {
                        policeMessageHelper.sendClearedRoadMessage(road.getID());
                        path = world.getPath(road.getID());
                        if (path.isItOpened() && !sentPathMessage.contains(path)) {
                            //policeMessageHelper.sendClearedPathMessage(path.getId());
                            sentPathMessage.add(path);
                        }
                    }
                }
                if (thisAreaBlockades != null && !thisAreaBlockades.isEmpty()) {
                    allBlockades.addAll(thisAreaBlockades);
                }

                /**
                 * add this area blockades in list
                 */
                if (road.isBlockadesDefined()) {
                    for (EntityID id : road.getBlockades()) {
                        blockade = (Blockade) world.getEntity(id);
                        if (isAgentOnThis(blockade) || iConnectedToThis(blockade)) {
                            allBlockades.add(id);
                        }
                    }
                }
            }
        }

        //add self position blockades
        Area selfPosition = (Area) world.getSelfPosition();
        if (selfPosition.getBlockades() != null) {
            for (EntityID bId : selfPosition.getBlockades()) {
                if (amIOnThis((Blockade) world.getEntity(bId))) {
                    allBlockades.add(bId);
                }
            }
        }
        return allBlockades;
    }

    private boolean inSelfPositionOrNeighbours(Blockade blockade) {
        EntityID blockadePosition = blockade.getPosition();
        Area selfPosition = (Area) world.getSelfPosition();
        if (selfPosition.getID().equals(blockadePosition)) {
            return true;
        }
        for (EntityID id : selfPosition.getNeighbours()) {
            if (id.equals(blockadePosition)) {
                return true;
            }
        }
        return false;
    }

    private void moveToImportantBlockades(List<Blockade> awayBlockades) throws CommandException {
        EntityID areaId;
        Map<Blockade, Integer> map = new FastMap<Blockade, Integer>();
        for (Blockade blockade : awayBlockades) {
            map.put(blockade, world.getDistance(blockade, world.getSelfPosition()));
        }
        List<Blockade> sorted = Util.sortByValueInc(map);
        // ramin added to move to nearest blockade first
        for (Blockade blockade : sorted) {
            areaId = blockade.getPosition();

            if (iConnectedToThis(blockade)) {
                importantBlockades.add(blockade);
                platoonAgent.sendClearAct(world.getTime(), blockade.getID());
            } else {
                if (areaId != null) {
//                    importantBlockades.add(blockade);
                    platoonAgent.moveToPoint(areaId, blockade.getX(), blockade.getY());
                }
            }
        }
    }


    private Blockade getNearestBlockade(List<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return null;
        } else if (blockades.size() == 1) {
            return blockades.get(0);
        }

        int minDistance = Integer.MAX_VALUE;
        Blockade nearestBlockade = blockades.get(0);
        int tempDistance;
        for (Blockade blockade : blockades) {
            tempDistance = Util.distance(world.getSelfHuman().getX(), world.getSelfHuman().getY(), blockade.getX(), blockade.getY());
            if (tempDistance < minDistance) {
                minDistance = tempDistance;
                nearestBlockade = blockade;
            }
        }

        return nearestBlockade;
    }

    private void moveToBlockadeIfIsAway(Blockade blockade) throws CommandException {
        if (blockade == null) {
            return;
        }
        if (Util.distance(world.getSelfHuman().getX(), world.getSelfHuman().getY(), blockade.getX(), blockade.getY()) < clearDistance) {
            //do nothing
        } else {
            platoonAgent.moveToPoint(blockade.getPosition(), blockade.getX(), blockade.getY());
        }

    }

    private boolean isOnImportantEntrance(EntityID areaId) {
        Area area = (Area) world.getEntity(areaId);
        Area neighbour;
        AreaHelper areaHelper = world.getHelper(AreaHelper.class);

        for (EntityID id : area.getNeighbours()) {
            neighbour = (Area) world.getEntity(id);
            if (neighbour instanceof Refuge) {
                return true;
            }

            if (world.getEntity(id) == null) {
                System.err.println(id + " this entity is not exist or its ID is not correct which is one neighbour of " + area.getID());
                continue;
            }
            if (!areaHelper.isEmptyBuilding(id)) {
                return true;
            }
        }
        return false;
    }

    private boolean iConnectedToThis(Blockade blockade) {
        return (distanceToBlockade(blockade) <= MRLConstants.AGENT_SIZE);
    }

    private boolean isAgentOnThis(Blockade blockade) {
        Shape blockadeShape = blockade.getShape();
        Pair<Integer, Integer> agentLocation;

        for (EntityID id : world.getChanges()) {
            StandardEntity entity = world.getEntity(id);
            if (entity instanceof Human) {
                agentLocation = entity.getLocation(world);
                if (blockadeShape.contains(agentLocation.first(), agentLocation.second())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean amIOnThis(Blockade blockade) {
        Shape blockadeShape = blockade.getShape();
        Pair<Integer, Integer> agentLocation = world.getSelfLocation();
        return blockadeShape.contains(agentLocation.first(), agentLocation.second());
    }

    private int distanceToBlockade(Blockade blockade) {

        int nearestDistance = Integer.MAX_VALUE;
        int[] allApexes = blockade.getApexes();
        int length = (allApexes.length / 2) - 1;
        int x0 = world.getSelfHuman().getX();
        int y0 = world.getSelfHuman().getY();

        for (int i = 0; i < length; i++) {

            int x1 = allApexes[i * 2];
            int y1 = allApexes[i * 2 + 1];
            int x2 = allApexes[(i + 1) * 2];
            int y2 = allApexes[(i + 1) * 2 + 1];

            // faseleye noghte az khat.
            int dist = calculateDistanceToEdge(x0, y0, x1, y1, x2, y2);
            if (dist < nearestDistance) {
                nearestDistance = dist;
            }
        }
        int x1 = allApexes[0];
        int y1 = allApexes[1];
        int x2 = allApexes[allApexes.length - 2];
        int y2 = allApexes[allApexes.length - 1];

        int dist = calculateDistanceToEdge(x0, y0, x1, y1, x2, y2);
        if (dist < nearestDistance) {
            nearestDistance = dist;
        }

        return nearestDistance;
    }

    private int calculateDistanceToEdge(int a, int b, int x1, int y1, int x2, int y2) {

        int numerator = (y2 - y1);
        int denominator = (x2 - x1);
        int m;
        boolean flag = false;

        if (denominator != 0) {
            m = (numerator / denominator);
        } else {
            m = -1;
        }

        if (m == 0) {
            if ((a <= x1 && a >= x2) || (a >= x1 && a <= x2)) {
                flag = true;
            }
        } else if (m == -1) {
            if ((b <= y1 && b >= y2) || (b >= y1 && b <= y2)) {
                flag = true;
            }
        } else {
            int x = (((b - y1) + (m * (x1 + a))) / 2 * m);
            int y = ((m * x) - (m * x1) - y1);

            if ((y <= y1 && y >= y2) || (y >= y1 && y <= y2)) {
                flag = true;
            }
        }
        if (flag) {
            return (int) ((Math.abs(((x2 - x1) * (y1 - b)) - (x1 - a) * (y2 - y1))) / Math.sqrt(Math.pow((x2 - x1), 2) + (Math.pow((y2 - y1), 2))));
        } else {
            return Math.min(Util.distance(a, b, x1, y1), Util.distance(a, b, x2, y2));
        }

    }
}
