package mrl.firebrigade;

import mrl.common.ConvexHull_Rubbish;
import mrl.world.object.MrlBuilding;
import mrl.world.object.MrlFireCluster;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad Salehi
 * Date: 12/22/11
 * Time: 6:08 PM
 */
public class FireClusterManager {
    private List<MrlFireCluster> clusters;
    private MrlFireCluster assignedCluster;
    private List<EntityID> burningBuildings;
    private MrlFireBrigadeWorld world;
    EntityID id;
    List<ConvexHull_Rubbish> convexHulls;
    Map<EntityID, EntityID> FBToTarget;
    Map<EntityID, MrlFireCluster> centersMap;
    int MAX_POWER;

    public FireClusterManager(int fireBrigadeMaxPower, EntityID id, MrlFireBrigadeWorld world) {
        this.world = world;
        clusters = new ArrayList<MrlFireCluster>();
        assignedCluster = null;
        burningBuildings = new ArrayList<EntityID>();
        convexHulls = new ArrayList<ConvexHull_Rubbish>();
        MAX_POWER = fireBrigadeMaxPower;
        this.id = id;
        FBToTarget = new HashMap<EntityID, EntityID>();
        centersMap = new HashMap<EntityID, MrlFireCluster>();
    }

    public void extinguishedBuilding(MrlBuilding b) {
        MrlFireCluster belongedCluster = matchABuildingToCluster(b);
        if (belongedCluster == null)
            return;
        else {
            belongedCluster.removeBuilding(b, world);
            if (burningBuildings.contains(b)) {
                burningBuildings.remove(b.getID());
            }
            if (belongedCluster.getBuildings().size() == 0) {
                clusters.remove(belongedCluster);
                centersMap.remove(belongedCluster.getCenter());
                if (assignedCluster != null && assignedCluster.equals(belongedCluster))
                    assignedCluster = null;
            }
        }
    }

    public void addANewBuilding(MrlBuilding b) {
        //System.out.println();
        //System.out.println("adding building " + b.getID());
        MrlFireCluster foundedCluster = matchABuildingToCluster(b);
        if (foundedCluster != null) {
            if (!foundedCluster.getBuildings().contains(b)) {
                foundedCluster.addBuilding(b);
                burningBuildings.add(b.getID());
                //System.out.println("added to cluster");
            } else {
                //System.out.println("Cluster has been contained this");
            }
        } else {
            MrlFireCluster cluster = new MrlFireCluster(MAX_POWER);
            cluster.addBuilding(b);
            centersMap.put(b.getID(), cluster);
            clusters.add(cluster);
            burningBuildings.add(b.getID());
            //System.out.println("a new cluster has been added");
        }
    }

    public void updateBuildings() {
        if (burningBuildings != null) {
            for (EntityID building : burningBuildings) {
                MrlBuilding b = world.getMrlBuilding(building);
                if (!b.isBurning() && !b.isBurned()) {
                    extinguishedBuilding(world.getMrlBuilding(building));
                }
            }
        }
        for (MrlBuilding building : world.getMrlBuildings()) {
            if (building.isBurning()) {
                if (!burningBuildings.contains(building.getID()))
                    addANewBuilding(building);
            }
        }
    }

    public void update(MrlFireBrigadeWorld world) {
        updateBuildings();
        //checkClusters();
        updateFriends(world);
        updateConvexHullsForViewer(world);
    }

    public void updateFriends(MrlFireBrigadeWorld world) {
        Map<EntityID, EntityID> letsGo = world.getGotoMap();
        for (EntityID friend : letsGo.keySet()) {
            MrlBuilding clusterCenter = world.getMrlBuilding(letsGo.get(friend));

            if (world.getRefuges().contains(clusterCenter)) {
                detachFriendFromCluster(friend);
                return;
            }

            if (FBToTarget.containsKey(friend)) {
                if (FBToTarget.get(friend) != null && letsGo.get(friend) != null) {
                    if (FBToTarget.get(friend).equals(letsGo.get(friend))) {
                        continue;
                    } else {
                        detachFriendFromCluster(friend);
                    }
                }
            }

            if (centersMap.containsKey(clusterCenter.getID())) {
                assignFriendToCluster(centersMap.get(clusterCenter.getID()), friend);
            } else {
                assignFriendToCluster(matchABuildingToCluster(clusterCenter), friend);
            }
        }
        world.clearGoToMap();
    }

    public void checkClusters() {
        List<MrlFireCluster> shouldMerge = new ArrayList<MrlFireCluster>();
        for (MrlFireCluster cluster : clusters) {
            for (int i = 0; i < clusters.size() - 1; i++) {
                if (cluster.getConvexHull().intersectConvexHull(clusters.get(i).getConvexHull())) {
                    shouldMerge.add(cluster);
                    shouldMerge.add(clusters.get(i));
                    mergeClusters(shouldMerge);
                    return;
                }
            }
        }
    }

    public MrlFireCluster matchABuildingToCluster(MrlBuilding b) {
        List<MrlFireCluster> matchedClusters = new ArrayList<MrlFireCluster>();
        if (clusters.size() == 0) {
            //System.out.println("no cluster has matched");
            return null;
        } else {
            for (MrlFireCluster cluster : clusters) {
                if (cluster.isNearToBuilding(b) || cluster.getBuildings().contains(b)) {
                    matchedClusters.add(cluster);
                }
            }
            if (matchedClusters.size() == 1) {
                //System.out.println("building " + b.getID() + " is near one cluster");
                return matchedClusters.get(0);
            } else if (matchedClusters.size() > 1) {
                //System.out.println("building " + b.getID() + "is near " + matchedClusters.size() +" cluster");
                return mergeClusters(matchedClusters);
            } else {
                return null;
            }
        }
    }

    public MrlFireCluster mergeClusters(List<MrlFireCluster> fireClusters) {
//        System.out.println("merging " + fireClusters.size() + " clusters");
//        System.out.println("now clusters contain " + clusters.size());
        clusters.removeAll(fireClusters);
        /*System.out.println("now clusters contain " + clusters.size());*/
        MrlFireCluster newCluster = new MrlFireCluster(MAX_POWER);
        for (MrlFireCluster clr : fireClusters) {
            newCluster.addAllBuildings(clr.getBuildings());
            centersMap.remove(clr.getCenter());
        }
        //System.out.println("new cluster has been added");
        clusters.add(newCluster);
        centersMap.put(newCluster.getCenter(), newCluster);
        return newCluster;
    }

    public boolean assignMeToCluster(MrlFireCluster cluster) {
        if (!clusters.contains(cluster)) {
            return false;
        }

        if (!cluster.getAssignedFireBrigades().contains(id))
            cluster.assignMe(id);

        assignedCluster = cluster;
        return true;
    }

    public boolean assignFriendToCluster(MrlFireCluster cluster, EntityID friend) {
        if (cluster == null)
            return false;

        if (!cluster.getAssignedFireBrigades().contains(friend)) {
            cluster.assignAFireBrigade(friend);
            FBToTarget.put(friend, cluster.getCenter());
        }

        return true;
    }

    public boolean detachFriendFromCluster(EntityID friend) {
        if (!FBToTarget.containsKey(friend) || !centersMap.containsKey(FBToTarget.get(friend)))
            return false;
        if (centersMap.get(FBToTarget.get(friend)).removeFireBrigade(friend)) {
            FBToTarget.remove(friend);
            return true;
        }
        return false;
    }

    public void updateConvexHullsForViewer(MrlFireBrigadeWorld world) {
//        convexHulls.clear();
//        for (MrlFireCluster cluster : clusters)
//            convexHulls.add(cluster.getConvexHullObject());
//        MrlConvexHullLayer.CONVEX_HULLS_MAP.put(world.getSelf().getID(), convexHulls);
    }

    public List<MrlFireCluster> getClusters() {
        return clusters;
    }

    public MrlFireCluster getAssignedCluster() {
        return assignedCluster;
    }

    public int getMyPower() {
        return assignedCluster.power();
    }

    public void printClusters(int time) {
        System.out.println();
        System.out.println(time);
        System.out.println("-------------Fire Clusters---------------");
        int i = 0;
        for (MrlFireCluster cluster : clusters) {
            i++;
            cluster.printCluster(i);
        }
        System.out.println("-------------------------------------------");
    }
}
