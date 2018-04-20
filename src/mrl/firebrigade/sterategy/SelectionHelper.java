package mrl.firebrigade.sterategy;

import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.clustering.Cluster;
import mrl.common.comparator.ConstantComparators;
import mrl.firebrigade.FireBrigadeUtilities;
import mrl.firebrigade.MrlFireBrigadeDirectionManager;
import mrl.firebrigade.MrlFireBrigadeWorld;
import mrl.world.object.MrlBuilding;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Sajjad
 * Date: 3/3/12
 * Time: 7:17 PM
 */
public class SelectionHelper {

    private MrlBuilding target;
    private MrlFireBrigadeWorld world;
    private FireBrigadeUtilities fireBrigadeUtilities;
    private int buildingWaterSpray;
    //    private int power;
//    private MrlFireBrigadeDirectionManager directionManager;
    int numberOfFireBrigades;
    private MrlBuilding lastTarget;
    IPathPlanner a_star;
    int max_distance;

    public SelectionHelper(MrlFireBrigadeWorld world, MrlFireBrigadeDirectionManager directionManager, int maxDistance, FireBrigadeUtilities fireBrigadeUtilities) {
        target = null;
        lastTarget = null;
        this.world = world;
        buildingWaterSpray = 0;
        numberOfFireBrigades = world.getFireBrigades().size();
        a_star = world.getPlatoonAgent().getPathPlanner();
        this.max_distance = maxDistance;
        this.fireBrigadeUtilities = fireBrigadeUtilities;

//        this.directionManager = directionManager;
    }

    /**
     * This Function selects the best target to extinguish
     *
     * @returns the best MrlBuilding
     */
    public MrlBuilding selectTarget() {
//        List<MrlBuilding> sortedCandidates = new ArrayList<MrlBuilding>();
//        List<MrlBuilding> finalCandidates = new ArrayList<MrlBuilding>();
        if (world.getFireClusterManager().getClusters() != null && world.getFireClusterManager().getClusters().size() > 0) {
            Set<StandardEntity> borders = world.getFireClusterManager().findAllBorderEntities();

            List<MrlBuilding> borderBuildings = new ArrayList<MrlBuilding>();
            for (StandardEntity entity : borders) {
                MrlBuilding b = world.getMrlBuilding(entity.getID());
                borderBuildings.add(b);
            }
//            target = getOverallBestBuilding(borderBuildings);
            target = getOverallBestBuildings(borderBuildings); //sajjad --> value base target selection
//            target = getNearestTarget(borderBuildings);//mostafa --> greedy target selection
//            target = getTarget();
            lastTarget = target;
//            sortedCandidates = getOverallBestBuildings(borderBuildings);
//            finalCandidates = selectTop(5,sortedCandidates);

            MrlPersonalData.VIEWER_DATA.setBuildingValues(world.getSelf().getID(), world.getMrlBuildings());

            return target;
        }
        return null;
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

    // It just does a very simple choosing, chooses the first! we don't use it any more
    public Cluster chooseFireCluster() {
        List<Cluster> fireClusters = world.getFireClusterManager().getClusters();

        if (fireClusters.iterator().hasNext()) {
            return fireClusters.iterator().next();
        }

        return null;
    }

    /**
     * This Functions chooses the target just by distance parameter
     *
     * @param burningBuildings are all the border burning buildings of the map (In all clusters)
     * @returns the best building to extinguish
     */
    private MrlBuilding getBestBuildingByDistance(List<MrlBuilding> burningBuildings) {
        if (burningBuildings == null || burningBuildings.size() == 0)
            return null;

        List<Pair<EntityID, Integer>> distances = new ArrayList<Pair<EntityID, Integer>>();

        for (MrlBuilding b : burningBuildings) {
            distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
        }
        Collections.sort(distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Pair<EntityID, Integer> res = distances.get(0);
        MrlBuilding best = world.getMrlBuilding(res.first());

        if (best.getEstimatedFieryness() == 8)   // If the building is burnt out doesn't select it!
        {
            res = distances.get(1);
            best = world.getMrlBuilding(res.first());
        }

        return best;
    }

    /**
     * This function chooses best building to extinguish using 2 parameters, 1. Distance 2. Fieryness
     * Using these 2 parameters helps the fireBrigades work better, because they don't switch between clusters too much and choose buildings that are easier to be extinguished
     *
     * @param burningBuildings are all the border burning buildings of the map (In all clusters)
     * @returns the best building to extinguish
     */
    private MrlBuilding getBestBuildingByDistanceAndFieryness(List<MrlBuilding> burningBuildings) {
        if (burningBuildings == null || burningBuildings.size() == 0)
            return null;
        List<Pair<EntityID, Integer>> fieryness1Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> fieryness2Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> fieryness3Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> otherDistances = new ArrayList<Pair<EntityID, Integer>>();

        for (MrlBuilding b : burningBuildings) {
            switch (b.getEstimatedFieryness()) {
                case 1:
                    fieryness1Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
                case 2:
                    fieryness2Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
                case 3:
                    fieryness3Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
                case 5:
                case 4:
                case 6:
                case 7:
                case 8:
                    otherDistances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
            }
        }
        Collections.sort(fieryness1Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(fieryness2Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(fieryness3Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(otherDistances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);

        Pair<EntityID, Integer> result = new Pair<EntityID, Integer>(new EntityID(0), 0);
        if (fieryness1Distances.size() > 0) {
            result = fieryness1Distances.get(0);
        } else if (fieryness2Distances.size() > 0) {
            result = fieryness2Distances.get(0);
        } else if (fieryness3Distances.size() > 0) {
            result = fieryness3Distances.get(0);
        } else if (otherDistances.size() > 0) {
            result = otherDistances.get(0);
        }

        MrlBuilding best = world.getMrlBuilding(result.first());
        return best;
    }

    /**
     * This function is the last function written by me at march 2012
     * This is the collection of all other ideas that we had before. The difference is that now we use Value, so that they seem not to switch too much between clusters. This makes the supremacy in the performance.
     *
     * @param burningBuildings as previous functions
     * @return as previous functions
     */
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

    /**
     * select nearest target in border buildings of cluster (for use in greedy strategy)
     */
    private MrlBuilding getNearestTarget(List<MrlBuilding> burningBuildings) {
        SortedSet<Pair<EntityID, Integer>> sortedBuildings = new TreeSet<Pair<EntityID, Integer>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        List<MrlBuilding> lessValueBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> edgeBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> highValueBuildings = getHighValueBuildings(lessValueBuildings, edgeBuildings);
        MrlBuilding nearestTarget = null;
        if (!highValueBuildings.isEmpty()) {
            for (MrlBuilding next : highValueBuildings) {
                sortedBuildings.add(new Pair<EntityID, Integer>(next.getID(), world.getDistance(world.getSelfPosition(), next.getSelfBuilding())));
            }
            nearestTarget = world.getMrlBuilding(sortedBuildings.first().first());
        } else if (!lessValueBuildings.isEmpty()) {
            for (MrlBuilding next : lessValueBuildings) {
                sortedBuildings.add(new Pair<EntityID, Integer>(next.getID(), world.getDistance(world.getSelfPosition(), next.getSelfBuilding())));
            }
            nearestTarget = world.getMrlBuilding(sortedBuildings.first().first());
        } else if (!edgeBuildings.isEmpty()) {
            for (MrlBuilding next : edgeBuildings) {
                sortedBuildings.add(new Pair<EntityID, Integer>(next.getID(), world.getDistance(world.getSelfPosition(), next.getSelfBuilding())));
            }
            nearestTarget = world.getMrlBuilding(sortedBuildings.first().first());
        } else if (!burningBuildings.isEmpty()) {
            for (MrlBuilding next : burningBuildings) {
                sortedBuildings.add(new Pair<EntityID, Integer>(next.getID(), world.getDistance(world.getSelfPosition(), next.getSelfBuilding())));
            }
            nearestTarget = world.getMrlBuilding(sortedBuildings.first().first());
        }
        return nearestTarget;
    }

    /**
     * This function is the last function written by me at may 2012
     * This is the collection of all other ideas that we had before. The difference is that now we use Value, so that they seem not to switch too much between clusters. This makes the supremacy in the performance.
     *
     * @param burningBuildings as previous functions
     * @return returns a sorted list of buildings which to extinguish
     */
    private MrlBuilding getOverallBestBuildings(List<MrlBuilding> burningBuildings) {
        SortedSet<Pair<EntityID, Double>> sortedBuildings = new TreeSet<Pair<EntityID, Double>>(ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        List<MrlBuilding> lessValueBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> edgeBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> highValueBuildings = getHighValueBuildings(lessValueBuildings, edgeBuildings);
        double distanceNormalizer = MRLConstants.MEAN_VELOCITY_OF_MOVING;
        MrlBuilding bestBuilding;
        for (MrlBuilding b : burningBuildings) {

            if (calculateEdgeBuildingValue(sortedBuildings, edgeBuildings, b, distanceNormalizer)) continue;

            if (calculateValueForLessValueBuildings(sortedBuildings, lessValueBuildings, b, distanceNormalizer))
                continue;

            if (calculateValueForHighValueBuildings(sortedBuildings, highValueBuildings, b, distanceNormalizer))
                continue;

            calculateValueForOtherBuildings(sortedBuildings, b, distanceNormalizer);

        }

        SortedSet<Pair<EntityID, Double>> tenTopBuildings = fireBrigadeUtilities.selectTop(10, sortedBuildings, ConstantComparators.DISTANCE_VALUE_COMPARATOR_DOUBLE);
        tenTopBuildings = fireBrigadeUtilities.oldReRankBuildings(tenTopBuildings);
        if (tenTopBuildings.size() > 0) {
            bestBuilding = world.getMrlBuilding(tenTopBuildings.first().first());
            return bestBuilding;
        } else {
            return null;
        }
    }

    private void calculateValueForOtherBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, MrlBuilding b, double distanceNormalizer) {
        double normalizedDistance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / distanceNormalizer;
        switch (b.getEstimatedFieryness()) {
            case 1:
                b.BUILDING_VALUE = 50;
                break;
            case 2:
                b.BUILDING_VALUE = 110;
                break;
            case 3:
                b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 210 : 410 - b.getEstimatedTemperature();
                break;
            case 0:
                b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 190 : 210;
                break;
            case 4:
                b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 160 : 190;
                break;
            case 5:
            case 6:
                b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 200 : 240;
                break;
            case 7:
                b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 300 : 380;
                break;
            default:
            case 8:         //Burnt Building
                b.BUILDING_VALUE = 19000;
                break;
        }
        b.BUILDING_VALUE += normalizedDistance;
        b.BUILDING_VALUE += b.getAdvantageRatio();
        if (b == lastTarget) {
            b.BUILDING_VALUE /= 1.3;
        }
            /*if (world.isMapHuge()) {
                System.out.println("Map is huge");
                b.BUILDING_VALUE *= 100;
            }*/
        sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
    }

    private boolean calculateValueForHighValueBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> highValueBuildings, MrlBuilding b, double distanceNormalizer) {
        double normalizedDistance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / distanceNormalizer;
        if (highValueBuildings.contains(b)) {
            switch (b.getEstimatedFieryness()) {
                case 1:
                    b.BUILDING_VALUE = 1;
                    break;
                case 2:
                    b.BUILDING_VALUE = 25;
                    break;
                case 3:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 150) ? 75 : 275 - b.getEstimatedTemperature();
                    break;
                case 0:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 130 : 160;
                    break;
                case 4:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 40 : 80;
                    break;
                case 5:
                case 6:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 75 : 95;
                    break;
                case 7:
                    b.BUILDING_VALUE = (b.getEstimatedTemperature() >= 35) ? 120 : 170;
                    break;
                case 8:   //Burnt building
                default:
                    b.BUILDING_VALUE = 15000;
                    break;
            }
            b.BUILDING_VALUE += normalizedDistance;
            b.BUILDING_VALUE += b.getAdvantageRatio();
            if (b == lastTarget) {
                b.BUILDING_VALUE /= 1.3;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
            return true;
        }
        return false;
    }

    private boolean calculateValueForLessValueBuildings(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> lessValueBuildings, MrlBuilding b, double distanceNormalizer) {
        double normalizedDistance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / distanceNormalizer;
        if (lessValueBuildings.contains(b)) {
            switch (b.getEstimatedFieryness() /*b.getSelfBuilding().getFieryness()*/) {
                case 1:
                case 2:
                    System.out.println("Something has gone wrong, building" + b.getID() + " Should not have been added to less values");
                    b.BUILDING_VALUE = 50;
                    break;
                case 3:
                    b.BUILDING_VALUE = 5000;
                    break;
                case 4:
                case 5:
                case 6:
                case 7:
                    b.BUILDING_VALUE = 7000;
                    break;
                case 0:
                    b.BUILDING_VALUE = 9000;
                    break;
                case 8:
                    b.BUILDING_VALUE = 20000;
                    break;
                default:
                    b.BUILDING_VALUE = 9000;
                    break;
            }

            b.BUILDING_VALUE += normalizedDistance;
            b.BUILDING_VALUE += b.getAdvantageRatio();
            if (b == lastTarget) {
                b.BUILDING_VALUE /= 1.3;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
            return true;
        }
        return false;
    }

    private boolean calculateEdgeBuildingValue(SortedSet<Pair<EntityID, Double>> sortedBuildings, List<MrlBuilding> edgeBuildings, MrlBuilding b, double distanceNormalizer) {
        double normalizedDistance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding()) / distanceNormalizer;
        if (edgeBuildings.contains(b)) {
            switch (b.getEstimatedFieryness()) {
                case 1:
                    b.BUILDING_VALUE = 50;
                    break;
                case 2:
                    b.BUILDING_VALUE = 500;
                    break;
                case 3:
                    b.BUILDING_VALUE = 10000;
                    break;
                case 4:
                case 5:
                case 6:
                case 7:
                    b.BUILDING_VALUE = 20000;
                    break;
                case 8:
                case 0:
                    b.BUILDING_VALUE = 25000;
                    break;
                default:
                    b.BUILDING_VALUE = 25000;
                    break;
            }
            b.BUILDING_VALUE += normalizedDistance;
            b.BUILDING_VALUE += b.getAdvantageRatio();
            if (b == lastTarget) {
                b.BUILDING_VALUE /= 1.3;
            }
            sortedBuildings.add(new Pair<EntityID, Double>(b.getID(), b.BUILDING_VALUE));
            return true;
        }
        return false;
    }


    public List<MrlBuilding> sortedSetToList(SortedSet<Pair<EntityID, Integer>> sortedBuildings) {
        List<MrlBuilding> sortedList = new ArrayList<MrlBuilding>();
        for (Pair<EntityID, Integer> pair : sortedBuildings) {
            sortedList.add(world.getMrlBuilding(pair.first()));
        }
        return sortedList;
    }

    /**
     * This function is another overall function, but doesn't work as well as the Overall function. Because they switch a lot when they are in a multi-cluster map
     *
     * @param burningBuildings as previous
     * @return as privious
     */
    private MrlBuilding getBestBuildingByDistanceAndFierynessAndDirection(List<MrlBuilding> burningBuildings) {
        List<MrlBuilding> lessValueBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> edgeBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> highValueBuildings = getHighValueBuildings(lessValueBuildings, edgeBuildings);

        List<Pair<EntityID, Integer>> highValueFieryness1Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> highValueFieryness2Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> highValueFieryness3Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> highValueOtherDistances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> fieryness1Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> fieryness2Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> fieryness3Distances = new ArrayList<Pair<EntityID, Integer>>();
        List<Pair<EntityID, Integer>> otherDistances = new ArrayList<Pair<EntityID, Integer>>();

        for (MrlBuilding b : burningBuildings) {
            if (highValueBuildings.contains(b)) {
                switch (b.getSelfBuilding().getFieryness()) {
                    case 1:
                        highValueFieryness1Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getMyDistanceTo(b.getSelfBuilding())));
                        break;
                    case 2:
                        highValueFieryness2Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getMyDistanceTo(b.getSelfBuilding())));
                        break;
                    case 3:
                        highValueFieryness3Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                        break;
                    case 5:
                    case 4:
                    case 6:
                    case 7:
                    case 8:
                    default:
                        highValueOtherDistances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                        break;
                }
                continue;
            }
            switch (b.getSelfBuilding().getFieryness()) {
                case 1:
                    fieryness1Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
                case 2:
                    fieryness2Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
                case 3:
                    fieryness3Distances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
                case 5:
                case 4:
                case 6:
                case 7:
                case 8:
                default:
                    otherDistances.add(new Pair<EntityID, Integer>(b.getID(), world.getDistance(world.getSelfPosition(), b.getSelfBuilding())));
                    break;
            }
        }

        Collections.sort(highValueFieryness1Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(highValueFieryness2Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(highValueFieryness3Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(highValueOtherDistances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(fieryness1Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(fieryness2Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(fieryness3Distances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);
        Collections.sort(otherDistances, ConstantComparators.DISTANCE_VALUE_COMPARATOR);

        Pair<EntityID, Integer> result = new Pair<EntityID, Integer>(new EntityID(0), 0);
        if (highValueFieryness1Distances.size() > 0) {
            result = highValueFieryness1Distances.get(0);
        } else if (highValueFieryness2Distances.size() > 0) {
            result = highValueFieryness2Distances.get(0);
        } else if (highValueFieryness3Distances.size() > 0) {
            result = highValueFieryness3Distances.get(0);
        } else if (fieryness1Distances.size() > 0) {
            result = fieryness1Distances.get(0);
        } else if (fieryness2Distances.size() > 0) {
            result = fieryness2Distances.get(0);
        } else if (fieryness3Distances.size() > 0) {
            result = fieryness3Distances.get(0);
        } else if (otherDistances.size() > 0) {
            result = otherDistances.get(0);
        } else if (highValueOtherDistances.size() > 0) {
            result = highValueOtherDistances.get(0);
        }

        MrlBuilding best = world.getMrlBuilding(result.first());
        return best;
    }

    /**
     * this function returns the high valued buildings that are in the direction triangle
     *
     * @return
     */
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
//                highValueBuildings = directionManager.getHighValueBuildings();

            } else {
//                highValueBuildings = directionManager.getHighValueBuildings();
//                if (MRLConstants.FILL_VIEWER_DATA)
//                {
//                    for (MrlBuilding building : highValueBuildings){
//                        borderDirectionBuildings.add(building.getSelfBuilding());
//                    }
//                }
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

    /**
     * this is the very first function that mostafa shabani wrote, It seems that this doesn't work very well
     *
     * @param preTargetBuilding
     * @param burningBuildings
     * @return
     */
    private MrlBuilding getBestBuilding(MrlBuilding preTargetBuilding, List<MrlBuilding> burningBuildings)// This function was written by Mostafa Shabani, I just changed it a bit to be able to use here
    {
        MrlBuilding best = null;
        double maxVal = Double.MIN_VALUE;
        for (MrlBuilding b : burningBuildings) {

            boolean leavingBuilding = false;

            double val = (b.getBuildingAreaTempValue() * 1) + (b.getBuildingRadiation() * 2) - (b.getNeighbourRadiation() * 1);
            if (b.getEstimatedFieryness() == 3 && b.getEstimatedTemperature() < 200) {
                val *= 0.7;
                leavingBuilding = true;
            }

            if (preTargetBuilding != null && b.equals(preTargetBuilding) && !leavingBuilding) {
                val *= 1.54;
                switch (buildingWaterSpray) {
                    case 0:
                        break;
                    case 1:
                        val += 0.3;
                        break;
                    case 2:
                        val += 0.5;
                        break;
                    case 3:
                        val += 0.7;
                        break;
                    case 4:
                        val += 0.9;
                        break;
                    default:
                        val += 1.1;
                        break;
                }
            }

            if (b != null) {
                double distance = world.getDistance(world.getSelfPosition(), b.getSelfBuilding());
                double scale = distance / world.getMapWidth();
                int dis = (int) (scale * 100);
                /*switch (dis)
                {
                    case 0:
                        val *= 1.9;
                        break;
                    case 1:
                        val *= 1.7;
                        break;
                    case 2:
                        val *= 1.5;
                        break;
                    case 3:
                        val *= 1.3;
                        break;
                    case 4:
                        val *= 1.1;
                        break;
                    case 5:
                        val *= 1;
                        break;
                    case 6:
                        val *= 0.9;
                        break;
                    case 7:
                        val *= 0.8;
                        break;
                    case 8:
                        val *= 0.7;
                        break;
                    case 9:
                        val *= 0.6;
                        break;
                    case 10:
                        val *= 0.5;
                        break;
                }*/
                switch (dis) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        //val += 0.95;
                        val *= 2;
                        break;
                }
            }

            if (b.getEstimatedFieryness() == 8) {
                val *= 0.5;
            }

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
            //if (FILL_VIEWER_DATA) {
            b.BUILDING_VALUE = val;
            //}
            if (val > maxVal) {
                maxVal = val;
                best = b;
            }
        }

        if (preTargetBuilding != null && best != null && preTargetBuilding.equals(best)) {
            buildingWaterSpray++;
        } else {
            buildingWaterSpray = 0;
        }
        return best;
    }


}
