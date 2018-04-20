package mrl.ambulance;

import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.ambulance.targetSelector.*;
import mrl.partitioning.AmbulancePartitionManager;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 6/18/12
 * Time: 9:15 AM
 */
public class PartitionBasedCoordination {

    private MrlWorld world;
    private AmbulanceUtilities ambulanceUtilities;
    private AmbulanceConditionChecker conditionChecker;
    private AmbulancePartitionManager ambulancePartitionManager;
    private Partition myPartition;

    private VictimClassifier victimClassifier;

    private ITargetSelector targetSelector;
    private TargetSelectorType targetSelectorType = TargetSelectorType.DISTANCE_BASED;
//    private TargetSelectorType targetSelectorType = TargetSelectorType.UTILITY_BASED;

    public PartitionBasedCoordination(MrlWorld world, AmbulanceUtilities ambulanceUtilities, AmbulanceConditionChecker conditionChecker, VictimClassifier victimClassifier) {
        this.world = world;
        this.ambulanceUtilities = ambulanceUtilities;
        this.conditionChecker = conditionChecker;
        this.victimClassifier = victimClassifier;

        ambulancePartitionManager = new AmbulancePartitionManager(world, ambulanceUtilities, victimClassifier);
        ambulancePartitionManager.initialise();
        world.setPartitionManager(ambulancePartitionManager);

        myPartition = ambulancePartitionManager.findHumanPartition(world.getSelfHuman());

        targetSelector = chooseTargetSelector();
    }

    private ITargetSelector chooseTargetSelector() {

        ITargetSelector targetSelector = null;
        switch (targetSelectorType) {
            case LEGACY_PARTITION_BASED:
                targetSelector = new LegacyPartitionBasedTargetSelector(world, conditionChecker, ambulanceUtilities, myPartition);
                break;
            case SIMPLE_GREEDY:
                targetSelector = new SimpleGreedyTargetSelector(world, conditionChecker, ambulanceUtilities, myPartition);
                break;
            case DISTANCE_BASED:
                targetSelector = new DistanceBasedTargetSelector(world, conditionChecker, ambulanceUtilities, myPartition);
                break;
            case UTILITY_BASED:
                targetSelector = new UtilityBasedTargetSelector(world, conditionChecker, ambulanceUtilities, myPartition);
                break;
        }

        return targetSelector;
    }


    boolean utilityBasedSat = false;
    public Human getNextTarget(Set<StandardEntity> goodHumans) {
        if (!(targetSelector instanceof LegacyPartitionBasedTargetSelector)) {
            ambulancePartitionManager.update();
            myPartition = ambulancePartitionManager.findHumanPartition(world.getSelfHuman());
        }

        if(world.getTime()>=90 && !utilityBasedSat){
            targetSelectorType = TargetSelectorType.UTILITY_BASED;
            targetSelector = chooseTargetSelector();
            utilityBasedSat = true;
            world.printData("ATs Target selector changed to UTILITY_BASED");
        }
        StandardEntity myTarget = targetSelector.nextTarget(goodHumans);
        if (myTarget != null) {
            return (Human) myTarget;

        } else {
            return null;
        }
    }

}
