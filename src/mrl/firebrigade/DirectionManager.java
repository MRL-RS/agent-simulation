package mrl.firebrigade;

import javolution.util.FastSet;
import mrl.common.Util;
import mrl.helper.CivilianHelper;
import mrl.world.object.FireCluster;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User: roohola
 * Date: 6/5/11
 * Time: 11:18 PM
 */
public class DirectionManager {
    private static final double CIVILIAN_VALUE = 100;
    private static final double POSSIBLE_BUILDING_VALUE = 5;
    protected boolean directionEnabled = false;
    private static final int COLLISION_CONSTANT = 50000;
    protected double directValue[] = new double[4];
    protected MrlFireBrigadeWorld world;
    protected int maxWater;
    private CivilianHelper civilianHelper;
    java.util.List<ArrayList<MrlZone>> zonesInDirection = new ArrayList<ArrayList<MrlZone>>();
    private int direction;

    public DirectionManager(MrlFireBrigadeWorld world, int maxWater) {
        this.world = world;
        this.maxWater = maxWater;
        zonesInDirection.add(Direction.UP.dir(), new ArrayList<MrlZone>());
        zonesInDirection.add(Direction.RIGHT.dir(), new ArrayList<MrlZone>());
        zonesInDirection.add(Direction.DOWN.dir(), new ArrayList<MrlZone>());
        zonesInDirection.add(Direction.LEFT.dir(), new ArrayList<MrlZone>());
        this.civilianHelper = world.getHelper(CivilianHelper.class);
    }

//    protected void chooseFireCluster() {
//        for (int i = 0; i < zonesInDirection.size(); i++) {
//            directValue[i] = 0;
//            zonesInDirection.get(i).clear();
//        }
//
//        FireCluster maxFireCluster = null;
//        int maxWaterNeeded = Integer.MIN_VALUE;
//
//        for (FireCluster fireCluster : world.getFireClusters()) {
//            int waterNeeded = 0;
//            for (MrlZone zone : fireCluster) {
//                for (MrlBuilding building : zone) {
//                    if (building.isBurning()) {
//                        waterNeeded += world.getCoolingEstimator().waterNeededToExtinguish(building);
//                    }
//                }
//            }
//
//            if (waterNeeded > maxWaterNeeded) {
//                maxWaterNeeded = waterNeeded;
//                maxFireCluster = fireCluster;
//            }
//        }
//
//        directionEnabled = false;
//
//        if (maxFireCluster != null) {
//            directionEnabled = (maxFireCluster.getTotalArea() > (world.getWorldTotalArea() / 4)) || ((maxWater * Math.max(1, world.getFreeFireBrigades().size()) * 0.75) < maxWaterNeeded);
//        }
//        if (directionEnabled) {
//
//            setDirectValue(-1, 1);
////            collision(maxFireCluster);
//            chooseDirection(maxFireCluster);
//        }
//    }


    private void setDirectValue(int index, double value) {
        if (index < 0) {
            directValue[0] = 1.0;
            directValue[1] = 1.0;
            directValue[2] = 1.0;
            directValue[3] = 1.0;
        } else {
            directValue[index] = value;
        }
    }

    private void collision(FireCluster fireCluster) {

        int maxX = Math.max(Math.abs((int) fireCluster.getCenter().getX() - fireCluster.getMaxX()), Math.abs((int) fireCluster.getCenter().getX() - fireCluster.getMinX()));
        int maxY = Math.max(Math.abs((int) fireCluster.getCenter().getY() - fireCluster.getMaxY()), Math.abs((int) fireCluster.getCenter().getY() - fireCluster.getMinY()));
        int maxPointOfFireCluster = Math.max(maxX, maxY);

        int maxXf;
        int maxYf;
        int maxPointOfF;
        for (FireCluster f : world.getFireClusters()) {
            maxXf = Math.max(Math.abs((int) f.getCenter().getX() - f.getMaxX()), Math.abs((int) f.getCenter().getX() - f.getMinX()));
            maxYf = Math.max(Math.abs((int) f.getCenter().getY() - f.getMaxY()), Math.abs((int) f.getCenter().getY() - f.getMinY()));
            maxPointOfF = Math.max(maxXf, maxYf);


            int distance = Util.distance(fireCluster.getCenter(), f.getCenter());
            int clusterDistance = Math.abs(distance - (maxPointOfF + maxPointOfFireCluster));


            if (clusterDistance <= COLLISION_CONSTANT) {
                if (f.getCenter().getX() > fireCluster.getCenter().getX()) {
                    directValue[Direction.RIGHT.dir()] *= (clusterDistance / 100000);
                } else {
                    directValue[Direction.LEFT.dir()] *= (clusterDistance / 100000);
                }
                if (f.getCenter().getY() > fireCluster.getCenter().getY()) {
                    directValue[Direction.UP.dir()] *= (clusterDistance / 100000);
                } else {
                    directValue[Direction.UP.dir()] *= (clusterDistance / 100000);
                }
            }

        }
    }

    private void chooseDirection(FireCluster fireCluster) {
        double x = fireCluster.getCenter().getX();
        double y = fireCluster.getCenter().getY();


        for (MrlZone zone : fireCluster) {
            if (zone.getCenter().getX() > x) {
                zonesInDirection.get(Direction.RIGHT.dir()).add(zone);
            } else {
                zonesInDirection.get(Direction.LEFT.dir()).add(zone);
            }

            if (zone.getCenter().getY() > y) {
                zonesInDirection.get(Direction.UP.dir()).add(zone);
            } else {
                zonesInDirection.get(Direction.DOWN.dir()).add(zone);
            }
        }

        Set<EntityID> possibleBuilding = new FastSet<EntityID>();

        for (StandardEntity standardEntity : world.getCivilians()) {
            Civilian civilian = (Civilian) standardEntity;
            if (!civilian.isPositionDefined()) {
                possibleBuilding.addAll(civilianHelper.getPossibleBuildings(standardEntity.getID()));
            }
        }

        for (EntityID entityId : possibleBuilding) {
            Building building = (Building) world.getEntity(entityId);
            if (building.isFierynessDefined()) {
                if (building.getFieryness() == 0 || building.getFieryness() == 4) {
                    if (building.getX() > x) {
                        directValue[Direction.RIGHT.dir()] += POSSIBLE_BUILDING_VALUE;
                    } else {
                        directValue[Direction.LEFT.dir()] += POSSIBLE_BUILDING_VALUE;
                    }
                    if (building.getY() > y) {
                        directValue[Direction.UP.dir()] += POSSIBLE_BUILDING_VALUE;
                    } else {
                        directValue[Direction.DOWN.dir()] += POSSIBLE_BUILDING_VALUE;
                    }
                }
            } else {
                if (!building.isOnFire()) {
                    if (!world.getZones().getBuildingZone(building.getID()).isBurned()) {
                        if (building.getX() > x) {
                            directValue[Direction.RIGHT.dir()] += POSSIBLE_BUILDING_VALUE;
                        } else {
                            directValue[Direction.LEFT.dir()] += POSSIBLE_BUILDING_VALUE;
                        }
                        if (building.getY() > y) {
                            directValue[Direction.UP.dir()] += POSSIBLE_BUILDING_VALUE;
                        } else {
                            directValue[Direction.DOWN.dir()] += POSSIBLE_BUILDING_VALUE;
                        }
                    }
                }
            }
        }

        List<MrlZone> allZones = new ArrayList<MrlZone>();
        allZones.addAll(world.getZones());

        for (FireCluster fCluster : world.getFireClusters()) {
            allZones.removeAll(fCluster);
        }

        for (MrlZone zone : allZones) {
            for (MrlBuilding building : zone.getUnBurnedBuildings()) {
                if (building.getSelfBuilding().getX() > x) {
                    directValue[Direction.RIGHT.dir()] += building.getSelfBuilding().getTotalArea();
                } else {
                    directValue[Direction.LEFT.dir()] += building.getSelfBuilding().getTotalArea();
                }

                if (building.getSelfBuilding().getY() > y) {
                    directValue[Direction.UP.dir()] += building.getSelfBuilding().getTotalArea();
                } else {
                    directValue[Direction.DOWN.dir()] += building.getSelfBuilding().getTotalArea();
                }
            }

        }

        for (StandardEntity standardEntity : world.getCivilians()) {

            Civilian civilian = (Civilian) standardEntity;
            if (civilian.isPositionDefined()) {

                if (world.getEntity(civilian.getPosition()) instanceof Building) {

                    Building building = (Building) world.getEntity(civilian.getPosition());
                    if (!building.isOnFire()) {
//                        if (zone == null) {
//                            System.err.println("");
//                        }
                        if (building.isFierynessDefined() && building.getFieryness() < 5) {

                            if (civilian.isHPDefined() && civilian.getHP() > 0) {
                                if (building.getX() > x) {
                                    directValue[Direction.RIGHT.dir()] += CIVILIAN_VALUE;
                                } else {
                                    directValue[Direction.LEFT.dir()] += CIVILIAN_VALUE;
                                }
                                if (building.getY() > y) {
                                    directValue[Direction.UP.dir()] += CIVILIAN_VALUE;
                                } else {
                                    directValue[Direction.DOWN.dir()] += CIVILIAN_VALUE;
                                }
                            }
                        }
                    }
                }
            }
        }


        double maxValue = Double.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            if (directValue[i] > maxValue) {
                maxValue = directValue[i];
                direction = i;
            }
        }

    }


    public java.util.List<MrlZone> getTargetDirectZones() {
        return Collections.unmodifiableList(zonesInDirection.get(direction));
    }

    public boolean isDirectionEnabled() {
        return directionEnabled;
    }
}
//package mrl.firebrigade;
//
//import javolution.util.FastSet;
//import mrl.common.Util;
//import mrl.helper.CivilianHelper;
//import mrl.world.object.FireCluster;
//import mrl.world.object.MrlBuilding;
//import mrl.world.object.mrlZoneEntity.MrlZone;
//import rescuecore2.standard.entities.Building;
//import rescuecore2.standard.entities.Civilian;
//import rescuecore2.standard.entities.StandardEntity;
//import rescuecore2.worldmodel.EntityID;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Set;
//
///**
// * User: roohola
// * Date: 6/5/11
// * Time: 11:18 PM
// */
//public class DirectionManager {
//    private static final double CIVILIAN_VALUE = 100;
//    private static final double POSSIBLE_BUILDING_VALUE = 5;
//    protected boolean directionEnabled = false;
//    private static final int COLLISION_CONSTANT = 50000;
//    protected double directValue[] = new double[4];
//    protected MrlFireBrigadeWorld world;
//    protected int maxWater;
//    private CivilianHelper civilianHelper;
//    java.util.List<ArrayList<MrlZone>> zonesInDirection = new ArrayList<ArrayList<MrlZone>>();
//    private int direction;
//
//    public DirectionManager(MrlFireBrigadeWorld world, int maxWater) {
//        this.world = world;
//        this.maxWater = maxWater;
//        zonesInDirection.add(Direction.UP.dir(), new ArrayList<MrlZone>());
//        zonesInDirection.add(Direction.RIGHT.dir(), new ArrayList<MrlZone>());
//        zonesInDirection.add(Direction.DOWN.dir(), new ArrayList<MrlZone>());
//        zonesInDirection.add(Direction.LEFT.dir(), new ArrayList<MrlZone>());
//        this.civilianHelper = world.getHelper(CivilianHelper.class);
//    }
//
//    protected void chooseFireCluster() {
//        for (int i = 0; i < zonesInDirection.size(); i++) {
//            directValue[i] = 0;
//            zonesInDirection.get(i).clear();
//        }
//
//        FireCluster maxFireCluster = null;
//        int maxWaterNeeded = Integer.MIN_VALUE;
//
//        for (FireCluster fireCluster : world.getFireClusters()) {
//            int waterNeeded = 0;
//            for (MrlZone zone : fireCluster) {
//                for (MrlBuilding building : zone) {
//                    if (building.isBurning()) {
//                        waterNeeded += world.getCoolingEstimator().waterNeededToExtinguish(building);
//                    }
//                }
//            }
//
//            if (waterNeeded > maxWaterNeeded) {
//                maxWaterNeeded = waterNeeded;
//                maxFireCluster = fireCluster;
//            }
//        }
//
//        directionEnabled = false;
//
//        if (maxFireCluster != null) {
//            directionEnabled = (maxFireCluster.getTotalArea() > (world.getWorldTotalArea() / 4)) || ((maxWater * Math.max(1, world.getFreeFireBrigades().size()) * 0.75) < maxWaterNeeded);
//        }
//        if (directionEnabled) {
//
//            setDirectValue(-1, 1);
////            collision(maxFireCluster);
//            possibleBuilding(maxFireCluster);
//            chooseDirection(maxFireCluster);
//        }
//    }
//
//    private void possibleBuilding(FireCluster fireCluster) {
//        Set<EntityID> possibleBuilding = new FastSet<EntityID>();
//
//        for (StandardEntity standardEntity : world.getCivilians()) {
//            Civilian civilian = (Civilian) standardEntity;
//            if (!civilian.isPositionDefined()) {
//                possibleBuilding.addAll(civilianHelper.getPossibleBuildings(standardEntity.getID()));
//            }
//        }
//
//        for (EntityID entityId : possibleBuilding) {
//            Building building = (Building) world.getEntity(entityId);
//
//            if (building.getX() > fireCluster.getCenter().getX()) {
//                directValue[Direction.RIGHT.dir()] += POSSIBLE_BUILDING_VALUE;
//            } else {
//                directValue[Direction.LEFT.dir()] += POSSIBLE_BUILDING_VALUE;
//            }
//            if (building.getY() > fireCluster.getCenter().getY()) {
//                directValue[Direction.UP.dir()] += POSSIBLE_BUILDING_VALUE;
//            } else {
//                directValue[Direction.UP.dir()] += POSSIBLE_BUILDING_VALUE;
//            }
//        }
//    }
//
//
//    private void setDirectValue(int index, double value) {
//        if (index < 0) {
//            directValue[0] = 1.0;
//            directValue[1] = 1.0;
//            directValue[2] = 1.0;
//            directValue[3] = 1.0;
//        } else {
//            directValue[index] = value;
//        }
//    }
//
//    private void collision(FireCluster fireCluster) {
//
//        int maxX = Math.max(Math.abs((int) fireCluster.getCenter().getX() - fireCluster.getMaxX()), Math.abs((int) fireCluster.getCenter().getX() - fireCluster.getMinX()));
//        int maxY = Math.max(Math.abs((int) fireCluster.getCenter().getY() - fireCluster.getMaxY()), Math.abs((int) fireCluster.getCenter().getY() - fireCluster.getMinY()));
//        int maxPointOfFireCluster = Math.max(maxX, maxY);
//
//        int maxXf;
//        int maxYf;
//        int maxPointOfF;
//        for (FireCluster f : world.getFireClusters()) {
//            maxXf = Math.max(Math.abs((int) f.getCenter().getX() - f.getMaxX()), Math.abs((int) f.getCenter().getX() - f.getMinX()));
//            maxYf = Math.max(Math.abs((int) f.getCenter().getY() - f.getMaxY()), Math.abs((int) f.getCenter().getY() - f.getMinY()));
//            maxPointOfF = Math.max(maxXf, maxYf);
//
//
//            int distance = Util.distance(fireCluster.getCenter(), f.getCenter());
//            int clusterDistance = Math.abs(distance - (maxPointOfF + maxPointOfFireCluster));
//            double value = 0;
//
//            if (clusterDistance <= COLLISION_CONSTANT) {
//                if (f.getCenter().getX() > fireCluster.getCenter().getX()) {
//                    directValue[Direction.RIGHT.dir()] *= (clusterDistance / 100000);
//                } else {
//                    directValue[Direction.LEFT.dir()] *= (clusterDistance / 100000);
//                }
//                if (f.getCenter().getY() > fireCluster.getCenter().getY()) {
//                    directValue[Direction.UP.dir()] *= (clusterDistance / 100000);
//                } else {
//                    directValue[Direction.UP.dir()] *= (clusterDistance / 100000);
//                }
//            }
//
//        }
//    }
//
//    private void chooseDirection(FireCluster fireCluster) {
//
//        double zonesArea[] = new double[4];
//
//
//        for (MrlZone zone : fireCluster) {
//            if (zone.getCenter().getX() > fireCluster.getCenter().getX()) {
//                zonesInDirection.get(Direction.RIGHT.dir()).add(zone);
//            } else {
//                zonesInDirection.get(Direction.LEFT.dir()).add(zone);
//            }
//            if (zone.getCenter().getY() > fireCluster.getCenter().getY()) {
//                zonesInDirection.get(Direction.UP.dir()).add(zone);
//            } else {
//                zonesInDirection.get(Direction.DOWN.dir()).add(zone);
//            }
//        }
//
//        for (MrlZone zone : world.getZones()) {
//            if (zone.getCenter().getX() > fireCluster.getCenter().getX()) {
//                for (MrlBuilding building : zone.getUnBurnedBuildings()) {
//                    zonesArea[Direction.RIGHT.dir()] += building.getSelfBuilding().getTotalArea();
//                }
//            } else {
//                for (MrlBuilding building : zone.getUnBurnedBuildings()) {
//                    zonesArea[Direction.LEFT.dir()] += building.getSelfBuilding().getTotalArea();
//                }
//            }
//            if (zone.getCenter().getY() > fireCluster.getCenter().getY()) {
//                for (MrlBuilding building : zone.getUnBurnedBuildings()) {
//                    zonesArea[Direction.UP.dir()] += building.getSelfBuilding().getTotalArea();
//                }
//            } else {
//                for (MrlBuilding building : zone.getUnBurnedBuildings()) {
//                    zonesArea[Direction.DOWN.dir()] += building.getSelfBuilding().getTotalArea();
//                }
//            }
//
//
//            System.arraycopy(zonesArea, 0, directValue, 0, zonesInDirection.size());
//
//            civilianDirection(fireCluster.getCenter().x, fireCluster.getCenter().y);
//
//            double maxValue = Double.MIN_VALUE;
//            for (int i = 0; i < 4; i++) {
//                if (directValue[i] > maxValue) {
//                    maxValue = directValue[i];
//                    direction = i;
//                }
//            }
//        }
//    }
//
//    private void civilianDirection(int x, int y) {
//        for (StandardEntity standardEntity : world.getCivilians()) {
//            Civilian civilian = (Civilian) standardEntity;
//            if (civilian.isPositionDefined()) {
//
//                if (world.getEntity(civilian.getPosition()) instanceof Building) {
//
//                    Building building = (Building) world.getEntity(civilian.getPosition());
//                    MrlZone zone = world.getZones().getBuildingZone(building.getID());
//                    if (zone == null) {
//                        System.err.println("");
//                    }
//                    if (civilian.isHPDefined() && civilian.getHP() > 0) {
//                        if (zone.getCenter().getX() > x) {
//                            directValue[Direction.RIGHT.dir()] += CIVILIAN_VALUE;
//                        } else {
//                            directValue[Direction.LEFT.dir()] += CIVILIAN_VALUE;
//                        }
//                        if (zone.getCenter().getY() > y) {
//                            directValue[Direction.UP.dir()] += CIVILIAN_VALUE;
//                        } else {
//                            directValue[Direction.DOWN.dir()] += CIVILIAN_VALUE;
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    public java.util.List<MrlZone> getTargetDirectZones() {
//        return Collections.unmodifiableList(zonesInDirection.get(direction));
//    }
//
//    public boolean isDirectionEnabled() {
//        return directionEnabled;
//    }
//}
