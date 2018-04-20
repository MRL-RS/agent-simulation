package mrl.firebrigade.tools;

import mrl.MrlPersonalData;
import mrl.partition.PairSerialized;
import mrl.partition.Partition;
import mrl.partition.PreRoutingPartitions;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.List;

import static mrl.common.MRLConstants.*;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/17/13
 * Time: 8:46 PM
 * Author: Mostafa Movahedi
 */

/**
 * Process a value for each building to determine which one is most probable to extinguish
 */
public class ProcessAdvantageRatio {
    private MrlWorld world;
    private PreRoutingPartitions preRoutingPartitions;
    private int maxPower;
    private int maxWater;
    private int waterRefillRate;
    private List<Partition> refugePartitions;

    public ProcessAdvantageRatio(MrlWorld world, Config config) {
        this.world = world;
        preRoutingPartitions = new PreRoutingPartitions(world);
        maxPower = config.getIntValue(MAX_EXTINGUISH_POWER_KEY);
        maxWater = config.getIntValue(MAX_WATER_KEY);
        waterRefillRate = WATER_REFILL_RATE;//config.getIntValue(WATER_REFILL_RATE_KEY);
        refugePartitions = new ArrayList<Partition>();
    }

    public void process() {
//        long startTime = System.currentTimeMillis();
        setRefugePartitions();
        for (MrlBuilding mrlBuilding : world.getMrlBuildings()) {

            Partition buildingPartition = preRoutingPartitions.getPartition(mrlBuilding.getSelfBuilding().getLocation(world));
            double distanceToNearestRefuge = Double.MAX_VALUE;
            for (Partition refugePartition : refugePartitions) {
                if (refugePartition.getId().equals(buildingPartition.getId())) {
                    distanceToNearestRefuge = 1.0;
                    break;
                }
                double dist = refugePartition.getPathsToOthers().get(new PairSerialized<Integer, Integer>(refugePartition.getId(), buildingPartition.getId())).second();
                if (dist < distanceToNearestRefuge) {
                    distanceToNearestRefuge = dist;
                }
            }

            double advantageRatio = distanceToNearestRefuge;
            mrlBuilding.setAdvantageRatio(advantageRatio);

            MrlPersonalData.VIEWER_DATA.setBuildingAdvantageRatio(mrlBuilding.getID(), mrlBuilding.getAdvantageRatio());
        }
//        long endTime = System.currentTimeMillis();
//        System.out.print(" ProcessAdvantageRatio(" + (endTime - startTime) + ")");
    }

    private void setRefugePartitions() {
        for (StandardEntity refugeEntity : world.getRefuges()) {
            Partition refugePartition = preRoutingPartitions.getPartition(refugeEntity.getLocation(world));
            if (!refugePartitions.contains(refugePartition)) {
                refugePartitions.add(refugePartition);
            }
        }
    }

}
