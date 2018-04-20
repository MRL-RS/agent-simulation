package mrl.world.routing.pathPlanner.move;

import mrl.common.Util;
import mrl.firebrigade.tools.LineInfo;
import mrl.firebrigade.tools.Ray;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;

import java.util.*;

/**
 * @author Mahdi
 */
public class LineOfSightForMovePerception {

    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(LineOfSightForMovePerception.class);

    private int rayCount;
    private int rayLength;
    private int errorThreshold;

    private MrlWorld world;


    /**
     * Create a LineOfSightPerception object.
     */
    public LineOfSightForMovePerception(MrlWorld world) {
        this.world = world;
        this.rayCount = 36;
        this.rayLength = world.getViewDistance();
        this.errorThreshold =  500;//(MRLConstants.AGENT_SIZE/2);
    }

    //obstacles => blockade & Road & buildings
    public Map<Line2D, List<Point2D>> findEscapePoints(java.awt.Polygon scaledConvexHull, List<StandardEntity> obstacles, Point2D targetPoint) {
        Pair<Integer, Integer> location = world.getSelfLocation();
        Map<Line2D, List<Point2D>> targetRaysHitPoint = new HashMap<>();
        if (location != null) {
            Point2D point = new Point2D(location.first(), location.second());
            Set<Ray> freeRays = findRaysNotHit(point, obstacles);
            targetRaysHitPoint = raysFromTargetPoint(targetPoint, freeRays, obstacles);
        }

        return targetRaysHitPoint;
    }


    private Set<Ray> findRaysNotHit(Point2D location, Collection<StandardEntity> obstacles) {
        Set<LineInfo> lines = getAllLines(obstacles);
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Set<Ray> result = new HashSet<>();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(rayLength);
            Point2D distanceLocation = new Point2D(
                    location.getX() + errorThreshold*Math.sin(angle),
                    location.getY() + errorThreshold*Math.cos(angle)
            );
            Ray ray = new Ray(new Line2D(distanceLocation, vector), lines);
            if (ray.getLinesHit().isEmpty()) {
                result.add(ray);
            }
        }
        return result;
    }

    private Map<Line2D, List<Point2D>> raysFromTargetPoint(Point2D location, Collection<Ray> freeRays, Collection<StandardEntity> obstacles) {
        Set<LineInfo> lines = new HashSet<>(getAllLines(obstacles));
        for (Ray ray : freeRays) {
            lines.add(new LineInfo(ray.getRay(), null, false));
        }
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Map<Line2D, List<Point2D>> targetRaysHitPoint = new HashMap<>();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(rayLength);
            Ray ray = new Ray(new Line2D(new Point2D((int) location.getX(), (int) location.getY()), vector), lines);

            List<LineInfo> linesHit = ray.getLinesHit();
            if (!linesHit.isEmpty()) {
                List<Point2D> intersections = new ArrayList<>();
                for (LineInfo lineInfo : linesHit) {
                    if (lineInfo.isBlocking()) {
                        continue;
                    }
                    Point2D intersection = Util.getIntersection(lineInfo.getLine(), ray.getRay());
                    if (Util.contains(lineInfo.getLine(), intersection, 10)) {
                        intersections.add(intersection);
                    }
                }
                targetRaysHitPoint.put(ray.getRay(), intersections);
            }
        }
        return targetRaysHitPoint;
    }


    private Set<LineInfo> getAllLines(Collection<StandardEntity> entities) {
        Set<LineInfo> result = new HashSet<LineInfo>();
        for (StandardEntity next : entities) {
            if (next instanceof Building) {
                for (Edge edge : ((Building) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, !edge.isPassable()));
                }
            } else if (next instanceof Road) {
                for (Edge edge : ((Road) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, !edge.isPassable()));
                }
            } else if (next instanceof Blockade) {
                int[] apexes = ((Blockade) next).getApexes();
                List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
                List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
                for (Line2D line : lines) {
                    result.add(new LineInfo(line, next, true));
                }
            } else {
                continue;
            }
        }
        return result;
    }

    public void setRayCount(int rayCount) {
        this.rayCount = rayCount;
    }


}
