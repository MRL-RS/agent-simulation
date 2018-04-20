package mrl.helper.info;

import javolution.util.FastMap;
import rescuecore2.standard.entities.Edge;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 3:59:18 PM
 */
public class RoadInfo {

    Set<EntityID> neighbours = new HashSet<>();
    EntityID pathId;
    Boolean passable;
    Boolean isolated;
    Boolean seen;
    Integer value;
    int lastMessageTime = -5;
    Map<Edge, Boolean> edgesInfoMap;

    public RoadInfo(Set<EntityID> neighbours, List<Edge> edgeList) {
        this.neighbours = new HashSet<>(neighbours);
        edgesInfoMap = new FastMap<>();
        for (Edge edge : edgeList) {
            edgesInfoMap.put(edge, edge.isPassable());
        }
        isolated = false;
    }

    public void setPathId(EntityID pathId) {
        this.pathId = pathId;
    }


    public void setPassable(Boolean passable) {
        this.passable = passable;
    }

    public void setIsolated(Boolean isolated) {
        this.isolated = isolated;
    }

    public void setSeen(Boolean seen) {
        this.seen = seen;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setLastMessageTime(int lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public Set<EntityID> getNeighbours() {
        return neighbours;
    }

    public EntityID getPathId() {
        return pathId;
    }

    public Boolean isPassable() {
        return passable;
    }

    public Boolean isIsolated() {
        return isolated;
    }

    public Boolean isSeen() {
        return seen;
    }

    public Integer getValue() {
        return value;
    }

    public int getLastMessageTime() {
        return lastMessageTime;
    }

    public boolean isThisEdgePassable(Edge edge) {
        return edgesInfoMap.get(edge);
    }

    public void setEdgePassably(Edge edge, boolean passably) {
        edgesInfoMap.put(edge, passably);
    }
}
