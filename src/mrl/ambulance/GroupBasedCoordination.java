package mrl.ambulance;

import mrl.ambulance.marketLearnerStrategy.AmbulanceConditionChecker;
import mrl.ambulance.marketLearnerStrategy.AmbulanceUtilities;
import mrl.ambulance.structures.CivilianValue;
import mrl.partitioning.AmbulancePartitionManager;
import mrl.partitioning.Partition;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 6/18/12
 * Time: 9:15 AM
 */
public class GroupBasedCoordination {

    private MrlWorld world;
    private AmbulanceUtilities ambulanceUtilities;
    private AmbulanceConditionChecker conditionChecker;
    private AmbulancePartitionManager ambulancePartitionManager;
    private Human myTarget;
    private Partition myPartition;

    public GroupBasedCoordination(MrlWorld world, AmbulanceUtilities ambulanceUtilities, AmbulanceConditionChecker conditionChecker, VictimClassifier victimClassifier) {
        this.world = world;
        this.ambulanceUtilities = ambulanceUtilities;
        this.conditionChecker = conditionChecker;

        ambulancePartitionManager = new AmbulancePartitionManager(world, ambulanceUtilities, victimClassifier);
        ambulancePartitionManager.initialise();
        world.setPartitionManager(ambulancePartitionManager);

        myPartition = ambulancePartitionManager.findHumanPartition(world.getSelfHuman());

    }


    public Human getNextTarget(Set<StandardEntity> goodHumans) {

        if (!goodHumans.contains(myTarget)) {
            myTarget = null;
        }

        myTarget = (Human) selectTargetGreedily(goodHumans, myTarget);
        return myTarget;
    }

    private StandardEntity selectTargetGreedily(Set<StandardEntity> myGoodHumans, StandardEntity myTarget) {
        if (myTarget != null) {
            Human human = (Human) myTarget;
            if (conditionChecker.isPassable(world.getSelfPosition().getID(), human.getPosition())) {
                return myTarget;
            } else {
                myGoodHumans.remove(myTarget);
            }
        }
        if (myGoodHumans.isEmpty()) {
            return null;
        }

        ArrayList<CivilianValue> civilianTTAs;
        //look in partition
        civilianTTAs = sortHumansBasedOnTTA(myGoodHumans, true);

        //look in world
        if (civilianTTAs.isEmpty()) {
            civilianTTAs = sortHumansBasedOnTTA(myGoodHumans, false);
        }
        if (civilianTTAs.isEmpty()) {
            return null;
        }

        return world.getEntity(civilianTTAs.get(0).getId());


    }


    private ArrayList<CivilianValue> sortHumansBasedOnTTA(Set<StandardEntity> myGoodHumans, boolean inPartition) {
        int tta;
        Pair<Integer, Integer> ttd;
        ArrayList<CivilianValue> civilianTTAs = new ArrayList<CivilianValue>();
        CivilianValue civilianTTA;

        for (StandardEntity standardEntity : myGoodHumans) {
            Human human = (Human) standardEntity;
//            ttd = ambulanceUtilities.computeTTD(human);
            if (ambulanceUtilities.isAlivable((Human) standardEntity) && conditionChecker.isPassable(world.getSelfPosition().getID(), human.getPosition())) {  // if this victim can be alive
                // it will be alive
//                    tta = ambulanceUtilities.approximatingTTA(human);
                tta = ambulanceUtilities.computeDistance(world.getSelfPosition().getID(), human.getPosition());
                civilianTTA = new CivilianValue(human.getID(), tta);
                civilianTTAs.add(civilianTTA);

            }
        }

        Collections.sort(civilianTTAs);

        return civilianTTAs;
    }


}
