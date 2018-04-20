package mrl.firebrigade.targetSelection;

import mrl.common.clustering.Cluster;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 6:45 PM
 * Author: Mostafa Movahedi
 */
public interface IFireBrigadeTargetSelector {

    /**
     * This method finds a FireBrigadeTarget
     *
     * @return FireBrigadeTarget
     */
    public FireBrigadeTarget selectTarget(Cluster targetCluster);

}
