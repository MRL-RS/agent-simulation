package mrl.world.routing.pathPlanner;

import mrl.common.Util;
import mrl.world.MrlWorld;
import mrl.world.routing.a_star.A_Star;
import mrl.world.routing.graph.Graph;
import mrl.world.routing.graph.Node;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

/**
 * author: <strong color="#ff0000">MAHDI</strong><br/>
 * <p/>
 * Date: 6/1/12
 * Time: 4:52 PM
 */
public class AverageTools {
    private MrlWorld world;
    private A_Star a_star;
    private Graph graph;

    public AverageTools(MrlWorld world) {
        this.world = world;
        graph = world.getPlatoonAgent().getPathPlanner().getGraph();
        a_star = new A_Star((PathPlanner) world.getPlatoonAgent().getPathPlanner());
//        estimate();
    }

    /**
     * calculate distance of an area to other area using A_Star
     * note that if you set blockade affect true, if one road blocked , will try another path.
     *
     * @param start source
     * @param end   destination
     * @param force if you set blockade affect true, if one road blocked , will try another path.
     * @return distance between two area
     */
    public double distance(Area start, Area end, boolean force) {
        List<EntityID> pathGraph = a_star.getShortestGraphPath(start, end, force);
        if (pathGraph.isEmpty())
            return Double.MAX_VALUE;
        double distance = 0;
        for (int i = 0; i < pathGraph.size() - 1; i++) {
            int j = i + 1;
            EntityID nodeID1 = pathGraph.get(i);
            EntityID nodeID2 = pathGraph.get(j);
            Node node1 = graph.getNode(nodeID1);
            Node node2 = graph.getNode(nodeID2);
            distance += Util.distance(node1.getPosition(), node2.getPosition());
        }
        return distance;
    }

    public Pair<Double, List<EntityID>> distancePath(Area start, Area end, boolean force) {
        List<EntityID> pathGraph = a_star.getShortestGraphPath(start, end, force);

        if (pathGraph.isEmpty())
            return new Pair<Double, List<EntityID>>(Double.MAX_VALUE, pathGraph);
        double distance = 0;
        for (int i = 0; i < pathGraph.size() - 1; i++) {
            int j = i + 1;
            EntityID nodeID1 = pathGraph.get(i);
            EntityID nodeID2 = pathGraph.get(j);
            Node node1 = graph.getNode(nodeID1);
            Node node2 = graph.getNode(nodeID2);
            distance += Util.distance(node1.getPosition(), node2.getPosition());
        }
        return new Pair<Double, List<EntityID>>(distance, pathGraph);
    }

//    public int getAgentsInRoad(Class<? extends MrlPlatoonAgent> agentType){
//        int count = 0;
//        for (StandardEntity platoonAgent : world.getAgents()){
//            if(agentType.isInstance(platoonAgent)){
//                MrlPlatoonAgent agent = agentType.cast(platoonAgent);
//                world.getE
//                count++;
//            }
//        }
//        return count;
//    }

    /**
     * motevaset zamane residane yek police be yek road...
     * average time to reach one police force to a road
     */
//    private void estimate() {
//        int pfSize = world.getPoliceForceList().size();
//        List<Path> paths = world.getPaths();
//        double totalDistance = 0;
//        for (Path path : paths) {
//            Road head = path.getHeadOfPath();
//            Road end = path.getEndOfPath();
//            double distance = distance(head, end, false);
//            totalDistance += distance;
//        }
//        double average = totalDistance / paths.size();
//        double averagePassingTime = (average / MRLConstants.MEAN_VELOCITY_OF_MOVING);
//        pfReachToRoadTime = averagePassingTime * paths.size() / pfSize;//todo bayad tedade pf haye salem(buried = 0) jaygozin she
//        pfReachToRoadTime = Math.ceil(pfReachToRoadTime / 2);
//
//    }

    /**
     * average time to reach one police force to a road in map
     *
     * @return predicted time to reach PF to a Road
     */

}
