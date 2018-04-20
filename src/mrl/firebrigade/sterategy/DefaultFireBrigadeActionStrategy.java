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
import mrl.firebrigade.extinguishBehaviour.DirectionBasedExtinguishBehaviour;
import mrl.firebrigade.extinguishBehaviour.ExtinguishBehaviourType;
import mrl.firebrigade.extinguishBehaviour.IExtinguishBehaviour;
import mrl.firebrigade.extinguishBehaviour.MutualLocationExtinguishBehaviour;
import mrl.firebrigade.targetSelection.*;
import mrl.helper.HumanHelper;
import mrl.platoon.State;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 5:39 PM
 * Author: Mostafa Movahedi
 */
public class DefaultFireBrigadeActionStrategy extends FireBrigadeActionStrategy {
    public DefaultFireBrigadeActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        super(world, fireBrigadeUtilities, directionManager);
        setTargetSelectorApproach();
        setExtinguishBehaviourApproach();
        exploreManager = new TargetBaseExploreManager(world);
//        exploreManager = new TimeBaseExploreManager(world);
        if (MRLConstants.DEBUG_FIRE_BRIGADE) {
            System.out.print(" " + exploreManager.getClass().getSimpleName() + " ");
        }
    }

    private IFireBrigadeTargetSelector targetSelector;
    //    private TargetSelectorType targetSelectorType = TargetSelectorType.DIRECTION_BASED;
//    private TargetSelectorType targetSelectorType = TargetSelectorType.DIRECTION_BASED14;
    private TargetSelectorType targetSelectorType = TargetSelectorType.FULLY_GREEDY;
    private IExtinguishBehaviour extinguishBehaviour;
    private ExtinguishBehaviourType extinguishBehaviourType = ExtinguishBehaviourType.CLUSTER_BASED;
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

//        if (isTimeToSwitchTargetSelectorApproach()) {
            //FireBrigadeUtilities.refreshFireEstimator(world);
            //targetSelectorType = TargetSelectorType.DIRECTION_BASED;
            //setTargetSelectorApproach();
//        }
        initialAct();


        Cluster targetCluster = world.getFireClusterManager().findNearestCluster((world.getSelfLocation()));
        if (targetCluster != null) {
            if (!targetCluster.isControllable() && targetSelectorType.equals(TargetSelectorType.FULLY_GREEDY)) {
                targetSelectorType = TargetSelectorType.DIRECTION_BASED14;
                setTargetSelectorApproach();
                MrlPersonalData.VIEWER_DATA.print(world.getPlatoonAgent().getDebugString() + " change strategy to " + targetSelectorType);
            } else if (targetCluster.isControllable() && targetSelectorType.equals(TargetSelectorType.DIRECTION_BASED14)) {
                targetSelectorType = TargetSelectorType.FULLY_GREEDY;
                setTargetSelectorApproach();
                MrlPersonalData.VIEWER_DATA.print(world.getPlatoonAgent().getDebugString() + " change strategy to " + targetSelectorType);
            }
        }

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
        }
    }
}
