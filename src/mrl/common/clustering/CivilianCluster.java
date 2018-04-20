package mrl.common.clustering;

import javolution.util.FastMap;
import mrl.geometry.CompositeConvexHull;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Vahid Hooshangi
 */
public class CivilianCluster extends Cluster {

    private MrlWorld world;
    private IClusterMembershipChecker membershipChecker;

    private final double roundingRadius = 1000;
    private List<Map<FireCluster, Double>> distanceMap;
    private Map<FireCluster, Double> finalValues;

    private double finalValue;

    // a map of building ID to number of civilians in it
    private Map<EntityID, Integer> buildingCivilianMap;


    public CivilianCluster(MrlWorld world, IClusterMembershipChecker membershipChecker) {
        this.world = world;
        this.membershipChecker = membershipChecker;

        distanceMap = new ArrayList<Map<FireCluster, Double>>();
        finalValues = new FastMap<FireCluster, Double>();
        buildingCivilianMap = new FastMap<EntityID, Integer>();

//        convexHull = new ConvexHull();
    }

    private void setCentre() {
        int sumX = 0;
        int sumY = 0;
        for (int x : convexObject.getConvexPolygon().xpoints) {
            sumX += x;
        }

        for (int y : convexObject.getConvexPolygon().ypoints) {
            sumY += y;
        }

        if (convexObject.getConvexPolygon().npoints > 0) {
            center = new Point(sumX / convexObject.getConvexPolygon().npoints, sumY / convexObject.getConvexPolygon().npoints);
        } else {
            center = new Point(0, 0);
        }

    }


    @Override
    public void updateConvexHull() {
        MrlBuilding mrlBuilding;
        convexHull = new CompositeConvexHull();

        for (StandardEntity entity : entities) {
            if (entity instanceof Building) {
                mrlBuilding = world.getMrlBuilding(entity.getID());
                try {
                    for (int i = 0; i < mrlBuilding.getSelfBuilding().getApexList().length; i += 2) {
                        convexHull.addPoint(mrlBuilding.getSelfBuilding().getApexList()[i],
                                mrlBuilding.getSelfBuilding().getApexList()[i + 1]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        convexObject.setConvexPolygon(convexHull.getConvexPolygon());

        setCentre();

    }

    @Override
    public void updateValue() {

        MrlBuilding mrlBuilding;
        int numberOfHumans = 0;
        for (StandardEntity buildingEntity : entities) {

            mrlBuilding = world.getMrlBuilding(buildingEntity.getID());
            numberOfHumans += mrlBuilding.getCivilians().size();
        }
        value = computeClusterValue(numberOfHumans, entities.size());

    }

    private double computeClusterValue(int numberOfHumans, int numberOfBuildings) {
        double civilianClusterCoef = (numberOfHumans * (numberOfHumans - 1)) / 3d;
        double civilianClusterDensity = numberOfHumans / (double) numberOfBuildings;
        double civilianClusterValue = civilianClusterCoef * civilianClusterDensity;
        double value = Math.round(civilianClusterValue * roundingRadius) / roundingRadius;
        return value;
    }

    public void setDistanceMap(FireCluster fireCluster, double distance) {
        if (fireCluster == null)
            return;
        Map<FireCluster, Double> map = new FastMap<FireCluster, Double>();

        map.put(fireCluster, distance);

        for (Map<FireCluster, Double> m : distanceMap) {
            if (m.containsKey(fireCluster)) {
                distanceMap.remove(m);
                break;
            }
        }


        distanceMap.add(map);
    }

    public double getFinalValue() {
        return finalValue;
    }
}
