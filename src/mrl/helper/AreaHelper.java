package mrl.helper;

import mrl.common.Util;
import mrl.helper.info.AreaInfo;
import mrl.helper.info.EdgeInfo;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 6:07:03 PM
 */
public class AreaHelper implements IHelper {

    protected MrlWorld world;
//    protected Map<EntityID, AreaInfo> areaInfoMap = new FastMap<EntityID, AreaInfo>();

    public AreaHelper(MrlWorld world) {
        this.world = world;
    }

    public void init() {
        for (StandardEntity entity : world.getAreas()) {

            setEdgesInfo((Area) entity);
        }
    }

    public void update() {

    }

    private void setEdgesInfo(Area area) {
        int length;
        Edge longestEdge = null;
        int longer = Integer.MIN_VALUE;
        Edge smallestEdge = null;
        int smaller = Integer.MAX_VALUE;
        int sumOfLengths = 0;

        AreaInfo areaInfo = new AreaInfo();

        for (Edge edge : area.getEdges()) {
            EdgeInfo edgeInfo = new EdgeInfo();

            length = Util.distance(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY());
            edgeInfo.setLength(length);
            if (world.getEntity(edge.getNeighbour()) instanceof Building) {
                edgeInfo.setOnEntrance(true);
            }
            edgeInfo.setMiddle(EdgeHelper.getEdgeMiddle(edge));

            areaInfo.addEdge(edge, edgeInfo);

            sumOfLengths += length;

            if (length > longer) {
                longer = length;
                longestEdge = edge;
            }
            if (length < smaller) {
                smaller = length;
                smallestEdge = edge;
            }
        }

        if (longestEdge != null) {
            areaInfo.setLongestEdge(longestEdge);
        }
        if (smallestEdge != null) {
            areaInfo.setSmallestEdge(smallestEdge);
        }
        areaInfo.setMilieu(sumOfLengths);
    }

//    public AreaInfo getAreaInfoMap(EntityID id) {
//        return areaInfoMap.get(id);
//    }

    public static int totalRepairCost(MrlWorld world, Area area) {
        int total = 0;
        if (area.isBlockadesDefined()) {
            for (EntityID blockId : area.getBlockades()) {
                Blockade blockade = (Blockade) world.getEntity(blockId);
                if (blockade != null) {
                    total += blockade.getRepairCost();
                }
            }
        } else {
            total = -1;
        }
        return total;
    }

    public static Edge getLongestEdge(Area area) {
        double length;
        double longer = Double.MIN_VALUE;
        Edge longestEdge = null;

        for (Edge edge : area.getEdges()) {
            length = EdgeHelper.getEdgeLength(edge);
            if (length > longer) {
                longer = length;
                longestEdge = edge;
            }
        }
        return longestEdge;
    }

    public static Edge getSmallestEdge(Area area) {
        double length;
        double smaller = Double.MAX_VALUE;
        Edge smallestEdge = null;

        for (Edge edge : area.getEdges()) {
            length = EdgeHelper.getEdgeLength(edge);
            if (length < smaller) {
                smaller = length;
                smallestEdge = edge;
            }
        }
        return smallestEdge;
    }

    public List<EntityID> whoIsIn(EntityID areaId) {
        List<EntityID> list = new ArrayList<EntityID>();
        Collection<StandardEntity> entities = world.getObjectsInRange(areaId, world.getViewDistance());

        for (StandardEntity entity : entities) {
            if ((entity instanceof Human) && ((Human) entity).isPositionDefined() && ((Human) entity).getPosition().equals(areaId)) {
                list.add(entity.getID());
            }
        }

        return list;
    }

    public boolean isEmptyBuilding(EntityID areaId) {
        Collection<StandardEntity> entities = world.getObjectsInRange(areaId, world.getViewDistance());

        for (StandardEntity entity : entities) {
            if ((entity instanceof Human) && ((Human) entity).isPositionDefined() && ((Human) entity).getPosition().equals(areaId)) {
                return true;
            }
        }

        return true;
    }
}
