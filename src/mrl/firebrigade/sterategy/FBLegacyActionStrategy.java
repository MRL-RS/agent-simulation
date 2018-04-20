package mrl.firebrigade.sterategy;

import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.common.clustering.FireCluster;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.HumanHelper;
import mrl.helper.PropertyHelper;
import mrl.platoon.State;
import mrl.world.object.Entrance;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/18/13
 * Time: 10:41 PM
 * Author: Mostafa Movahedi
 */
public class FBLegacyActionStrategy extends FireBrigadeActionStrategy {

    private int changePosition = 0;

    private int targetCycle;
    private SelectionHelper selectionHelper;


    public FBLegacyActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        super(world, fireBrigadeUtilities, directionManager);
        this.fireBrigadeUtilities = fireBrigadeUtilities;
        this.selectionHelper = new SelectionHelper(world, directionManager, world.getMaxExtinguishDistance(), fireBrigadeUtilities);
    }

    @Override
    public void execute() throws CommandException, TimeOutException {

        if (world.getHelper(HumanHelper.class).isBuried(selfHuman.getID())) {
            self.setAgentState(State.BURIED);
            return;
        }
//        if (world.isCommunicationLess()) {
//            communicationLessAct();
//        } else {
        commonAct();
//        }

    }

    /**
     * gets type of the action strategy
     */
    @Override
    public FireBrigadeActionStrategyType getType() {
        return FireBrigadeActionStrategyType.LEGACY;
    }

    public void setCenterDirection() {
        List<FireCluster> fireClusters = world.getFireClusterManager().getClusters();
        for (FireCluster fireCluster : fireClusters) {
            self.getFireBrigadeDirectionManager().setCenterBasedDirection(fireCluster.getConvexHullObject());
        }
    }

    private void communicationLessAct() throws CommandException, TimeOutException {
//        moveToRefugeIfDamagedOrTankIsEmpty();
//        self.isThinkTimeOver("moveToRefugeIfDamagedOrTankIsEmpty");
//
//        self.getFBCLStrategy().execute();

        throw new NotImplementedException();

    }

    private void commonAct() throws CommandException, TimeOutException {

        setCenterDirection();
//        setDirection();

        ourPreciousAct();

        self.setAgentState(State.SEARCHING);
        self.heatTracerSearchManager.execute();
        self.fireSearcher.execute();
//            defaultSearchManager.execute();
//            civilianSearchManager.execute();
        self.simpleSearchManager.execute();
    }

    protected void moveToRefugeIfDamagedOrTankIsEmpty() throws CommandException {
        int requiredWater;
        if (world.getRefuges().isEmpty()) {
            return;
        }

        int fbCount = 0;
        if (self.getPosition(world) instanceof Refuge) {
            for (StandardEntity standardEntity : world.getFireBrigades()) {
                FireBrigade fb = (FireBrigade) standardEntity;
                if (fb.getPosition().equals(self.getPosition()) && world.isVisible(fb)) {
                    fbCount++;
                }
            }

            requiredWater = (world.getMaxWater() + world.getWaterRefillRate()) - (fbCount * world.getWaterRefillRate());

            requiredWater = Util.max(requiredWater, world.getMaxWater() * 2 / 3);

//            if (target != null) {
//                if (world.getDistance(me(), target) <= 140000) {
//                    if (target.getTotalArea() <= 150) {
//                        requiredWater = maxWater / 2;
//                    } else {
//                        requiredWater = maxWater;
//                    }
//                }
//            }

            if (self.isWaterDefined() && self.getWater() < requiredWater) {
                self.sendRestAct(world.getTime());
            }
        }

        if ((selfHuman.getDamage() > 10 && selfHuman.getHP() < 5000) || self.getWater() == 0) {


//            fireBrigadeMessageHelper.sendGoToClusterMessage(null);
            self.restAtRefuge();
        }

    }

    public void ourPreciousAct() throws CommandException, TimeOutException {
//        try{
        moveToRefugeIfDamagedOrTankIsEmpty();
        self.isThinkTimeOver("moveToRefugeIfDamagedOrTankIsEmpty");

        extinguishNearbyWhenStuck();
        self.isThinkTimeOver("extinguishNearbyWhenStuck");

//            extinguishMyself();
//            isThinkTimeOver("extinguish myself");
        target = selectionHelper.selectTarget();
        self.isThinkTimeOver("selectBuilding");

        MrlPersonalData.VIEWER_DATA.setMyTarget(self.getID(), target.getSelfBuilding());

//            moveToRefugeIfNeedWater();
//            isThinkTimeOver("just moving to refuge");

        extinguishTarget();
        self.isThinkTimeOver("extinguishTarget");
//        }catch(TimeOutException e){
//            timeoutExceptionCounter++;
//        }
    }

    private void extinguishTarget() throws CommandException {
        if (target == null) {
            targetCycle = 0;
            return;
        }

        if (lastTarget == target) {
            targetCycle++;
        } else {
            targetCycle = 0;
            lastTarget = target;
        }
//        FireBrigade nearestFb = (FireBrigade) world.getSelfHuman();
        int myDistanceToTarget = world.getDistance(world.getSelfHuman(), target.getSelfBuilding());
        EntityID targetId = target.getSelfBuilding().getID();

        moveToEntranceForCheck();

        // move to target if I can't see this.
        int maxExtinguishDistance = world.getMaxExtinguishDistance();
        if (myDistanceToTarget > maxExtinguishDistance) {
            StandardEntity targetToGo = chooseBestWayToFire();

            if (targetToGo == null) {
                self.move(target.getSelfBuilding(), maxExtinguishDistance, false);
            } else {
                MrlPersonalData.VIEWER_DATA.setStand(world.getSelf().getID(), targetToGo);

                if (!self.move((Area) targetToGo, maxExtinguishDistance, false)) {
                    //age jayee ke mikhast bere rahi behesh nabood miad in khate code
                    Collection<StandardEntity> inRange = world.getObjectsInRange(targetToGo, maxExtinguishDistance / 3);
                    int counter = 0;
                    for (StandardEntity entity : inRange) {
                        if (entity instanceof Area && world.getDistance(target.getSelfBuilding(), entity) < maxExtinguishDistance) {
                            MrlPersonalData.VIEWER_DATA.setStand(world.getSelf().getID(), entity);

                            counter++;
                            self.move((Area) entity, maxExtinguishDistance, false);
                            if (counter > 3) {
                                break;
                            }
                        }
                    }

                }
            }
        }

//        exitFromBuilding();
        if (myDistanceToTarget <= maxExtinguishDistance) {
//            if (location() instanceof Refuge) {
//                Refuge ref = (Refuge) location();
//                Area area = getConnectedAreaToRefugeEntrance(ref);
//                if (area != null) {
//                    move(area, IN_TARGET, false);
//                }
//            }

//            if (target.isTemperatureDefined() && getCyclesRemainTarget() * maxPower > calculateWaterPower(target) * 1.5) {
////                System.out.println(getDebugString() + " needed Water used for : " + target);
//
//                int f = target.getFieryness();
//                if (f <= 3 && f >= 1)
//                    target.setFieryness(f + 4);// set to   put off
//            }

            if (((FireBrigade) world.getSelfHuman()).getWater() != 0) {
                int waterPower = calculateWaterPower(target);

                self.getFireBrigadeMessageHelper().sendWaterMessage(targetId, waterPower);
                target.increaseWaterQuantity(waterPower);

                self.sendExtinguishAct(world.getTime(), targetId, waterPower);
            }

        }
//        if (target != null) {
//            fireBrigadeMessageHelper.sendTargetToGoMessage(target.getID());
//        }
//        System.out.println(getDebugString() + " No way to Extinguish:" + target);

    }

    protected int calculateWaterPower(MrlBuilding building) {
//        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), maxPower);
//        myPower = fireClusterManager.getMyPower();
        return Math.min(((FireBrigade) world.getSelfHuman()).getWater(), Math.min(world.getMaxPower(), Math.max(500, FireBrigadeUtilities.waterNeededToExtinguish(building))));
    }

    @Override
    protected void moveToEntranceForCheck() throws CommandException {
        PropertyHelper propertyHelper = world.getHelper(PropertyHelper.class);
        int lastUpdate = propertyHelper.getEntityLastUpdateTime(target.getSelfBuilding());

        if (        //Commented by Sajjad
            /*(targetCycle > (random.nextInt(3) + 2)
            || (world.isCommunicationLess()
            || world.isCommunicationLimited())
            ||*/ (world.getTime() - lastUpdate) > 2
                && !world.isVisible(target.getSelfBuilding())) {
            Entrance openEntrance = getAnOpenEntrance(target);
            if (openEntrance != null && !world.getSelfPosition().equals(openEntrance.getNeighbour())) {
                self.move(openEntrance.getNeighbour(), MRLConstants.IN_TARGET, false);
            }
        }

    }

    private void extinguishNearbyWhenStuck() throws CommandException {

        if (world.getPlatoonAgent().isStuck()) {
            if (((FireBrigade) world.getSelfHuman()).getWater() == 0) {
                target = null;
                return;
            }
            Collection<StandardEntity> collection = world.getObjectsInRange(world.getSelfHuman(), world.getMaxExtinguishDistance());
            List<MrlBuilding> firedBuildings = new ArrayList<MrlBuilding>();


            for (StandardEntity entity : collection) {
                if ((entity instanceof Building) && Util.distance(entity.getLocation(world), world.getSelfLocation()) < world.getMaxExtinguishDistance()) {
                    MrlBuilding building = world.getMrlBuilding(entity.getID());
                    if (/*building.isBurning()*/ building.getEstimatedTemperature() > 10) {
                        firedBuildings.add(building);
                    }
                }
            }


            target = selectionHelper.selectOneOfThese(firedBuildings);

            if (target != null) {

                if (((FireBrigade) world.getSelfHuman()).getWater() != 0) {
                    int waterPower = FireBrigadeUtilities.calculateWaterPower(world, target);

                    self.getFireBrigadeMessageHelper().sendWaterMessage(target.getID(), waterPower);
                    target.increaseWaterQuantity(waterPower);

                    self.sendExtinguishAct(world.getTime(), target.getID(), waterPower);
                }

//                sendExtinguishAct(world.getTime(), target.getID(), Math.min(2000, maxPower));
            }
        }
    }

    private void moveToRefugeIfNeedWater() throws CommandException { //do not put this function before target selection
        if (target == null && self.getWater() < world.getMaxWater() / 2) {
            self.restAtRefuge();
        } //if we are sure that there is no other work to do... we can go and refill water
    }

    private void extinguishMyself() throws CommandException {
        if ((world.getSelfPosition() instanceof Building) && ((Building) world.getSelfPosition()).isOnFire()) {
            target = world.getMrlBuilding(world.getSelfPosition().getID());
            if (target != null) {

                if (((FireBrigade) world.getSelfHuman()).getWater() != 0) {
                    int waterPower = FireBrigadeUtilities.calculateWaterPower(world, target);

                    self.getFireBrigadeMessageHelper().sendWaterMessage(target.getID(), waterPower);
                    target.increaseWaterQuantity(waterPower);

                    self.sendExtinguishAct(world.getTime(), target.getID(), waterPower);
                }
            }
        }
    }

    public Entrance getAnOpenEntrance(MrlBuilding building) {
        Entrance nearestEntrance = null;
        int minDistance = Integer.MAX_VALUE;

        for (Entrance entrance : building.getEntrances()) {
            int dis = Util.distance(world.getSelfHuman().getX(), world.getSelfHuman().getY(), entrance.getNeighbour().getX(), entrance.getNeighbour().getY());
            if (entrance.isBlockedOrNotSeen(world.getRoadHelper()) && dis < minDistance) {
                minDistance = dis;
                nearestEntrance = entrance;
            }
        }
        return nearestEntrance;
    }

    private boolean aFireBrigadeIsOnEntrances(MrlBuilding building) {
        for (StandardEntity standardEntity : world.getFireBrigades()) {
            FireBrigade fb = (FireBrigade) standardEntity;
            if (fb.isPositionDefined() && building.getID().equals(fb.getPosition())) {
                return true;
            }
        }
        return false;
    }

    private boolean isMyLocationOnEntrances(MrlBuilding building) {
        for (Entrance entrance : building.getEntrances()) {
            if (self.getPosition(world).equals(entrance.getNeighbour())) {
                return true;
            }
        }
        return false;
    }

    private void directionSelection() {
//        directionManager.chooseFireCluster();
//        if (directionManager.directionEnabled) {
//            world.printData("  Direction: " + directionManager.directionEnabled + " UP:" + directionManager.directValue[0] + " RIGHT:" + directionManager.directValue[1] + " DOWN:" + directionManager.directValue[2] + " LEFT:" + directionManager.directValue[3]);
//        }

    }

    // it was an old function, commented by Sajjad
    public MrlBuilding getBestBuilding(List<MrlBuilding> burningBuildings) {
        MrlBuilding best = null;
        double maxVal = Double.MIN_VALUE;
        for (MrlBuilding b : burningBuildings) {

            double val = (b.getBuildingAreaTempValue() * 1) + (b.getBuildingRadiation() * 2) - (b.getNeighbourRadiation() * 1);
//                   if (preTargetBuilding != null && b.equals(preTargetBuilding)) {
//                       val *= 1.2;
//                   }
            int civCount = 0;

            for (StandardEntity entity : world.getCivilians()) {
                Civilian civilian = (Civilian) entity;
                if (civilian.isPositionDefined() && b.getNeighbourIdBuildings().contains(civilian.getPosition())) {
                    civCount++;
                }
            }
            for (EntityID id : world.getBuriedAgents()) {
                Human human = (Human) world.getEntity(id);
                if (human.isPositionDefined() && b.getNeighbourIdBuildings().contains(human.getPosition())) {
                    civCount += 2;
                }
            }

            val += val * (((double) civCount * 5) / 100.0);
            if (val > maxVal) {
                maxVal = val;
                best = b;
            }
        }
        return best;
    }

    private StandardEntity chooseBestWayToFire() {
        double minDistance = Integer.MAX_VALUE;
        StandardEntity targetToExtinguish = null;
        double dis;
        Collection<StandardEntity> inRange = world.getObjectsInRange(target.getID(), world.getMaxExtinguishDistance());

        MrlPersonalData.VIEWER_DATA.setBestPlaceToStand(world.getSelf().getID(), inRange);
//        System.out.println("In Range: " + inRange.size());
        for (StandardEntity se : inRange) {
            if (se instanceof Building) {
                Building b = (Building) se;
                dis = world.getDistance(world.getSelfPosition(), se);
                if (dis < minDistance && (b.isFierynessDefined() && ((b.getFieryness() >= 4 || (b.getFieryness() <= 1))))) {
                    minDistance = dis;
                    targetToExtinguish = se;
                }
            } else if (se instanceof Road) {
                dis = world.getDistance(world.getSelfPosition(), se);
                if (dis < minDistance) {
                    minDistance = dis;
                    targetToExtinguish = se;
                }
            }
        }

        return targetToExtinguish;
    }

}
