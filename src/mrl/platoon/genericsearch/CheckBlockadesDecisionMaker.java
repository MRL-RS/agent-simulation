package mrl.platoon.genericsearch;

import mrl.common.Util;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Siavash
 */
public class CheckBlockadesDecisionMaker extends SearchDecisionMaker {

    private static Log logger = LogFactory.getLog(CheckBlockadesDecisionMaker.class);
    private final static int SEARCH_RADIUS = 45000;

    private int blockedRoadsSize = 0;
    private LinkedList<Area> blockedRoads = new LinkedList<Area>();

    public CheckBlockadesDecisionMaker(MrlWorld world) {
        super(world);
    }

    @Override
    public void update() {
        super.update();
        EntityID selfID = world.getSelf().getID();
        Road road = null;
        int minDistance = Integer.MAX_VALUE;
        int distance = 0;
        for (StandardEntity entity : world.getObjectsInRange(selfID, SEARCH_RADIUS)) {
            if (entity instanceof Road) {
                road = (Road) entity;
                if (!road.isBlockadesDefined()
                        || (road.isBlockadesDefined() && road.getBlockades().isEmpty())) {
                    blockedRoads.remove(road);
                }
            }
        }

        if (blockedRoads.size() != blockedRoadsSize) {
//            world.getPlatoonAgent().setStuck(false);
        }
    }

    /**
     * Initializes values for this class depending on implementation
     * <p/>
     * <b>Note:</b> usage of this method is optional
     */
    @Override
    public void initialize() {
        EntityID selfID = world.getSelf().getID();
        Road road = null;
        for (StandardEntity entity : world.getObjectsInRange(selfID, SEARCH_RADIUS)) {
            if (entity instanceof Road) {
                road = (Road) entity;
                if (road.isBlockadesDefined()) {
                    blockedRoads.add(road);
                }
            }
        }

        blockedRoadsSize = blockedRoads.size();

        update();
    }

    /**
     * Evaluates targets
     *
     * @return List of {@link rescuecore2.standard.entities.Area} to search
     */
    @Override
    public List<Area> evaluateTargets() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    /**
     * returns next path for search.
     * <p/>
     * <b>Warning:</b> this method returns different values each time called. you should consider keeping the returning
     * value if you want to use it several times.
     *
     * @return {@link mrl.world.routing.path.Path} to search
     */
    @Override
    public Path getNextPath() {
        throw new UnsupportedOperationException("Not Supported.");
    }

    /**
     * returns next area to search.
     * <p/>
     * <b>Warning:</b> this method returns different values each time called. you should consider keeping the returning
     * value if you want to use it several times.
     *
     * @return
     */
    @Override
    public Area getNextArea() {

        if (Util.isOnBlockade(world)) {
            logger.debug(world.getSelf() + " Buried under blockade.");
            return null;
        }

        Area area = blockedRoads.pollFirst();

        if (area != null) {
            blockedRoads.offerLast(area);
        }

        if (area == null) {
            logger.debug(world.getSelf() + " Blocked Roads list is empty!");
//            world.getPlatoonAgent().setStuck(false);
        }
        return area;
    }
}
