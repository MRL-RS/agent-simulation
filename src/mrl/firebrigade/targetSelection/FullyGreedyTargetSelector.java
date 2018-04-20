package mrl.firebrigade.targetSelection;

import mrl.MrlPersonalData;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p> This strategy work on cluster border entities and set values with buildings fieriness and distance only.</p>
 * <p>
 * in ravesh zamani estefade mishavad ke mizane blockade kam va andaze fire ghabele mahar bashad <i>(in maghadir ghablan mohasebe mishavand)</i><br/>
 * raveshe meghdar dehi be in sorat ast ke atash-haye ba fieriness va size-e kamtar meghdare bishtari khahand dasht va harche mizane fieriness bishtar mishavad value aan kamtar mishavad.<br/>
 * nokte inke fasele ta aan building ham dar nazar gerefte mishavad va bar ruye value aan asar migozarad.<br/>
 * entekhabe cluster-e atash be haman sooreta ghabl ast. <br/>
 * </p>
 * <p>
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 12/12/13
 * Time: 3:16 PM
 * </p>
 *
 * @Author: Mostafa Shabani
 */
public class FullyGreedyTargetSelector extends DefaultFireBrigadeTargetSelector {
    public FullyGreedyTargetSelector(MrlFireBrigadeWorld world) {
        super(world);
        this.buildingCostComputer = new ZJUBaseBuildingCostComputer(world);
    }

    private ZJUBaseBuildingCostComputer buildingCostComputer;

    @Override
    public FireBrigadeTarget selectTarget(Cluster targetCluster) {

        FireBrigadeTarget fireBrigadeTarget = null;

        if (targetCluster != null) {
            target = calculateValueZJUBase((FireCluster) targetCluster);
            if (target != null) {
                lastTarget = target;
                fireBrigadeTarget = new FireBrigadeTarget(target, targetCluster);
            }
        }

        return fireBrigadeTarget;

    }

    private MrlBuilding calculateValueZJUBase(FireCluster fireCluster) {
        List<MrlBuilding> buildings = fireCluster.getBuildings();
        MrlBuilding targetBuilding = null;
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        buildingCostComputer.updateFor(fireCluster, lastTarget);

        for (MrlBuilding building : buildings) {
            if (building.isBurning()) {
                building.BUILDING_VALUE = buildingCostComputer.getCost(building);
                sortedBuildings.add(new Pair<EntityID, Double>(building.getID(), building.BUILDING_VALUE));
            }
        }

        MrlPersonalData.VIEWER_DATA.setBuildingValues(world.getSelf().getID(), world.getMrlBuildings());

        if (sortedBuildings != null && !sortedBuildings.isEmpty()) {
            lastTarget = target;
            target = world.getMrlBuilding(sortedBuildings.first().first());
            targetBuilding = target;
        }

        return targetBuilding;
    }


}
