package mrl.common.clustering;

import javolution.util.FastSet;
import mrl.helper.HumanHelper;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;

import java.util.Set;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 12:33 AM
 */
public class PoliceTargetClusterManager extends ClusterManager {
    public PoliceTargetClusterManager(MrlWorld world) {
        super(world);
        if (world.isMapHuge()) {
            CLUSTER_RANGE_THRESHOLD = world.getViewDistance() * 2;
        } else {
            CLUSTER_RANGE_THRESHOLD = world.getViewDistance();
        }
    }

    @Override
    public void updateClusters() {
        Cluster cluster;
        Cluster tempCluster;
        Set<Cluster> adjacentClusters = new FastSet<Cluster>();

        entityClusterMap.clear();
        clusters.clear();


        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        EntityID fistPositionID;
        EntityID secondPositionID;


        for (Human firstHuman : humanHelper.getBlockedAgents()) {
            fistPositionID = world.getAgentPositionMap().get(firstHuman.getID());
            cluster = getCluster(fistPositionID);
            if (cluster == null) {
                cluster = new PoliceTargetCluster();
                cluster.add(world.getEntity(fistPositionID));

                //checking neighbour clusters
                for (Human secondHuman : humanHelper.getBlockedAgents()) {
                    secondPositionID = world.getAgentPositionMap().get(secondHuman.getID());
                    if (world.getDistance(fistPositionID, secondPositionID) < CLUSTER_RANGE_THRESHOLD) {

                        tempCluster = getCluster(secondPositionID);
                        if (tempCluster == null) {
                            cluster.add(world.getEntity(secondPositionID));
                            entityClusterMap.put(secondPositionID, cluster);

                        } else {
                            adjacentClusters.add(tempCluster);
                        }
                    }
                }

                if (adjacentClusters.isEmpty()) {
                    addToClusterSet(cluster, fistPositionID);
                } else {
                    merge(adjacentClusters, cluster, fistPositionID);
                }


            } else {
                //do noting
            }
            adjacentClusters.clear();
        }


        // updating convexHull of each cluster
//        for (Cluster c : clusters) {
//            c.updateConvexHull();
//        }


    }
}
