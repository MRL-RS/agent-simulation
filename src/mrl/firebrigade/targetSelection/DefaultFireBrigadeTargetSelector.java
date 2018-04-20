package mrl.firebrigade.targetSelection;

import mrl.common.clustering.FireCluster;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Human;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 6:25 PM
 * Author: Mostafa Movahedi
 */
public abstract class DefaultFireBrigadeTargetSelector implements IFireBrigadeTargetSelector {

    protected MrlFireBrigadeWorld world;
    protected Human selfHuman;
    protected MrlFireBrigade self;
    protected FireBrigadeUtilities fireBrigadeUtilities;
    protected MrlFireBrigadeDirectionManager directionManager;

    protected MrlBuilding target;
    protected MrlBuilding lastTarget;
    protected FireCluster targetCluster;

    protected DefaultFireBrigadeTargetSelector(MrlFireBrigadeWorld world) {
        this.world = world;
        this.fireBrigadeUtilities = new FireBrigadeUtilities(world);
        this.directionManager = new MrlFireBrigadeDirectionManager(world);
    }


}
