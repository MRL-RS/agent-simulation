package mrl.helper.info;

import javolution.util.FastMap;
import rescuecore2.standard.entities.Edge;

import java.util.Map;

/**
 * User: mrl
 * Date: Dec 3, 2010
 * Time: 6:23:26 PM
 */
public class AreaInfo {

    protected Integer milieu;
    protected Edge longestEdge;
    protected Edge smallestEdge;
    protected Map<Edge, EdgeInfo> edgesInfoMap = new FastMap<Edge, EdgeInfo>();

    public AreaInfo() {
    }

    public void setMilieu(Integer milieu) {
        this.milieu = milieu;
    }

    public void setLongestEdge(Edge longestEdge) {
        this.longestEdge = longestEdge;
    }

    public void setSmallestEdge(Edge smallestEdge) {
        this.smallestEdge = smallestEdge;
    }

    public void addEdge(Edge edge, EdgeInfo info) {
        this.edgesInfoMap.put(edge, info);
    }

    public int getMilieu() {
        return milieu;
    }

    public Edge getLongestEdge() {
        return longestEdge;
    }

    public Edge getSmallestEdge() {
        return smallestEdge;
    }

    public EdgeInfo getEdgeInfo(Edge edge) {
        return edgesInfoMap.get(edge);
    }
}
