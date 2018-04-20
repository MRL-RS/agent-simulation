package mrl.firebrigade.sterategy;


import mrl.MrlPersonalData;
import mrl.common.CommandException;
import mrl.common.TimeOutException;
import mrl.common.Util;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.FireCluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigade;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.helper.PropertyHelper;
import mrl.partitioning.FireBrigadePartitionManager;
import mrl.partitioning.IPartitionManager;
import mrl.partitioning.Partition;
import mrl.platoon.State;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

import static mrl.common.MRLConstants.AVAILABLE_HYDRANTS_UPDATE_TIME;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/18/13
 * Time: 10:44 PM
 * Author: Mostafa Movahedi
 */
public abstract class FireBrigadeActionStrategy implements IFireBrigadeActionStrategy {
    protected MrlFireBrigadeWorld world;
    protected Human selfHuman;
    protected MrlFireBrigade self;
    protected FireBrigadeUtilities fireBrigadeUtilities;
    protected MrlFireBrigadeDirectionManager directionManager;

    protected MrlBuilding target;
    protected MrlBuilding lastTarget;
    protected FireCluster targetCluster;
    protected int currentWater = 0;
    protected int prevWater = 0;
    protected boolean isWaterRefillRateSet = false;
    protected boolean ifFirstTimeInRefuge = true;
    private boolean isWaterRefillRateInHydrantSet = false;
    private boolean ifFirstTimeInHydrant = true;
    protected int timeInThisHydrant = 0;
    protected EntityID lastHydrant = null;
    protected int stayInHydrant = 15;
    protected IPartitionManager fireBrigadePartitionManager;
    protected Partition myPartition;

    protected FireBrigadeActionStrategy(MrlFireBrigadeWorld world, FireBrigadeUtilities fireBrigadeUtilities, MrlFireBrigadeDirectionManager directionManager) {
        this.world = world;
        this.self = (MrlFireBrigade) world.getPlatoonAgent();
        this.selfHuman = world.getSelfHuman();
        this.fireBrigadeUtilities = fireBrigadeUtilities;
        this.directionManager = directionManager;
        prevWater = self.getWater();
        currentWater = self.getWater();
        initializePartitioning();
    }


    private void initializePartitioning() {
        fireBrigadePartitionManager = new FireBrigadePartitionManager(world, fireBrigadeUtilities);
        fireBrigadePartitionManager.initialise();
        world.setPartitionManager(fireBrigadePartitionManager);

        myPartition = fireBrigadePartitionManager.findHumanPartition(world.getSelfHuman());


    }

    @Override
    public abstract void execute() throws CommandException, TimeOutException;


    protected void initialAct() throws CommandException, TimeOutException {
        moveToRefugeIfDamagedOrTankIsEmpty();
        self.isThinkTimeOver("moveToRefugeIfDamagedOrTankIsEmpty");

        extinguishNearbyWhenStuck();
        self.isThinkTimeOver("extinguishNearbyWhenStuck");

    }

    private boolean moveToHydrant = false;
    private boolean moveToHydrantFail = false;
    private int failTime = 0;

    protected void moveToRefugeIfDamagedOrTankIsEmpty() throws CommandException {
        int requiredWater;
        if (world.getRefuges().isEmpty() && world.getHydrants().isEmpty()) {
            return;
        }

        if (lastHydrant == null || !world.getSelfPosition().getID().equals(lastHydrant)) {
            timeInThisHydrant = 0;
            lastHydrant = null;
        }
        prevWater = currentWater;
        currentWater = self.getWater();

        if (world.getTime() - failTime > AVAILABLE_HYDRANTS_UPDATE_TIME) {
            moveToHydrantFail = false;
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
            if (self.isWaterDefined() && self.getWater() < requiredWater) {
                if (!isWaterRefillRateSet) {
//                    if (!ifFirstTimeInRefuge) {
                    int refillRate = Math.abs(currentWater - prevWater);
                    if (refillRate <= 0) {
//                        world.printData("refill rate can't have negative or zero value");
                    } else if (!self.getLastCommand().equals("Extinguish")) {
                        world.setWaterRefillRate(refillRate);
//                        world.printData("refillRate in refuge is " + refillRate);
                        isWaterRefillRateSet = true;
                    }
//                        System.out.println(self.getID() + " : " + " Refill Rate(" + refillRate + ")");
//                    }
//                    ifFirstTimeInRefuge = false;
                }
                self.sendRestAct(world.getTime());
            }
        } else if (self.getPosition(world) instanceof Hydrant) {
            if (!isWaterRefillRateInHydrantSet) {
                int refillRate = Math.abs(currentWater - prevWater);
                if (refillRate <= 0) {
                    //world.printData("refill rate in hydrant is negative or zero value");
                } else if (self.getLastCommand() != null && !self.getLastCommand().equals("Extinguish")) {
                    world.setWaterRefillRateInHydrant(refillRate);
                    //world.printData("refillRate in hydrant is " + refillRate);
                    isWaterRefillRateInHydrantSet = true;
                }
            }
            boolean havePermission;//show whether current agent has permission to use current hydrant or not
            if (world.getAvailableHydrants().contains(self.getPosition(world))) {
                havePermission = true;
            } else {
                havePermission = false;
            }


            if (havePermission) {

                Hydrant hydrant = (Hydrant) self.getPosition(world);
                lastHydrant = hydrant.getID();
                requiredWater = Math.min(world.getMaxPower() * 3, world.getMaxWater() * 2 / 3);
                if (self.isWaterDefined() && self.getWater() < requiredWater) {
                    if (timeInThisHydrant < stayInHydrant) {
                        timeInThisHydrant++;
                        List<EntityID> path = new ArrayList<EntityID>();
                        path.add(self.getPosition());
                        self.sendMoveAct(world.getTime(), path);
//                    self.sendRestAct(world.getTime());
                    } else {
                        failTime = world.getTime();
                        moveToHydrantFail = true;

                    }
                }
                moveToHydrant = false;
            }
        }

        if (world.getRefuges().size() > 0 && (selfHuman.getDamage() > 10 && selfHuman.getHP() < 5000) || self.getWater() == 0) {
            self.restAtRefuge();
        }

        if (world.getHydrants().size() > 0 && self.getWater() <= 0 && !moveToHydrantFail) {
            moveToHydrant = true;
            self.moveToHydrant();
            moveToHydrant = false;
        }

    }

    private void extinguishNearbyWhenStuck() throws CommandException {
        if (world.getPlatoonAgent().isStuck()) {
            if (((FireBrigade) world.getSelfHuman()).getWater() == 0) {
                target = null;
                return;
            }
            Set<MrlBuilding> buildingsInExtinguishRange = FireBrigadeUtilities.getBuildingsInMyExtinguishRange(world);
            List<MrlBuilding> firedBuildings = new ArrayList<MrlBuilding>();


            for (MrlBuilding mrlBuilding : buildingsInExtinguishRange) {
                if (mrlBuilding.getEstimatedTemperature() > 10) {
                    firedBuildings.add(mrlBuilding);
                }
            }


            target = selectOneOfThese(firedBuildings);

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

    private MrlBuilding getOverallBestBuilding(List<MrlBuilding> burningBuildings) {
        SortedSet<Pair<EntityID, Integer>> sortedBuildings = new TreeSet<Pair<EntityID, Integer>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        List<MrlBuilding> lessValueBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> edgeBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> highValueBuildings = getHighValueBuildings(lessValueBuildings, edgeBuildings);
        for (MrlBuilding b : burningBuildings) {
            /* if (world.isMapHuge())
            {
                if (highValueBuildings.contains())
                continue;
            }*/

            if (edgeBuildings.contains(b)) {
                switch (b.getEstimatedFieryness()) {
                    case 1:
                        b.BUILDING_VALUE = 50 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        break;
                    case 2:
                        b.BUILDING_VALUE = 500 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        break;
                    case 3:
                        b.BUILDING_VALUE = 10000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        break;
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        b.BUILDING_VALUE = 20000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        break;
                    case 8:
                    case 0:
                        b.BUILDING_VALUE = 25000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        break;
                    default:
                        b.BUILDING_VALUE = 25000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        break;
                }

                sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                continue;
            }

            if (lessValueBuildings.contains(b)) {
                switch (b.getEstimatedFieryness() /*b.getSelfBuilding().getFieryness()*/) {
                    case 1:
                    case 2:
                        System.out.println("Something has gone wrong, building" + b.getID() + " Should not have been added to less values");
                        b.BUILDING_VALUE = 50 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 3:
                        b.BUILDING_VALUE = 5000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                        b.BUILDING_VALUE = 7000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 0:
                        b.BUILDING_VALUE = 9000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 8:
                        b.BUILDING_VALUE = 20000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    default:
                        b.BUILDING_VALUE = 9000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                }
                /*if (world.isMapHuge()) {
                    System.out.println("Map is huge");
                    b.BUILDING_VALUE *= 100;
                }*/     //TODO uncomment it
                continue;
            }

            if (highValueBuildings.contains(b)) {
                switch (b.getEstimatedFieryness() /*b.getSelfBuilding().getFieryness()*/) {
                    case 1:
                        b.BUILDING_VALUE = 1 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 2:
                        b.BUILDING_VALUE = 25 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 3:
                        b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 75 : 275 - b.getEstimatedTemperature();
                        b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 0:
                        b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 130 : 160;
                        b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 4:
                        b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 40 : 80;
                        b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 5:
                    case 6:
                        b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 75 : 95;
                        b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 7:
                        b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 120 : 170;
                        b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                    case 8:   //Burnt building
                    default:
                        b.BUILDING_VALUE = 15000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                        break;
                }
                if (b == lastTarget) {
//                    b.BUILDING_VALUE /= 1.54;
                }
                /* if (world.isMapHuge()) {
                    b.BUILDING_VALUE /= 100;
                    System.out.println("Map is huge");
                }*/
                sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                continue;
            }
            switch (b.getEstimatedFieryness() /*b.getSelfBuilding().getFieryness()*/) {
                case 1:
                    b.BUILDING_VALUE = 50 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                case 2:
                    b.BUILDING_VALUE = 110 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                case 3:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 210 : 410 - b.getEstimatedTemperature();
                    b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                case 0:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 190 : 210;
                    b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                case 4:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 160 : 190;
                    b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                case 5:
                case 6:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 200 : 240;
                    b.BUILDING_VALUE += world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                case 7:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 300 : 380 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
                default:
                case 8:         //Burnt Building
                    b.BUILDING_VALUE = 19000 + world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / 1000;
//                        sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
                    break;
            }
            if (b == lastTarget) {
//                    b.BUILDING_VALUE /= 1.54;
            }
            /*if (world.isMapHuge()) {
                System.out.println("Map is huge");
                b.BUILDING_VALUE *= 100;
            }*/
            sortedBuildings.add(new Pair<EntityID, Integer>(b.getID(), (int) b.BUILDING_VALUE));
        }

        Pair<EntityID, Integer> result;
        if (sortedBuildings.size() > 0) {
            result = sortedBuildings.first();
            return world.getMrlBuilding(result.first());
        } else {
            return null;
        }
    }

    //This functions chooses one of the liked buildings to extinguish
    public MrlBuilding selectOneOfThese(List<MrlBuilding> buildings) {

        if (buildings == null || buildings.size() < 1) {
            return null;
        }

        target = getOverallBestBuilding(buildings);
        lastTarget = target;
        return target;
    }

    private List<MrlBuilding> getHighValueBuildings(List<MrlBuilding> lowValues, List<MrlBuilding> edgeBuildings) {
        List<MrlBuilding> highValueBuildings = new ArrayList<MrlBuilding>();
        List<StandardEntity> borderDirectionBuildings = new ArrayList<StandardEntity>();
        List<Cluster> clusters = world.getFireClusterManager().getClusters();
        for (Cluster cluster : clusters) {
            if (cluster.isDying()) {
                for (StandardEntity entity : cluster.getBorderEntities()) {
                    lowValues.add(world.getMrlBuilding(entity.getID()));
                }
                continue;
            }
            if (!cluster.isEdge()) {
                for (StandardEntity entity : cluster.getBorderEntities()) {
                    if (world.getBorderBuildings().contains(entity.getID())) {
                        edgeBuildings.add(world.getMrlBuilding(entity.getID()));
                    }
                }
            }
            if (!cluster.IsOverCenter()) {
                cluster.checkForOverCenter();
            }
            if (cluster.IsOverCenter()) {
                if (cluster == null || cluster.getConvexHullObject() == null || cluster.getConvexHullObject().CONVEX_POINT == null || cluster.getConvexHullObject().CENTER_POINT == null || cluster.getConvexHullObject().getTriangle() == null) {
                    continue;
                }
                Building building;
                Point p1 = cluster.getConvexHullObject().CONVEX_POINT;
                Point pc = cluster.getConvexHullObject().CENTER_POINT;

                int x1, x2, y1, y2, total1, total2;
                for (StandardEntity entity : cluster.getBorderEntities()) {
                    building = (Building) entity;
                    x1 = (p1.x - pc.x) / 1000;
                    x2 = (building.getX() - pc.x) / 1000;
                    y1 = (p1.y - pc.y) / 1000;
                    y2 = (building.getY() - pc.y) / 1000;
                    total1 = x1 * x2;
                    total2 = y1 * y2;
                    if (total1 <= 0 && total2 <= 0) {
                        highValueBuildings.add(world.getMrlBuilding(entity.getID()));
                        borderDirectionBuildings.add(entity);
                    }
                }

            } else {
                Polygon triangle = cluster.getConvexHullObject().getTriangle();
                MrlBuilding building;
                for (StandardEntity entity : cluster.getBorderEntities()) {
                    building = world.getMrlBuilding(entity.getID());
                    int vertexes[] = building.getSelfBuilding().getApexList();
                    for (int i = 0; i < vertexes.length; i += 2) {
                        if (triangle.contains(vertexes[i], vertexes[i + 1])) {
                            highValueBuildings.add(building);
                            borderDirectionBuildings.add(building.getSelfBuilding());
                            break;
                        }
                    }
                }
            }
        }
        MrlPersonalData.VIEWER_DATA.setBorderDirectionBuildings(world.getSelfHuman().getID(), borderDirectionBuildings);

        return highValueBuildings;
    }

    protected void moveToEntranceForCheck() throws CommandException {
        PropertyHelper propertyHelper = world.getHelper(PropertyHelper.class);
        int lastUpdate = propertyHelper.getEntityLastUpdateTime(target.getSelfBuilding());
        if ((world.getTime() - lastUpdate) > 3 && !world.isVisible(target.getSelfBuilding())) {
            self.move((Area) world.getEntity(Util.getNearest(world, target.getVisibleFrom(), self.getPosition())), 0, false);
        }
    }

    protected void finalizeAct() throws CommandException {
        self.setAgentState(State.SEARCHING);
        self.heatTracerSearchManager.execute();
//        self.fireSearcher.execute();

        self.simpleSearchDecisionMaker.setSearchInPartition(true);
        self.simpleSearchManager.execute();
    }

}
