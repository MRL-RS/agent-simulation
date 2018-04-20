package mrl.partitioning;

import javolution.util.FastMap;
import math.geom2d.line.Line2D;
import mrl.common.Util;
import mrl.partitioning.segmentation.*;
import mrl.partitioning.voronoi.Rendezvouz;
import mrl.world.MrlWorld;
import mrl.world.object.MrlBuilding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

import static java.lang.Integer.MAX_VALUE;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 5/16/12
 * Time: 7:59 PM
 */
public class PartitionHelper {
    private static final Log logger = LogFactory.getLog(PartitionHelper.class);

    private MrlWorld world;
    private Segmentation segmentation;
    private static int theSamePointThreshold = 1000;


    public PartitionHelper(MrlWorld world) {
        this.world = world;
    }

    /**
     * Split the provided partition into {@code n} partitions according to {@code segmentType}.
     *
     * @param partition   partition to be split
     * @param n           number result partitions,; -1 means numbers are not important
     * @param segmentType Segmentation approach
     * @return A list of partitions resulted from splitting the provided partition.
     * @throws UnsupportedOperationException if {@code segmentType} is POLYGON.
     */
    public List<Partition> split(Partition partition, int n, SegmentType segmentType) {

        List<Partition> partitions = null;
        switch (segmentType) {
            case ENTITY_CLUSTER:
                segmentation = KMeansClustering.getThreadInstance(world);
                partitions = entityClusterSplitting(partition, n);
                break;
            case TARGET_CLUSTER:
                segmentation = new SimpleDistanceBasedClustering(world);
                partitions = entityClusterSplitting(partition, n);
                break;

            case REPETITIVE_ENTITY_CLUSTER:
                segmentation = KMeansClustering.getThreadInstance(world);
                partitions = repetitiveEntityClusterSplitting(partition, n);
                break;
            case POLYGON:
//                TODO @Pooya The following line should be uncommented and the mentioned class should be implemented in order to support POLYGON
//                segmentation=new VoronoiSegmentation();
//                partitions = polygonSplitting(partition, n);
//                break;
                throw new UnsupportedOperationException("Not implemented yet.");
        }
        return partitions;
    }


    private List<Partition> entityClusterSplitting(Partition partition, int n) {
        List<Partition> partitions;

        List<StandardEntity> entities = new ArrayList<StandardEntity>();
        entities.addAll(partition.getBuildingEntities());
        entities.addAll(partition.getRoads());
        EntityCluster entityCluster = new EntityCluster(world, entities);
        SegmentationResult segmentationResult = segmentation.split(entityCluster, n);

        partitions = toPartitions(segmentationResult, partition.getEntityPositionMap());
        return partitions;
    }

    /**
     * This method  splits specified partition to some smaller partitions considering repetitive entities in the main partition
     * for splitting.
     *
     * @param partition The partition to split
     * @param n         number of needed slices
     * @return list of slices as list of partitions
     */
    private List<Partition> repetitiveEntityClusterSplitting(Partition partition, int n) {
        List<Partition> partitions;

        List<StandardEntity> entities = new ArrayList<StandardEntity>();
        for (MrlBuilding building : partition.getBuildings()) {
            entities.add(building.getSelfBuilding());
        }
        for (Road road : partition.getRoads()) {
            entities.add(road);
        }

        EntityCluster entityCluster = new EntityCluster(world, entities);
        SegmentationResult segmentationResult = segmentation.split(entityCluster, n);

        partitions = toPartitions(segmentationResult, partition.getEntityPositionMap());
        return partitions;
    }

    private List<Partition> polygonSplitting(Partition partition, int n) {
        throw new NotImplementedException();
    }


    private List<Partition> toPartitions(SegmentationResult segmentationResult, Map<StandardEntity, List<StandardEntity>> entityPositionMap) {
        List<Partition> partitions = new ArrayList<Partition>();
        if (segmentationResult.isEntityClustersDefined()) {
            for (EntityCluster entityCluster : segmentationResult.getEntityClusters()) {
                partitions.add(new Partition(world, entityCluster, entityPositionMap));
            }
        } else if (segmentationResult.isPolygonsDefined()) {
            for (Polygon polygon : segmentationResult.getPolygons()) {
                partitions.add(new Partition(world, polygon));
            }
        }

        return partitions;
    }

    public Partition makeWorldPartition() {
        return new Partition(world, world.getWorldPolygon());
    }

    public void setNeighbours(List<Partition> partitionList) {

        List<Pair<EntityID, EntityID>> ignoreList = new ArrayList<Pair<EntityID, EntityID>>();

        if (partitionList.size() <= 1) {
            return;
        }

        //clear previous neighbours
        for (Partition partition : partitionList) {
            partition.getNeighboursByEdge().clear();
        }

        for (int i = 0; i < partitionList.size(); i++) {
            for (int j = 0; j < partitionList.size(); j++) {

//                if (i >= partitionList.size() || j >= partitionList.size()) {
//                    continue;
//                }
                if (partitionList.get(i).getId().equals(partitionList.get(j).getId())) {
                    continue;
                }
                if (isNeighbourhoodComputed(ignoreList, partitionList.get(i).getId(), partitionList.get(j).getId())) {
                    continue;
                }

                List<Line2D> commonLines = checkIsNeighbour(partitionList.get(i), partitionList.get(j));
                if (!commonLines.isEmpty()) {
                    for (Line2D commonLine : commonLines) {
                        partitionList.get(i).addNeighbours(commonLine, partitionList.get(j));
                        partitionList.get(j).addNeighbours(commonLine, partitionList.get(i));
                        ignoreList.add(new Pair<EntityID, EntityID>(partitionList.get(i).getId(), partitionList.get(j).getId()));
                    }
                }
            }
        }


        setNeighbourForOrphanPartitions(partitionList);

    }

    private boolean isNeighbourhoodComputed(List<Pair<EntityID, EntityID>> ignoreList, EntityID firstID, EntityID secondID) {
        for (Pair<EntityID, EntityID> pair : ignoreList) {
            if (pair.first().equals(firstID) && pair.second().equals(secondID) || pair.first().equals(secondID) && pair.second().equals(firstID)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if two partitions have common line so they are neighbour otherwise not
     *
     * @param p1 fist partition
     * @param p2 second partition
     * @return common line of to partitions
     */
    private List<Line2D> checkIsNeighbour(Partition p1, Partition p2) {

        List<Line2D> commonLines;
        commonLines = findCommonLines(p1, p2);

        return commonLines;
    }

    private List<Line2D> findCommonLines(Partition p1, Partition p2) {

        List<Line2D> commonLines = new ArrayList<Line2D>();
        List<math.geom2d.Point2D> commonPoints = new ArrayList<math.geom2d.Point2D>();

        // p1 lines and p2 points to check for intersection
        for (Line2D line : p1.getBoundaryLines()) {
            for (int i = 0; i < p2.getPolygon().npoints; i++) {
                if (line.getDistance((double) p2.getPolygon().xpoints[i], (double) p2.getPolygon().ypoints[i]) < theSamePointThreshold) {
                    commonPoints.add(new math.geom2d.Point2D(p2.getPolygon().xpoints[i], p2.getPolygon().ypoints[i]));
                }
            }
        }

        // p2 lines and p1 points to check for intersection
        for (Line2D line : p2.getBoundaryLines()) {
            for (int i = 0; i < p1.getPolygon().npoints; i++) {
                if (line.getDistance((double) p1.getPolygon().xpoints[i], (double) p1.getPolygon().ypoints[i]) < theSamePointThreshold) {
                    commonPoints.add(new math.geom2d.Point2D(p1.getPolygon().xpoints[i], p1.getPolygon().ypoints[i]));
                }
            }
        }


        //remove the same points
        List<Point2D> toKeepPoints = new ArrayList<Point2D>();
        for (math.geom2d.Point2D commonPoint : commonPoints) {
            if (shouldAdd(toKeepPoints, commonPoint)) {
                toKeepPoints.add(commonPoint);
            }
        }

        commonPoints.clear();
        for (Point2D toKeepPoint : toKeepPoints) {
            commonPoints.add((math.geom2d.Point2D) toKeepPoint);
        }


        if (commonPoints.size() == 2) {
            commonLines.add(new Line2D(commonPoints.get(0), commonPoints.get(1)));
        } else if (commonPoints.size() > 2) {
            Pair<math.geom2d.Point2D, math.geom2d.Point2D> pair = getPointsWithMostDistances(commonPoints);
            commonLines.add(new Line2D(pair.first(), pair.second()));
        } else {
            // commonPoints is empty or has just one point, so there is no common line
        }

        return commonLines;


    }


    private Pair<math.geom2d.Point2D, math.geom2d.Point2D> getPointsWithMostDistances(List<math.geom2d.Point2D> commonPoints) {

        math.geom2d.Point2D p1 = null;
        math.geom2d.Point2D p2 = null;
        double longestDistance = Double.MIN_VALUE;
        double tempDistance;
        for (int i = 0; i < commonPoints.size(); i++) {
            for (int j = 0; j < commonPoints.size(); j++) {
                if (i != j) {
                    tempDistance = Util.distance(commonPoints.get(i).getX(), commonPoints.get(i).getY(), commonPoints.get(j).getX(), commonPoints.get(j).getY());
                    if (tempDistance > longestDistance) {
                        longestDistance = tempDistance;
                        p1 = commonPoints.get(i);
                        p2 = commonPoints.get(j);
                    }
                }
            }
        }

        return new Pair<math.geom2d.Point2D, math.geom2d.Point2D>(p1, p2);
    }


    private boolean shouldAdd(List<Point2D> points, math.geom2d.Point2D point) {

        for (Point2D p : points) {
            if (p.getX() == point.getX() && p.getY() == point.getY()) {
                return false;
            }
        }
        return true;

    }


    //if any partition has no neighbours, nearest line to this partition added as its neighbour

    private void setNeighbourForOrphanPartitions(List<Partition> partitions) {
        for (Partition partition : partitions) {
            if (partition.getNeighboursByEdge().size() == 0) {
//                System.out.println(" DefaultSegmentationHelper---Line 159-235");
                System.err.println("***time:" + world.getTime() + " partition:" + partition.getId() + " has no neighbour");
                partition.getNeighboursByEdge().add(getNeighbourPartition(partition, partitions));
                if (partition.getNeighboursByEdge() == null || partition.getNeighboursByEdge().isEmpty()) {
                    System.err.println("***time:" + world.getTime() + " partition:" + partition.getId() + " Again not found");
                } else {
                    System.err.println("***time:" + world.getTime() + " partition:" + partition.getId() + " Found one Neighbour neighbours" + partition.getNeighboursByEdge().size());
                }
            }
        }
    }

    private Pair<Line2D, Partition> getNeighbourPartition(Partition partition, List<Partition> partitions) {
        Line2D tempLine = null;
        int tempDist = MAX_VALUE;
        Partition tempPartition = null;
        Pair<Line2D, Partition> nearest;
        for (Partition p : partitions) {
            if (p.getId().equals(partition.getId())) {
                continue;
            }
            if (tempLine != null) {
                if (tempDist > getNearestNeighbour(p, partition).second()) {
                    tempLine = getNearestNeighbour(p, partition).first();
                    tempDist = getNearestNeighbour(p, partition).second();
                    tempPartition = p;
                }
            } else {
                tempDist = getNearestNeighbour(p, partition).second();
                tempLine = getNearestNeighbour(p, partition).first();
                tempPartition = p;
            }
        }
//        System.out.println("time:" + world.getTime() + " partition:" + partition.getId()+"  neighbour:"+tempPartition.getId());
        nearest = new Pair<Line2D, Partition>(tempLine, tempPartition);
        return nearest;
    }

    private Pair<Line2D, Integer> getNearestNeighbour(Partition p, Partition partition) {
        Line2D tempLine = null;
        int tempDist = MAX_VALUE;
        Pair<Line2D, Integer> nearestLine;
        for (Line2D line : partition.getBoundaryLines()) {
            if (nearestLine(p, line).second() < tempDist) {
                tempDist = nearestLine(p, line).second();
                tempLine = nearestLine(p, line).first();
            }
        }
        nearestLine = new Pair<Line2D, Integer>(tempLine, tempDist);
        return nearestLine;
    }

    private Pair<Line2D, Integer> nearestLine(Partition p, Line2D line) {
        Line2D tempLine = null;
        int tempDist = MAX_VALUE;
        Pair<Line2D, Integer> nearestLine;
        for (Line2D line2D : p.getBoundaryLines()) {
            if (linesDistance(line, line2D) < tempDist) {
                tempDist = linesDistance(line, line2D);
                tempLine = line2D;
            }
        }
        nearestLine = new Pair<Line2D, Integer>(tempLine, tempDist);
        return nearestLine;
    }

    private int linesDistance(Line2D line, Line2D line2D) {
        double n1p1 = Util.distance(line.getX1(), line.getY1(), line2D.getX1(), line2D.getY1());
        double n1p2 = Util.distance(line.getX1(), line.getY1(), line2D.getX2(), line2D.getY2());
        double n2p1 = Util.distance(line.getX2(), line.getY2(), line2D.getX1(), line2D.getY1());
        double n2p2 = Util.distance(line.getX2(), line.getY2(), line2D.getX2(), line2D.getY2());
        if (n1p1 + n2p2 < n1p2 + n2p1) {
            return (int) (n1p1 + n2p2);
        }
        return (int) (n1p2 + n2p1);
    }

    public List<Partition> mergeTooSmallPartitions(List<Partition> partitions) {
        Partition tooSmallPartition;
        while (true) {
            tooSmallPartition = getTooSmallPartition(partitions);
            if (tooSmallPartition == null) {
                break;
            }
            partitions = mergePartitions(partitions, tooSmallPartition);
        }
        return partitions;
    }

    /**
     * find smallest partition with size smaller than 2/3 average of all partition sizes
     *
     * @param partitions partitions to check
     * @return smmalest partition
     */
    private Partition getTooSmallPartition(List<Partition> partitions) {
//        double meanOfValues;
//        for (Partition partition : partitions) {
//            sumOfValues += partition.getSize();
//        }
//        meanOfValues = sumOfValues / partitions.size();
        Collections.sort(partitions, Partition.PARTITION_SIZE_COMPARATOR_ASCENDING);
        for (Partition partition : partitions) {
            if (partition.getNumberOfNeededPFs() == 0 /*|| partition.getSize() < 0.9f * eachPolicePerBuildings*/) {
                return partition;
            }
        }
        return null;

    }


    /**
     * merges specified partition to one of its neighbours (the one which has biggest common line)
     *
     * @param partitions list of partitions before merging
     * @param partition  partition to merge to others
     * @return list of new Partitions after merging
     */
    private List<Partition> mergePartitions(List<Partition> partitions, Partition partition) {

        //find partition with biggest common Line
//        double maxDistance = Integer.MIN_VALUE;
        double maxDistance = Integer.MIN_VALUE;
        double tempLength;
        Partition bestPartition = null;
        Map<EntityID, Double> partitionNeighborhood = new FastMap<EntityID, Double>();
//        for (Pair<Line2D, Partition> neighbour : partition.getNeighboursByEdge()) {
//            tempLength = 0;
//            if (partitionNeighborhood.keySet().contains(neighbour.second().getId())) {
//                tempLength = partitionNeighborhood.get(neighbour.second().getId());
//            }
//            tempLength += neighbour.first().getLength();
//            partitionNeighborhood.put(neighbour.second().getId(), tempLength);
//
//            if (tempLength > maxDistance) {
//                maxDistance = tempLength;
//                bestPartition = neighbour.second();
//            }
//        }
//        for (Pair<Line2D, Partition> neighbour : partition.getNeighboursByEdge()) {
//            tempLength = neighbour.second().getSize();
//            if (tempLength < minValue) {
//                minValue = tempLength;
//                bestPartition = neighbour.second();
//            }
//        }

        if (bestPartition == null) {
            System.out.println("bestPartition ==null");
        }
        bestPartition.eat(partition);
        partitions.remove(partition);
//        registeredPartitionIDs.remove(partition.getId());
//        setNeighboursByEdge(partitions); //todo remove it and correct neighbour setting of partition eating
//        makeIDs(partitions);
        return partitions;
    }


    public List<Partition> mergingOperation(int numberOfPolices, List<Partition> partitions, boolean sizeBased) {

        while (/*isThereTooSmallPartition(partitions) ||*/ partitions.size() > numberOfPolices) {
            if (sizeBased) {
                partitions = mergeSmallestPartition_SizeBased(partitions);
            } else {
                partitions = mergeSmallestPartition_ValueBased(partitions);
            }
        }

        return partitions;
    }

    private List<Partition> mergeSmallestPartition_SizeBased(List<Partition> partitions) {
        Partition smallestPartition = partitions.get(0);
        double minSize = Double.MAX_VALUE;
        for (Partition partition : partitions) {
            if (partition.getSize() < minSize) {
                minSize = partition.getSize();
                smallestPartition = partition;
            }
        }
        mergePartitions(partitions, smallestPartition);
        return partitions;
    }

    private List<Partition> mergeSmallestPartition_ValueBased(List<Partition> partitions) {
        Partition smallestPartition = partitions.get(0);
        double minValue = Double.MAX_VALUE;
        for (Partition partition : partitions) {
            if (partition.getValue() < minValue) {
                minValue = partition.getValue();
                smallestPartition = partition;
            }
        }
        mergePartitions(partitions, smallestPartition);
        return partitions;
    }


    public void createRendezvous(List<Partition> partitions, MrlWorld world) {

        if (partitions.size() <= 1) {
            return;
        }
        Rendezvouz rendezvouz = new Rendezvouz(world);
        rendezvouz.createRendezvous(partitions);

    }

    public Set<EntityID> findNeighbours(Partition targetPartition, List<Partition> partitions, int neighbourhoodThreshold) {

        Set<EntityID> neighbourSet = new HashSet<>();
        for (Partition partition : partitions) {
            if (!partition.getId().equals(targetPartition.getId()) && areNeighbour(targetPartition, partition, neighbourhoodThreshold)) {
                neighbourSet.add(partition.getId());
            }
        }
        return neighbourSet;
//        return partitions.stream().filter(partition ->
//                !partition.getId().equals(targetPartition.getId())
//                        && areNeighbour(targetPartition, partition, neighbourhoodThreshold)).
//                map(Partition::getId).collect(Collectors.toSet());
    }


    /**
     * This method uses partition polygons to check if they have conjunctions or they are closer than a threshold to
     * be assumed as neighbour
     *
     * @param firstPartition
     * @param secondPartition
     * @return True if they are neighbour
     */
    private boolean areNeighbour(Partition firstPartition, Partition secondPartition, int neighbourhoodThreshold) {
        return (Util.distance(firstPartition.getPolygon(), secondPartition.getPolygon()) < neighbourhoodThreshold);
    }

    public List<EntityID> findRoute(Partition firstPartition, Partition secondPartition) {
        return world.getPlatoonAgent().getPathPlanner().planMove((Area) firstPartition.getCenterEntity(), (Area) secondPartition.getCenterEntity(), 0, true);
    }
}