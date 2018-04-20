package mrl.common.clustering;

import javolution.util.FastMap;
import mrl.common.Util;
import mrl.partitioning.voronoi.ArraySet;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.*;


/**
 * @version 1.0
 */
public abstract class ClusterManager<cluster extends Cluster> {

    protected MrlWorld world;
    protected List<cluster> clusters;
    protected Map<EntityID, cluster> entityClusterMap;
    public static int CLUSTER_RANGE_THRESHOLD;

    /**
     * Checks MrlBuilding fire cluster membership based on real temperature information
     */
    protected IClusterMembershipChecker fireClusterMembershipChecker;
    protected IClusterMembershipChecker fireClusterMembershipCheckerBasedOnEstimatedFieryness;
    protected IClusterMembershipChecker fireClusterMembershipCheckerBasedOnRealFieryness;

    /**
     * Checks (estimates) MrlBuilding fire cluster membership based on Estimated temperature information
     */
    protected IClusterMembershipChecker clusterMembershipEstimator;
    //protected IClusterMembershipChecker humanConditionChecker;
    protected HumanClusterMembershipChecker humanConditionChecker;
    //protected CivilianClusterManager civilianClusterManager;

    public ClusterManager(MrlWorld world) {
        this.world = world;
        clusters = new ArrayList<cluster>();
        entityClusterMap = new FastMap<EntityID, cluster>();
        fireClusterMembershipChecker = new FireClusterMembershipChecker();
        fireClusterMembershipCheckerBasedOnEstimatedFieryness = new FireClusterMembershipCheckerBasedOnEstimatedFieryness();
        fireClusterMembershipCheckerBasedOnRealFieryness = new FireClusterMembershipCheckerBasedOnRealFieryness();
        clusterMembershipEstimator = new FireClusterMembershipEstimator();
//        humanConditionChecker = new HumanClusterMembershipChecker(this.world);
    }

    public abstract void updateClusters();

    /**
     * merge new cluster to others and replace the result with all others
     *
     * @param adjacentClusters adjacent clusters to the new cluster
     * @param cluster          new constructed cluster
     * @param entityID
     */
    protected void merge(Set<cluster> adjacentClusters, cluster cluster, EntityID entityID) {
        int maxCId = 0;
        for (cluster c : adjacentClusters) {
            if (maxCId < c.getId()) {
                maxCId = c.getId();
            }
            cluster.eat(c);

            // refreshing EntityClusterMap
            for (StandardEntity entity : c.entities) {
                entityClusterMap.remove(entity.getID()); //added 25 khordad! by sajjad & peyman
                entityClusterMap.put(entity.getID(), cluster);
            }
            clusters.remove(c);
            break;//todo: remove this line to merge all possible clusters
        }
        cluster.setId(maxCId);
        addToClusterSet(cluster, entityID);
    }


    public cluster getCluster(EntityID id) {
        return entityClusterMap.get(id);
    }

    public List<cluster> getClusters() {
        return Collections.unmodifiableList(clusters);
    }


    protected void addToClusterSet(cluster cluster, EntityID entityID) {
//        cluster.updateConvexHull();
        entityClusterMap.put(entityID, cluster);
        clusters.add(cluster);
    }

    public cluster findSmallestCluster() {
        cluster resultFireCluster = null;
        double clusterArea = Double.MAX_VALUE;
        for (cluster cluster : clusters) {
            double area = cluster.getBoundingBoxArea();
            if (area < clusterArea) {
                clusterArea = area;
                resultFireCluster = cluster;
            }
        }
        return resultFireCluster;
    }

    public cluster findNearestCluster(Pair<Integer, Integer> location) {
        if (clusters == null || clusters.isEmpty()) {
            return null;
        }
        cluster resultFireCluster = null;
        double minDistance = Double.MAX_VALUE;
        Set<cluster> dyingAndNoExpandableClusters = new ArraySet<cluster>();
        for (cluster cluster : clusters) {
            if (cluster.isDying() || (cluster instanceof FireCluster && !((FireCluster) cluster).isExpandableToCenterOfMap())) {
                dyingAndNoExpandableClusters.add(cluster);
                continue;
            }
            double distance = Util.distance(cluster.getConvexHullObject().getConvexPolygon(), location);
            if (distance < minDistance) {
                minDistance = distance;
                resultFireCluster = cluster;
            }
        }
        minDistance = Double.MAX_VALUE;
        if (resultFireCluster == null) {
            for (cluster cluster : dyingAndNoExpandableClusters) {
                double distance = Util.distance(cluster.getConvexHullObject().getConvexPolygon(), location);
                if (distance < minDistance) {
                    minDistance = distance;
                    resultFireCluster = cluster;
                }
            }
        }
        return resultFireCluster;
    }


    /**
     * This function gets all of the Buildings that are in the border of all clusters
     *
     * @return
     */
    public Set<StandardEntity> findAllBorderEntities() {
        Set<StandardEntity> result = new ArraySet<StandardEntity>();
        //if (world.getFireClusterManager().getClusterSet() != null && world.getFireClusterManager().getClusterSet().size() >= 0)// It is really important to use this condition if we don't use it inside select target
        for (Cluster cluster : clusters) {
            result.addAll(cluster.getBorderEntities());
        }
        return result;
    }


    public Cluster findMostValueCluster() {
        Cluster result = null;
        double value = 0;
        for (Cluster cluster : clusters) {
            double val = cluster.getValue();
            if (val > value) {
                value = val;
                result = cluster;
            }
        }
        return result;
    }
}
