package mrl.firebrigade.sterategy;

import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.HumanHelper;
import mrl.platoon.State;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 3/11/13
 * Time: 5:39 PM
 * Author: Mostafa Movahedi
 */
public class StuckFireBrigadeActionStrategy extends FireBrigadeActionStrategy {
    public StuckFireBrigadeActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        super(world, fireBrigadeUtilities, directionManager);
    }


    @Override
    public void execute() throws CommandException, TimeOutException {

        if (world.getHelper(HumanHelper.class).isBuried(selfHuman.getID())) {
            self.setAgentState(State.BURIED);
            return;
        }

        moveToRefugeIfDamagedOrTankIsEmpty();

        MrlBuilding extinguishTarget = chooseTarget();

        if (extinguishTarget != null) {
            MrlPersonalData.VIEWER_DATA.setFireBrigadeData(world.getSelfHuman().getID(), extinguishTarget);
        } else {
            MrlPersonalData.VIEWER_DATA.setFireBrigadeData(world.getSelfHuman().getID(), null);
        }

        if (extinguishTarget != null) {
            self.sendExtinguishAct(world.getTime(), extinguishTarget.getID(), Math.min(world.getMaxPower(), self.getWater()));
        }
    }

    /**
     * gets type of the action strategy
     */
    @Override
    public FireBrigadeActionStrategyType getType() {
        return FireBrigadeActionStrategyType.STUCK_SITUATION;
    }

    private MrlBuilding chooseTarget() {
        MrlBuilding targetBuilding = null;
        MrlBuilding building;
        List<MrlBuilding> warmBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> fOneBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> fTwoBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> fThreeBuildings = new ArrayList<MrlBuilding>();
        int maxExtinguishDistance = world.getMaxExtinguishDistance();

        for (StandardEntity entity : world.getObjectsInRange(world.getSelfLocation().first(), world.getSelfLocation().second(), world.getMaxExtinguishDistance())) {
            if (entity instanceof Building) {
                int myDistanceToTarget = world.getDistance(world.getSelfHuman(), entity);
                if (myDistanceToTarget <= maxExtinguishDistance) {
                    building = world.getMrlBuilding(entity.getID());
                    if (building.getSelfBuilding().isFierynessDefined()) {
                        switch (building.getSelfBuilding().getFieryness()) {
                            case 1:
                                fOneBuildings.add(building);
                                break;
                            case 2:
                                fTwoBuildings.add(building);
                                break;
                            case 3:
                                fThreeBuildings.add(building);
                                break;
                            case 0:
                            case 4:
                                if (building.getSelfBuilding().isTemperatureDefined() && building.getSelfBuilding().getTemperature() > 0) {
                                    warmBuildings.add(building);
                                }
                        }
                    }
                }
            }
        }

        // TODO @Pooya: check other properties than totalArea
        if (!fOneBuildings.isEmpty()) {
//            targetBuilding = fireBrigadeUtilities.findSmallestBuilding(fOneBuildings);
            targetBuilding = fireBrigadeUtilities.findNewestIgnitedBuilding(fOneBuildings);
        } else if (!warmBuildings.isEmpty()) {
//            targetBuilding = fireBrigadeUtilities.findSmallestBuilding(warmBuildings);
            targetBuilding = fireBrigadeUtilities.findNewestIgnitedBuilding(warmBuildings);
        } else if (!fTwoBuildings.isEmpty()) {
//            targetBuilding = fireBrigadeUtilities.findSmallestBuilding(fTwoBuildings);
            targetBuilding = fireBrigadeUtilities.findNewestIgnitedBuilding(fTwoBuildings);
        } else if (!fThreeBuildings.isEmpty()) {
//            targetBuilding = fireBrigadeUtilities.findSmallestBuilding(fThreeBuildings);
            targetBuilding = fireBrigadeUtilities.findNewestIgnitedBuilding(fThreeBuildings);
        }

        return targetBuilding;
    }

}
