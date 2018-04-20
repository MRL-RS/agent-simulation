package mrl.platoon;

import mrl.common.ConvexHull_Rubbish;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: Mahdi Taherian
 * Date: 2/15/12
 * Time: 10:02 PM
 */

public class CivilianCluster {
    private Set<Civilian> civSet;
    private Set<Civilian> clusteredCivilians;
    private Set<Civilian> newCluster;
    private int cRange = 0;
    private Collection<Set<Civilian>> totalClusters;
    private Collection<Set<Civilian>> importantCivClusters;
    private MrlWorld world;
    private int minCivShow;
    //TODO: ina ro be gheire static tabdil konid (Static baraye ersal be viewere!)
    private static Collection<Set<Building>> importantBuClusters;
    private static Set<Polygon> polygons = new HashSet<Polygon>();
    public static List<Integer> clusterCivSize = new ArrayList<Integer>();
    ////////
    /////////VIEWER///////////
    //TODO: in etelaat az viewer(MrlCivilianClusterLayer) gerefte mishe!!! taghir mikonad
    public static Set<Civilian> civilians = new HashSet<Civilian>();

    public static void createPolygons() {
        if (importantBuClusters == null)
            return;
        for (Set<Building> buildings : importantBuClusters) {
            ConvexHull_Rubbish hull = new ConvexHull_Rubbish();
            for (Building building : buildings) {
                int[] apexes = building.getApexList();
                for (int i = 0; i < apexes.length; i += 2) {
                    hull.addPoint(apexes[i], apexes[i + 1]);
                }
            }
            polygons.add(hull.getConvexPolygon());

        }
    }

    private static boolean dataIsChanged = true;

    public static Set<Polygon> getPolygons() {
        if (dataIsChanged) {
            createPolygons();
        }
        dataIsChanged = false;
        return polygons;
    }
    //////////////////////////


    public CivilianCluster(MrlWorld w, int clusteringRange, int minCivilianShow) {


        minCivShow = minCivilianShow;
        world = w;
        civSet = new HashSet<Civilian>(civilians);
        clusteredCivilians = new HashSet<Civilian>();
        newCluster = new HashSet<Civilian>();
        cRange = clusteringRange;
        totalClusters = new ArrayList<Set<Civilian>>();
        importantCivClusters = new ArrayList<Set<Civilian>>();
        importantBuClusters = new ArrayList<Set<Building>>();

    }
//    private void removeDefeatedCivilians(){
//        for(Civilian civilian : civilians){
//            if(civilian.getHP()==0){
//                civSet.remove(civilian);
//            }
//        }
//    }
//    private void removeCiviliansInRefuge(){
//        for(Civilian civilian : civSet){
//            if(civilian.getPosition(world) instanceof Refuge){
//                civSet.remove(civilian);
//            }
//        }
//        civSet = new HashSet<Civilian>(civilians);
//    }

    public void setImportantClusters() {
        clusterCivSize.clear();
        for (Set<Civilian> cluster : totalClusters) {
            if (cluster.size() >= minCivShow) {
                importantCivClusters.add(cluster);
                Set<Building> buCluster = new HashSet<Building>();
                int count = 0;
                for (Civilian civ : cluster) {
                    Building bu = (Building) world.getEntity(civ.getPosition());
                    buCluster.add(bu);
                    count++;
                }
                clusterCivSize.add(count);
                importantBuClusters.add(buCluster);
            }
        }
    }

    private static int aliveCiviliansCount = 0;

    public void clustering() {
//        removeDefeatedCivilians();
//        removeCiviliansInRefuge();
        if (civilians.size() != aliveCiviliansCount) {
            aliveCiviliansCount = civilians.size();
            dataIsChanged = true;
        }
        for (Civilian civ : civSet) {
            if (clusteredCivilians.contains(civ)) {
                continue;
            }
            clustering(civ);
            Set<Civilian> tempNewCluster = new HashSet<Civilian>(newCluster);
            totalClusters.add(tempNewCluster);
            newCluster.clear();
        }
        setImportantClusters();
    }

    public void clustering(Civilian civ) {
        Set<Civilian> civInRange = new HashSet<Civilian>(getCivilianInRange(civ));
        clusteredCivilians.addAll(civInRange);
        newCluster.addAll(civInRange);
        if (civInRange.size() > 0) {
            for (Civilian civilian : civInRange) {
                if (civilian.getID().getValue() != civ.getID().getValue())
                    clustering(civilian);
            }
        }
    }

    private List<Civilian> getCivilianInRange(Civilian civilian) {
        List<Civilian> civInRange = new ArrayList<Civilian>();
        for (Civilian civ : civSet) {
            if (clusteredCivilians.contains(civ))
                continue;
            int distance = getDistance(civilian, civ);
            if (distance <= cRange)
                civInRange.add(civ);
        }
        return civInRange;
    }

    private int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    private int getDistance(Civilian civ1, Civilian civ2) {
        return getDistance(civ1.getX(), civ1.getY(), civ2.getX(), civ2.getY());
    }

    public void printAll(boolean importance) {
        if (importance) {
            for (Set<Civilian> cluster : importantCivClusters) {
                System.err.println(cluster);
            }
        } else {
            for (Set<Civilian> cluster : totalClusters) {
                System.err.println(cluster);
            }
        }
        System.out.println(clusteredCivilians.size());
    }
}
