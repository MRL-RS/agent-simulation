package mrl.common.clustering;

import mrl.geometry.CompositeConvexHull;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 11/28/12
 *         Time: 12:31 AM
 */
public class PoliceTargetCluster extends Cluster {
    @Override
    public void updateConvexHull() {
        convexHull = new CompositeConvexHull();
        convexObject.setConvexPolygon(convexHull.getConvexPolygon());
        if (convexObject != null && convexObject.getConvexPolygon() != null && convexObject.getConvexPolygon().npoints != 0) {
            for (int i = 0; i < convexObject.getConvexPolygon().npoints; i++) {
                convexHull.addPoint(convexObject.getConvexPolygon().xpoints[i],
                        convexObject.getConvexPolygon().ypoints[i]);
            }
        }

        Building building;
        Road road;
        for (StandardEntity entity : entities) {

            if (entity instanceof Building) {
                building = (Building) entity;
                for (int i = 0; i < building.getApexList().length; i += 2) {
                    convexHull.addPoint(building.getApexList()[i], building.getApexList()[i + 1]);
                }
            } else {// entity MUST be instanceof Road
                road = (Road) entity;
                for (int i = 0; i < road.getApexList().length; i += 2) {
                    convexHull.addPoint(road.getApexList()[i], road.getApexList()[i + 1]);
                }
            }
        }

        convexObject.setConvexPolygon(convexHull.getConvexPolygon());
    }

    @Override
    public void updateValue() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
