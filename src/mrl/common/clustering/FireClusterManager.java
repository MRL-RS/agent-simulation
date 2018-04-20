package mrl.common.clustering;

import javolution.util.FastMap;
import javolution.util.FastSet;
import math.geom2d.polygon.SimplePolygon2D;
import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * @author Siavash
 */
public class FireClusterManager extends ClusterManager<FireCluster> {

    private Set<Cluster> civilianClusterSet;
    private Set<EntityID> lastBurningBuildings;
    private Set<EntityID> lastExtinguishedBuildings;
    private Set<EntityID> tempBurns;
    private int IDCounter = 1;


    public FireClusterManager(MrlWorld world, Collection<Cluster> civilianClusters) {
        super(world);
        if (world.isMapHuge()) {
            CLUSTER_RANGE_THRESHOLD = world.getViewDistance() * 3;
        } else {
            CLUSTER_RANGE_THRESHOLD = world.getViewDistance();
        }
//        civilianClusterSet = clusterSet;
        lastBurningBuildings = new FastSet<EntityID>();
    }

    public FireClusterManager(MrlWorld world) {
        super(world);
        if (world.getPlatoonAgent() != null) {
            if (world.isMapHuge()) {
                CLUSTER_RANGE_THRESHOLD = (int) (MRLConstants.MEAN_VELOCITY_OF_MOVING * 5);
            } else if (world.isMapMedium()) {
                CLUSTER_RANGE_THRESHOLD = (int) (MRLConstants.MEAN_VELOCITY_OF_MOVING * 3);
            } else {
                CLUSTER_RANGE_THRESHOLD = (int) MRLConstants.MEAN_VELOCITY_OF_MOVING;
            }
        }
        lastBurningBuildings = new FastSet<EntityID>();
        tempBurns = new FastSet<EntityID>();
    }

    @Override
    public void updateClusters() {    //todo; سنگينه
/*        clusters.clear();
        entityClusterMap.clear();*/
        FireCluster cluster;
        FireCluster tempCluster;
        Set<FireCluster> adjacentClusters = new FastSet<FireCluster>();

//        entityClusterMap.clear();
//        clusterSet.clear();

        for (MrlBuilding building : world.getMrlBuildings()) {
            MrlPersonalData.VIEWER_DATA.setBuildingValue(building.getID(), Double.NaN);
            //TODO: isEligible_Estimated ok or it should be isEligible
            //TODO @Siavash Check if the following condition is right
//
//            if(building.getID().getValue()==54179 && building.getSelfBuilding().isFierynessDefined()){
//                System.out.println("dihosgyud");
//            }


            if (clusterMembershipEstimator.checkMembership(building)) {  //old
//            if (fireClusterMembershipCheckerBasedOnEstimatedFieryness.checkMembership(building)) {
                cluster = getCluster(building.getID());
                if (cluster == null) {
//                    cluster = new FireCluster(world, fireClusterMembershipChecker); //old
                    cluster = new FireCluster(world);
                    cluster.add(building.getSelfBuilding());

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
                        cluster.setId(IDCounter++);
                        addToClusterSet(cluster, building.getID());
                    } else {
                        merge(adjacentClusters, cluster, building.getID());
                    }


                } else {
                    //do noting
                }
            } else {
                // Was it previously in any cluster?
                cluster = getCluster(building.getID());
                if (cluster == null) {
                    //do nothing
                } else {
                    cluster.remove(building.getSelfBuilding());
                    entityClusterMap.remove(building.getID());//edited by sajjad, 2 lines shifted up
                    if (cluster.entities.isEmpty()) {
                        clusters.remove(cluster);
                    }
                }
            }
            adjacentClusters.clear();
        }


        // updating convexHull of each cluster
//        List<Pair<Point2D, String>> clusterConditions = new ArrayList<Pair<Point2D, String>>();
        for (FireCluster c : clusters) {
            c.updateConvexHull();
            c.setAllEntities(world.getBuildingsInShape(c.getConvexHullObject().getConvexPolygon()));//Mostafa
        }
        Map<FireCluster, Set<FireCluster>> eat = new FastMap<FireCluster, Set<FireCluster>>();
        List<FireCluster> eatenFireClusters = new ArrayList<FireCluster>();
        for (FireCluster cluster1 : clusters) {
            if (eatenFireClusters.contains(cluster1)) continue;
            Set<FireCluster> feed = new FastSet<FireCluster>();
            for (FireCluster cluster2 : clusters) {
                if (eatenFireClusters.contains(cluster2)) continue;
                if (cluster1.equals(cluster2)) continue;
                if (canEat(cluster1, cluster2)) {
                    feed.add(cluster2);
                    eatenFireClusters.add(cluster2);
                }
            }
            eat.put(cluster1, feed);
        }
        for (FireCluster nextCluster : eat.keySet()) {
            for (FireCluster c : eat.get(nextCluster)) {
                nextCluster.eat(c);
                // refreshing EntityClusterMap
                for (StandardEntity entity : c.entities) {
                    entityClusterMap.remove(entity.getID());
                    entityClusterMap.put(entity.getID(), nextCluster);
                }
                clusters.remove(c);
            }
        }

        List<StandardEntity> ignoredBorderBuildings = new ArrayList<StandardEntity>();
        for (int i = 0; i < clusters.size() - 1; i++) {
            for (int j = i + 1; j < clusters.size(); j++) {
                findMutualEntities(clusters.get(i), clusters.get(j), ignoredBorderBuildings);
            }
        }

        MrlPersonalData.VIEWER_DATA.setIgnoredBorderBuildings(world.getSelf().getID(), ignoredBorderBuildings);

    }

    private boolean canEat(FireCluster cluster1, FireCluster cluster2) {
        int nPointsCluster1 = cluster1.getConvexHullObject().getConvexPolygon().npoints;
        int nPointsCluster2 = cluster2.getConvexHullObject().getConvexPolygon().npoints;
//        double[] xPointsCluster1 = new double[nPointsCluster1];
//        double[] yPointsCluster1 = new double[nPointsCluster1];
        double[] xPointsCluster2 = new double[nPointsCluster2];
        double[] yPointsCluster2 = new double[nPointsCluster2];
//        for (int i = 0; i < nPointsCluster1; i++) {
//            xPointsCluster1[i] = cluster1.getConvexHullObject().getConvexPolygon().xpoints[i];
//            yPointsCluster1[i] = cluster1.getConvexHullObject().getConvexPolygon().ypoints[i];
//        }
        for (int i = 0; i < nPointsCluster2; i++) {
            xPointsCluster2[i] = cluster2.getConvexHullObject().getConvexPolygon().xpoints[i];
            yPointsCluster2[i] = cluster2.getConvexHullObject().getConvexPolygon().ypoints[i];
        }

//        SimplePolygon2D cluster1Polygon = new SimplePolygon2D(xPointsCluster1,yPointsCluster1);
        SimplePolygon2D cluster2Polygon = new SimplePolygon2D(xPointsCluster2, yPointsCluster2);

        double mapArea = (world.getMapWidth() / 1000) * (world.getMapHeight() / 1000);
        if ((cluster2Polygon.getArea() / 1000000) > mapArea * 0.1) return false;

        if (cluster1.getConvexHullObject().getConvexPolygon().contains(cluster2.getCenter())) return true;

        rescuecore2.misc.geometry.Point2D clusterCenter = new rescuecore2.misc.geometry.Point2D(cluster2.getCenter().getX(), cluster2.getCenter().getY());
        Polygon convexPolygon = cluster1.getConvexHullObject().getConvexPolygon();
        for (int i = 0; i < nPointsCluster1; i++) {
            rescuecore2.misc.geometry.Point2D point1 = new rescuecore2.misc.geometry.Point2D(convexPolygon.xpoints[i], convexPolygon.ypoints[i]);
            rescuecore2.misc.geometry.Point2D point2 = new rescuecore2.misc.geometry.Point2D(convexPolygon.xpoints[(i + 1) % nPointsCluster1], convexPolygon.ypoints[(i + 1) % nPointsCluster1]);
            if (Util.distance(new rescuecore2.misc.geometry.Line2D(point1, point2), clusterCenter) < 30000) {
                return true;
            }
        }
        return false;
    }

    public List<MrlBuilding> getBuildingsInConvexPolygon(Polygon polygon) {
        List<MrlBuilding> result = new ArrayList<MrlBuilding>();
        for (MrlBuilding mrlBuilding : world.getEstimatedBurningBuildings()) {
            Pair<Integer, Integer> location = mrlBuilding.getSelfBuilding().getLocation(world);
            if (polygon.contains(location.first(), location.second()))
                result.add(mrlBuilding);
        }
        return result;
    }

    private void findMutualEntities(FireCluster primaryFireCluster, FireCluster secondaryFireCluster, List<StandardEntity> ignoredBorderBuildings) {
        for (MrlBuilding mrlBuilding : getBuildingsInConvexPolygon(primaryFireCluster.getConvexHullObject().getConvexPolygon())) {
            if (getBuildingsInConvexPolygon(secondaryFireCluster.getConvexHullObject().getConvexPolygon()).contains(mrlBuilding)) {
                primaryFireCluster.getIgnoredBorderEntities().add(mrlBuilding.getSelfBuilding());
                primaryFireCluster.getBorderEntities().remove(mrlBuilding.getSelfBuilding());
                secondaryFireCluster.getIgnoredBorderEntities().add(mrlBuilding.getSelfBuilding());
                secondaryFireCluster.getBorderEntities().remove(mrlBuilding.getSelfBuilding());
                ignoredBorderBuildings.add(mrlBuilding.getSelfBuilding());
            }
        }
    }

    /*public FireCluster findNearestCluster(Point myLocation) {
        if (clusters == null || clusters.isEmpty()) {
            return null;
        }
        FireCluster resultFireCluster = null;
        double minDistance = Double.MAX_VALUE;
        Set<FireCluster> dyingAndNoExpandableClusters = new ArraySet<FireCluster>();
        for (FireCluster cluster : clusters) {
            if (cluster.isDying() || !cluster.isExpandableToCenterOfMap()) {
                dyingAndNoExpandableClusters.add(cluster);
                continue;
            }
            double distance = Util.distance(cluster.getConvexHullObject().getConvexPolygon(), myLocation);
            if (distance < minDistance) {
                minDistance = distance;
                resultFireCluster = cluster;
            }
        }
        minDistance = Double.MAX_VALUE;
        if (resultFireCluster == null) {
            for (FireCluster cluster : dyingAndNoExpandableClusters) {
                double distance = Util.distance(cluster.getConvexHullObject().getConvexPolygon(), myLocation);
                if (distance < minDistance) {
                    minDistance = distance;
                    resultFireCluster = cluster;
                }
            }
        }
        return resultFireCluster;
    }*/

    public List<FireCluster> getClusters() {
        return clusters;
    }

}

