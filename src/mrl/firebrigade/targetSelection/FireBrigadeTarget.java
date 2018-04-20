package mrl.firebrigade.targetSelection;

import mrl.common.clustering.Cluster;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Area;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 6:30 PM
 * Author: Mostafa Movahedi
 */
public class FireBrigadeTarget {

    private MrlBuilding mrlBuilding;
    private Cluster cluster;
    private Area locationToExtinguish;

    public Area getLocationToExtinguish() {
        return locationToExtinguish;
    }

    public void setLocationToExtinguish(Area locationToExtinguish) {
        this.locationToExtinguish = locationToExtinguish;
    }

    public FireBrigadeTarget(MrlBuilding mrlBuilding, Cluster cluster) {
        this.mrlBuilding = mrlBuilding;
        this.cluster = cluster;
    }


    public MrlBuilding getMrlBuilding() {
        return mrlBuilding;
    }

    public Cluster getCluster() {
        return cluster;
    }

    @Override
    public String toString() {
        return String.valueOf(" cluster id = " + cluster.getId());
    }
}
