package mrl.common.clustering;

import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;

import java.util.List;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/11/12
 * Time: 4:36 PM
 */
public class MotionlessObjectCluster extends Cluster {

    MrlWorld world;
    IClusterMembershipChecker conditionChecker;
    private Set<Cluster> civilianClusterSet;
    private double coef;
    private double value;
    private List<Building> buildings;
    private List<Road> roads;


    public MotionlessObjectCluster(MrlWorld world, IClusterMembershipChecker conditionChecker) {
        this.world = world;
        this.conditionChecker = conditionChecker;
    }

    @Override
    public void updateConvexHull() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateValue() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
