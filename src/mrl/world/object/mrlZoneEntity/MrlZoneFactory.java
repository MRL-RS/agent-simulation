package mrl.world.object.mrlZoneEntity;

import mrl.LaunchMRL;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;

import java.awt.*;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Mostafa Shabani.
 * Date: 5/13/11
 * Time: 7:26 PM
 */
public class MrlZoneFactory {
    private MrlWorld world;
    private int distance;
    //zone id generator
    private int idGenerator = 0;


    public MrlZoneFactory(MrlWorld world) {
        this.world = world;
        distance = setDistance(world.getMapHeight(), world.getMapWidth());
    }

    public MrlZones createZones(String fileName) {
        boolean flag = true;
        Random random;
        if (world.getSelf() instanceof MrlPlatoonAgent) {
            random = world.getPlatoonAgent().getRandom();
        } else {
            random = world.getMrlCentre().getRandom();
            return new MrlZones(random);
        }
        MrlZones zones = new MrlZones(random);

        try {

            BufferedReader bf = new BufferedReader(new FileReader(fileName));
            String str = bf.readLine();
            int id;
            MrlZone zone = null;
            while (str != null) {
                if (!str.isEmpty()) {
                    if (str.startsWith("id: ")) {
                        if (zone != null) {
                            zones.add(zone);
                            zone.initZoneInfo();
                        }
                        id = Integer.parseInt(str.replaceFirst("id: ", ""));
                        zone = new MrlZone(world, id);
                    } else if (str.startsWith("neighbours: ") && zone != null) {
                        String s = str.replaceFirst("neighbours: ", "");
                        String[] neighbourIds = s.split(", ");
                        for (String neighbour : neighbourIds) {
                            if (!neighbour.isEmpty()) {
                                int neighbourId = Integer.parseInt(neighbour);
                                zone.addNeighborZoneIds(neighbourId);
                            }
                        }
                    } else if (zone != null) {

                        String[] values = str.split(",");
                        int x = Integer.parseInt(values[0]);
                        int y = Integer.parseInt(values[1]);
                        MrlBuilding building = null;
                        try {
                            Object obj = world.getBuildingInPoint(x, y);
                            if (obj instanceof Building) {
                                building = world.getMrlBuilding(((Building) obj).getID());
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                        if (building != null) {
                            zone.addBuilding(building);
                            zones.addBuildingZoneMap(building.getID(), zone);
                            building.setZoneId(zone.getId());
                        }
                    }
                }
                str = bf.readLine();
            }
            if (zone != null) {
                zones.add(zone);
                zone.initZoneInfo();
            }

        } catch (Exception e1) {

            zones = dynamicZoning();
            findZoneNeighbours(zones);
//            reWriteMapZoneFile(zones);
        }

        zones.fillZoneAroundObjects();
//        zones.createBuildingMap();
        if (flag) {
//            System.out.print("  Lz: success. ");
        }

        return zones;
    }


    /**
     * In this function, build zone from building with distance with to parameters:
     * 1- There aren't any road between two buildings
     * 2- Distance isn't longer that dis, if distance between two building longer than dis, these two building are two different zones.
     *
     * @return all zones
     */

    private MrlZones dynamicZoning() {
        ArrayList<MrlBuilding> uzb = new ArrayList<MrlBuilding>();
        uzb.addAll(world.getMrlBuildings());
        MrlZone zone;
        MrlZones zones = new MrlZones(world.getPlatoonAgent().getRandom());

        while (uzb.size() > 0) {
            zone = new MrlZone(world, idGenerator++);
            zone.addBuilding(uzb.get(0));
            zones.addBuildingZoneMap(uzb.get(0).getID(), zone);
            int j = 0;

            while (j < zone.size()) {
                MrlBuilding mainBd = zone.get(j);

                addAroundBuildings(mainBd.getSelfBuilding(), uzb, zone, zones);
                uzb.removeAll(zone);

                j++;
            }

            zones.add(zone);
            zone.initZoneInfo();
        }
        return zones;
    }

    private void addAroundBuildings(Building mainBd, ArrayList<MrlBuilding> bdList, MrlZone zone, MrlZones zones) {

        long radius = getRadius(world.getMapHeight(), world.getMapWidth());

        for (MrlBuilding bd : bdList) {
            if (!(mainBd.getID().equals(bd.getSelfBuilding().getID())) && minBuildingDis(mainBd, bd.getSelfBuilding()) < radius) {

                if (!isAnyRoadBetween(mainBd, bd.getSelfBuilding())) {
                    bd.setZoneId(zone.getId());
                    zone.addBuilding(bd);
                    zones.addBuildingZoneMap(bd.getID(), zone);

                }
            }
        }
    }

    private long getRadius(Integer w, Integer h) {

        BigInteger num1 = new BigInteger(w.toString());
        BigInteger num2 = new BigInteger(h.toString());
        BigInteger multiply = num2.multiply(num1);

        BigInteger range1 = new BigInteger("3000000000000");


        if (multiply.compareTo(range1) >= 0)
            return 25000;

        range1 = new BigInteger("500000000000");
        if (multiply.compareTo(range1) >= 0)
            return 20000;
        return 8000;
    }

    //
    private int minBuildingDis(Building building1, Building building2) {
        int distance;
        int minDistance = Integer.MAX_VALUE;
        int x1, y1, x2, y2;

        for (int n = 0; n < building1.getApexList().length; n++) {
            x1 = building1.getApexList()[n];
            y1 = building1.getApexList()[++n];
            for (int m = 0; m < building2.getApexList().length; m++) {
                x2 = building2.getApexList()[m];
                y2 = building2.getApexList()[++m];
                distance = Util.distance(x1, y1, x2, y2);
                if (distance < minDistance) {
                    minDistance = distance;
                }
            }
        }

        return minDistance;
    }

    private boolean isAnyRoadBetween(Building bd1, Building bd2) {
        Road road;
        int dist = world.getDistance(bd1, bd2);

        for (StandardEntity standardEntity : world.getObjectsInRange(bd1, dist)) {

            if (standardEntity instanceof Road) {
                road = (Road) standardEntity;
                if (hasCollision(road, new Point(bd1.getX(), bd1.getY()), new Point(bd2.getX(), bd2.getY()))) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean hasCollision(Road road, Point p1, Point p2) {
        for (Edge edge : road.getEdges()) {

            if (edgeIntersection(edge.getStart(), edge.getEnd(), new Point2D(p1.getX(), p1.getY()), new Point2D(p2.getX(), p2.getY()))) {
                return true;
            }
        }

        return false;
    }

    private boolean edgeIntersection(Point2D a, Point2D b, Point2D c, Point2D d) {

        double det = determinant(b.minus(a), c.minus(d));
        double t = determinant(c.minus(a), c.minus(d)) / det;
        double u = determinant(b.minus(a), c.minus(a)) / det;

        if ((t < 0) || (u < 0) || (t > 1) || (u > 1))
            return false;
        return true;
    }

    private double determinant(Vector2D p1, Vector2D p2) {
        return p1.getX() * p2.getY() - p1.getY() * p2.getX();
    }

    public int setDistance(int mapHeight, int mapWidth) {

        BigInteger bigInteger = new BigInteger(String.valueOf(mapHeight * mapWidth));
        BigInteger cmp = bigInteger.subtract(new BigInteger(String.valueOf("3000000000000")));
        if (cmp.intValue() <= 0) {
            BigInteger b = bigInteger.subtract(new BigInteger(String.valueOf("1000000000")));
            if (b.intValue() >= 0)
                return 22000;

            else {
//                System.out.println("7000");
                return 14000;
            }
        }
//        System.out.println("50000");
        return 50000;
    }

    private void findZoneNeighbours(MrlZones zones) {

        MrlZone bZone;
        for (MrlZone zone : zones) {

            for (MrlBuilding building : zone) {
                for (StandardEntity standardEntity : world.getObjectsInRange(building.getSelfBuilding(), distance)) {
                    if (standardEntity instanceof Building) {
                        if (!zone.contains(world.getMrlBuilding(standardEntity.getID()))) {

                            bZone = zones.getBuildingZone(standardEntity.getID());
                            if (bZone != null) {
                                zone.addNeighborZoneIds(bZone.getId());
                                bZone.addNeighborZoneIds(zone.getId());
                            }

                        }
                    }
                }
            }
        }
    }

//    public void checkZoneNeighbours(MrlZones zones) {
//
//        for (MrlZone zone : zones) {
//            int zoneID = zone.getId();
//            for (MrlZone z : zone.getNeighbors()) {
//                if (!(z.getNeighborZoneIds().contains(zoneID))) {
//                    z.getNeighbors().add(zone);
//                }
//            }
//        }
//    }

    public void reWriteMapZoneFile(MrlZones zones) {

        File file = new File(MRLConstants.PRECOMPUTE_DIRECTORY + world.getMapName() + ".zone");
        try {
            if (LaunchMRL.shouldPrecompute) {
                PrintWriter printWriter = new PrintWriter(new FileWriter(file));
                for (MrlZone zone : zones) {
                    printWriter.println("");
                    printWriter.println("id: " + zone.getId());
                    printWriter.print("neighbours: ");

                    for (int z : zone.getNeighborZoneIds()) {
                        printWriter.print(z + ", ");
                    }
                    printWriter.println();

                    for (MrlBuilding building : zone) {
                        printWriter.println(building.getSelfBuilding().getX() + "," + building.getSelfBuilding().getY());

                    }

                }

                printWriter.flush();
                printWriter.close();
            }

        } catch (IOException e) {
            System.err.println("error in write file.");
        }
    }

}
