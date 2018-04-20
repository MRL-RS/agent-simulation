package mrl.common.clustering;

import javolution.util.FastSet;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

import java.util.Set;

/**
 * @author vahid Hooshangi
 */
public class CivilianClusterManager extends ClusterManager<CivilianCluster> {
    public int CLUSTER_RANGE_THRESHOLD;


    public CivilianClusterManager(MrlWorld world) {
        super(world);
        CLUSTER_RANGE_THRESHOLD = (world.getViewDistance() / 2);
    }

    @Override
    public void updateClusters() {

        CivilianCluster cluster;
        CivilianCluster tempCluster;
        Set<CivilianCluster> adjacentClusters = new FastSet<CivilianCluster>();
        Building building;
        Human human;
        clusters.clear();
        entityClusterMap.clear();
        for (StandardEntity humanEntity : world.getHumans()) {
            human = (Human) humanEntity;
            if (humanConditionChecker.checkMembership(humanEntity)) {
                building = (Building) human.getPosition(world);
                cluster = getCluster(building.getID());
                if (cluster == null) {
                    cluster = new CivilianCluster(world, humanConditionChecker);
                    cluster.add(building);

                    //checking neighbour clusters
                    for (StandardEntity entity : world.getObjectsInRange(building.getID(), CLUSTER_RANGE_THRESHOLD)) {
                        if (!(entity instanceof Building)) {
                            continue;
                        }

                        tempCluster = getCluster(entity.getID());
                        if (tempCluster == null) {
                            //TODO: isEligible_Estimated ok or it should be isEligible
                            /*if (fireClusterMembershipChecker.isEligible_Estimated(world.getMrlBuilding(entity.getID()))) {
                                cluster.add(entity);
                                entityClusterMap.put(entity.getID(), cluster);
                            }*/
                        } else {
                            adjacentClusters.add(tempCluster);
                        }
                    }

                    if (adjacentClusters.isEmpty()) {
                        addToClusterSet(cluster, building.getID());
                    } else {
                        merge(adjacentClusters, cluster, building.getID());
                    }
                }
            }
            adjacentClusters.clear();
        }

        //-----------Mostafa-------------
        // updating convexHull of each cluster
        for (Cluster c : clusters) {
            c.updateConvexHull();
            c.updateValue();
        }
        //--------------------------------

    }

}
