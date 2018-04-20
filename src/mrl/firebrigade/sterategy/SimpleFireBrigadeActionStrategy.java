package mrl.firebrigade.sterategy;

import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.clustering.Cluster;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.firebrigade.exploration.ExploreManager;
import mrl.firebrigade.exploration.TargetBaseExploreManager;
import mrl.firebrigade.extinguishBehaviour.*;
import mrl.firebrigade.targetSelection.*;
import mrl.helper.HumanHelper;
import mrl.platoon.State;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 13/03/15
 * Time: 5:39 PM
 * Author: Mostafa Movahedi
 */
public class SimpleFireBrigadeActionStrategy extends FireBrigadeActionStrategy {
    public SimpleFireBrigadeActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        super(world, fireBrigadeUtilities, directionManager);
        setExtinguishBehaviourApproach();
        exploreManager = new TargetBaseExploreManager(world);
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.print(" " + exploreManager.getClass().getSimpleName() + " ");
        }
    }

    private IFireBrigadeTargetSelector targetSelector;
    private TargetSelectorType targetSelectorType = TargetSelectorType.FULLY_GREEDY;
    private IExtinguishBehaviour extinguishBehaviour;
    private ExtinguishBehaviourType extinguishBehaviourType = ExtinguishBehaviourType.SIMPLE;
    private ExploreManager exploreManager;

    @Override
    public void execute() throws CommandException, TimeOutException {
        self.heatTracerDecisionMaker.updateWarmBuildings();//this line help decision maker use correct data and update them every cycle;
        if (fireBrigadePartitionManager != null) {
            fireBrigadePartitionManager.update();
            myPartition = fireBrigadePartitionManager.findHumanPartition(selfHuman);
        }

        if (world.getHelper(HumanHelper.class).isBuried(selfHuman.getID())) {
            self.setAgentState(State.BURIED);
            return;
        }

        initialAct();


        Cluster targetCluster = world.getFireClusterManager().findNearestCluster((world.getSelfLocation()));
        setTargetSelectorApproach();
        FireBrigadeTarget fireBrigadeTarget = targetSelector.selectTarget(targetCluster);


        if (fireBrigadeTarget != null) {
            MrlPersonalData.VIEWER_DATA.setFireBrigadeData(world.getSelfHuman().getID(), fireBrigadeTarget.getMrlBuilding());
        } else {
            MrlPersonalData.VIEWER_DATA.setFireBrigadeData(world.getSelfHuman().getID(), null);
        }

        // explore around last target
        if (exploreManager.isTimeToExplore(fireBrigadeTarget)) {
            world.getPlatoonAgent().exploreAroundFireSearchManager.execute();
        }

        extinguishBehaviour.extinguish(world, fireBrigadeTarget);

        finalizeAct();

    }

    /**
     * gets type of the action strategy
     */
    @Override
    public FireBrigadeActionStrategyType getType() {
        return FireBrigadeActionStrategyType.DEFAULT;
    }

    private boolean isTimeToSwitchTargetSelectorApproach() {
        //todo: check blockade situation
        if (world.getTime() >= 120 && world.getTime() % 30 == 0) {
            return true;
        }
        return false;
    }


    private void setTargetSelectorApproach() {

        switch (targetSelectorType) {
            case DIRECTION_BASED:
                targetSelector = new DirectionBasedTargetSelector(world);
                break;
            case DIRECTION_BASED14:
                targetSelector = new DirectionBasedTargetSelector14(world);
                break;
            case GREEDY_DIRECTION_BASED:
                targetSelector = new GreedyDirectionBasedTargetSelector(world);
                break;
            case FULLY_GREEDY:
                targetSelector = new FullyGreedyTargetSelector(world);
                break;
            case BLOCKADE_BASED:
                targetSelector = new BlockadeBasedTargetSelector(world);
                break;
            case ZJU_BASED:
                break;
            case TIME_BASED:
                targetSelector = new Direction_Time_BasedTargetSelector(world);
                break;
        }
    }

    private void setExtinguishBehaviourApproach() {
        switch (extinguishBehaviourType) {
            case CLUSTER_BASED:
                extinguishBehaviour = new DirectionBasedExtinguishBehaviour();
                break;
            case MUTUAL_LOCATION:
                extinguishBehaviour = new MutualLocationExtinguishBehaviour();
                break;
            case SIMPLE:
                extinguishBehaviour = new SimpleExtinguishBehaviour();
                break;
        }
    }
}
