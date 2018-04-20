package mrl.partition;

import javolution.util.FastMap;
import mrl.LaunchMRL;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.platoon.MrlPlatoonAgent;
import mrl.world.MrlWorld;
import mrl.world.routing.path.Path;
import mrl.world.routing.pathPlanner.IPathPlanner;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by P.D.G.
 * User: mrl
 * Date: Sep 24, 2010
 * Time: 9:04:54 AM
 */
public class PreRoutingPartitions extends ArrayList<Partition> implements PartitionsI {
    private MrlWorld world;
    private ArrayList<Partition> mapPartitions;
    private FastMap<Integer, Partition> idPartitionMap;
    private int numberOfPartitions;
    private int rowNums;
    private int columnNums;
    private int eachPartitionWidth;
    private int eachPartitionHeight;


    public PreRoutingPartitions(MrlWorld world) {
        this.world = world;

        mapPartitions = new ArrayList<Partition>();
        idPartitionMap = new FastMap<Integer, Partition>();
        makePartitions();
        findCentralAreaOfEachPartitionAndRemoveNullPartitions();


        String mapName = world.getMapName();
        if (mapName == null) {
            mapName = Long.toString(world.getUniqueMapNumber());
        }


        String filename = MRLConstants.PRECOMPUTE_DIRECTORY + mapName + ".ptt";


        if (new File(filename).exists()) {
            try {
                ShortestPaths shortestPaths = (ShortestPaths) Util.readObject(filename);
                for (Partition partition : mapPartitions) {
                    for (Integer id : shortestPaths.keySet()) {
                        if (partition.getId().equals(id)) {
                            partition.setPathsToOthers(shortestPaths.get(id));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Cannot load PTT data!!!!   " + e.getMessage());
//                e.printStackTrace();
                computeShortestPathsFromEachPartitionToAllAndTheirMoveTime();
                ShortestPaths shortestPaths = new ShortestPaths();

                for (Partition partition : mapPartitions) {
                    shortestPaths.put(partition.getId(), partition.getPathsToOthers());
                }
                try {
                    if (LaunchMRL.shouldPrecompute) {
                        Util.writeObject(shortestPaths, filename);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            computeShortestPathsFromEachPartitionToAllAndTheirMoveTime();
            ShortestPaths shortestPaths = new ShortestPaths();

            for (Partition partition : mapPartitions) {
                shortestPaths.put(partition.getId(), partition.getPathsToOthers());
            }
            try {
                if (LaunchMRL.shouldPrecompute) {
                    Util.writeObject(shortestPaths, filename);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // MrlPreRoutingPartitionsLayer.PARTITIONS = this;
    }

    private void computeShortestPathsFromEachPartitionToAllAndTheirMoveTime() {


        int numberOfAllPaths = numberOfPartitions * (numberOfPartitions - 1) / 2;

        IPathPlanner pathPlanner;
        if (world.getSelf() instanceof MrlPlatoonAgent) {
            pathPlanner = world.getPlatoonAgent().getPathPlanner();
        } else {
            pathPlanner = world.getCenterAgent().getPathPlanner();
        }

        ArrayList<Partition> partitions = new ArrayList<Partition>();
        partitions.addAll(this);

        List<EntityID> path;
        int moveTime = 1000;

        while (numberOfAllPaths > 0) {

            Partition partition = partitions.get(0);
            for (int j = 1; j < partitions.size(); j++) {
                //todo ==> it should change to use AStar
                //todo ==> we should use new Graph to search in it, a graph based on area edges
                path = new ArrayList<EntityID>();

                path = pathPlanner.planMove(partition.getCentralArea(), partitions.get(j).getCentralArea(), MRLConstants.IN_TARGET, false);
                int pathLength = pathPlanner.getPathCost();
                if (pathLength < 1) {
//                    System.out.println("partition.getCentralArea()=" + partition.getCentralArea() + "  partitions.get(j).getCentralArea()=" + partitions.get(j).getCentralArea() + "   cost=" + pathLength);
                }
//                if (partition.getCentralArea().getID().getValue() == 349 || partition.getCentralArea().getID().getValue() == 251)
//                    System.out.println("");
                if (partition.getId() == 65 && partitions.get(j).getId() == 87) {
                    System.out.print("");
                }
                moveTime = computeMoveTimeOnPath(pathLength);

                // here, it sets a path from partition A to partition B and a moveTime .  ((A,B),(path,moveTime)) and vice versa
                partition.getPathsToOthers().put(new PairSerialized<Integer, Integer>(partition.getId(), partitions.get(j).getId()), new PairSerialized<List<Integer>, Integer>(null, moveTime));
                partitions.get(j).getPathsToOthers().put(new PairSerialized<Integer, Integer>(partitions.get(j).getId(), partition.getId()), new PairSerialized<List<Integer>, Integer>(null, moveTime));

                numberOfAllPaths--;
            }
            if (!partitions.isEmpty())
                partitions.remove(partition);

        }
    }

    private int computeMoveTimeOnPath(int pathLength) {

        if (pathLength == -1)
            return 1000; // means a long Move Time

//        ArrayList<Pair<Integer, Integer>> points = new ArrayList<Pair<Integer, Integer>>();
//
//        points.add(world.getEntity(path.get(0)).getLocation(world));
//        for (int i = 0; i < path.size() - 1; i++) {
//            Area a1 = (Area) world.getEntity(path.get(i));
//            Area a2 = (Area) world.getEntity(path.get(i + 1));
//            for (Edge edge : a1.getEdges()) {
//                if (edge.isPassable() && edge.getNeighbour().equals(a2.getID())) {
//                    points.add(edge.getMiddle());
//                }
//            }
//        }
//        points.add(world.getEntity(path.get(path.size() - 1)).getLocation(world));
//
//        int predictedDistance = 0;
//        for (int j = 0; j < points.size() - 1; j++) {
//            predictedDistance += Util.distance(points.get(j).first(), points.get(j).second(), points.get(j + 1).first(), points.get(j + 1).second());
//        }


        keepLongestDistanceOfTheMap(pathLength);

        //todo ==> compute moveTime for a path   =>>> TEST IS OK    sometimes bigger than real
        if (pathLength < 1)
            System.out.println("zzzzzzzzzz");
        return (int) Math.ceil(pathLength / MRLConstants.MEAN_VELOCITY_OF_MOVING);
    }


    private void keepLongestDistanceOfTheMap(int predictedDistance) {
        if (predictedDistance > world.getLongestDistanceOfTheMap())
            world.setLongestDistanceOfTheMap(predictedDistance);
    }

    private void findCentralAreaOfEachPartitionAndRemoveNullPartitions() {
        int range = (int) Math.ceil(Math.sqrt(Math.pow(eachPartitionHeight / 2, 2) + Math.pow(eachPartitionWidth / 2, 2)));

        ArrayList<Partition> nullPartitions = new ArrayList<Partition>();
        for (Partition p : this) {


            StandardEntity area = Util.nearestToCenterOfPolygon(world, world.getObjectsInRange(p.getCenterPosition().first(), p.getCenterPosition().second(), range), p.getPolygon(), p.getCenterPosition());
            if (area == null) {
                nullPartitions.add(p);
                idPartitionMap.remove(p.getId());
                continue;
            }
            p.setCentralArea((Area) area);

        }

        this.removeAll(nullPartitions);
        numberOfPartitions = this.size();

    }

    private void makePartitions() {

        // find the partition dimensions

        rowNums = 10;   // 10 should change to a dynamic value, for example by considering volume of roads
        columnNums = 10;

        numberOfPartitions = columnNums * rowNums;

        // 1 is added for overlapping between partitions
//        eachPartitionWidth = world.getMapWidth() / columnNums + 1;
//        eachPartitionHeight = world.getMapHeight() / rowNums + 1;
        eachPartitionWidth = (int) (world.getBounds().getWidth() / columnNums + 1);
        eachPartitionHeight = (int) (world.getBounds().getHeight() / rowNums + 1);

//        int tempX = world.getMinX();
//        int tempY = world.getMinY();
        int tempX = (int) world.getBounds().getMinX();
        int tempY = (int) world.getBounds().getMinY();


        for (int i = 0; i < numberOfPartitions; i++) {
            if (i % columnNums == 0 && i != 0) {
//                tempX = world.getMinX();
                tempX = (int) world.getBounds().getMinX();
                tempY += eachPartitionHeight;
            }

            Polygon polygon = new Polygon();
            polygon.addPoint(tempX, tempY);
            polygon.addPoint(tempX + eachPartitionWidth, tempY);
            polygon.addPoint(tempX + eachPartitionWidth, tempY + eachPartitionHeight);
            polygon.addPoint(tempX, tempY + eachPartitionHeight);

            Partition pD = new Partition(i, polygon, world);
            pD.setWidth(eachPartitionWidth);
            pD.setHeight(eachPartitionHeight);

            tempX += eachPartitionWidth;

            mapPartitions.add(pD);
            idPartitionMap.put(pD.getId(), pD);
        }
        this.addAll(mapPartitions);


    }


    public int getNumberOfPartitions() {
        return numberOfPartitions;
    }

    public void setNumberOfPartitions(int numberOfPartitions) {
        this.numberOfPartitions = numberOfPartitions;
    }

    public int getRowNums() {
        return rowNums;
    }

    public void setRowNums(int rowNums) {
        this.rowNums = rowNums;
    }

    public int getColumnNums() {
        return columnNums;
    }

    public void setColumnNums(int columnNums) {
        this.columnNums = columnNums;
    }

    public int getEachPartitionWidth() {
        return eachPartitionWidth;
    }

    public void setEachPartitionWidth(int eachPartitionWidth) {
        this.eachPartitionWidth = eachPartitionWidth;
    }

    public int getEachPartitionHeight() {
        return eachPartitionHeight;
    }

    public void setEachPartitionHeight(int eachPartitionHeight) {
        this.eachPartitionHeight = eachPartitionHeight;
    }

    @Override
    public Partition getMyPartition() {
        return null;
    }

    @Override
    public Partition getPartition(Pair<Integer, Integer> pairPosition) {

        // todo should compute       =>> TEST IS OK
        try {
            int x, y, partitionID;

            x = (pairPosition.first() - (int) world.getBounds().getMinX()) / eachPartitionWidth + 1;
            y = (pairPosition.second() - (int) world.getBounds().getMinY()) / eachPartitionHeight + 1;

            partitionID = (x + (y - 1) * columnNums) - 1;// -1 is because ids starts from zero

            // we didn't use this.get(partitionID) because som of partitions are removed and then the order is not right so
            // we used HashMap instead

            Partition p = idPartitionMap.get(partitionID);
            if (p == null) {
                return findNearestNotNullPartition(partitionID);
            }
            return p;
        } catch (NullPointerException ex) {
//            ex.printStackTrace();
            return null;
        }

    }

    private Partition findNearestNotNullPartition(int partitionID) {
        Partition p1, p2;
        int i = 1;
        while (true) {
            if (partitionID + i < idPartitionMap.size()) {
                p1 = idPartitionMap.get(partitionID + i);
                if (p1 != null)
                    return p1;
            }
            if (partitionID - i > 0) {
                p2 = idPartitionMap.get(partitionID - i);
                if (p2 != null)
                    return p2;
            }
            i++;
        }
    }

    @Override
    public Partition getPartition(Partitionable object) {
        return null;
    }

    @Override
    public Partition getPartition(Path path) {
        return null;
    }

    @Override
    public Partition findPartitionID(int x, int y) {
        return null;
    }

    @Override
    public Map<StandardEntity, Partition> getHumanPartitionMap() {
        return null;
    }
}
