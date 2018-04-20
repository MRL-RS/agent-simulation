package mrl.firebrigade;

import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlFireCluster;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad Salehi
 * Date: 12/28/11
 * Time: 1:03 PM
 */
public class MrlFireBrigadeDecisionMaker {

    public MrlFireBrigadeDecisionMaker() {

    }

    public MrlFireCluster chooseNearestCluster(List<MrlFireCluster> clusters, Point myPosition) {
        if (clusters.isEmpty()) {
            return null;
        }

        double minDistance = Double.MAX_VALUE;
        MrlFireCluster selectedCluster = new MrlFireCluster(0);
        List<Point> clusterPoints = new ArrayList<Point>();
        for (MrlFireCluster cluster : clusters) {
            clusterPoints = cluster.getConvexHull().getPoints();
            for (Point point : clusterPoints) {
                double distance = point.distance(myPosition);
                if (distance < minDistance) {
                    minDistance = distance;
                    if (cluster.getAssignedFireBrigades().size() < cluster.getNumberOfFirebrigadesNeeded())
                        selectedCluster = cluster;
                }
            }
        }
        return selectedCluster;
    }

    public MrlFireCluster chooseBestCluster(List<MrlFireCluster> clusters /* add any other parameter if needed   */) {
        // TODO writing the body of this function
        return null;
    }

    public MrlBuilding chooseTarget(MrlFireCluster cluster) {
        if (cluster == null || cluster.size() <= 0)
            return null;

        return firstEdgeBuilding(cluster);
    }

    public MrlBuilding firstEdgeBuilding(MrlFireCluster cluster) {
        if (cluster.getConvexHull().getEdgeBuildings() != null && cluster.getConvexHull().getEdgeBuildings().size() != 0)
            return cluster.getConvexHull().getEdgeBuildings().get(0);
        return null;
    }

}
