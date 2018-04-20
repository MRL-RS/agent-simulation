package mrl.police;

import javolution.util.FastSet;
import mrl.world.MrlWorld;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Blockade;

import java.util.List;
import java.util.Set;

/**
 * @author : Pooya Deldar Gohardani
 *         Date: 4/6/12
 *         Time: 1:56 AM
 */
public class BlockadeSelectionHelper {


    private MrlWorld world;
    private int selfX;
    private int selfY;
    private int maxClearDistance;

    public BlockadeSelectionHelper(MrlWorld world, int maxClearDistance) {
        this.world = world;
        selfX = world.getSelfLocation().first();
        selfY = world.getSelfLocation().second();
        this.maxClearDistance = maxClearDistance;
    }

    public Pair<Blockade, Integer> selectBlockade(Set<Blockade> blockades) {

        return getNearestBlockade(blockades);
//        return getBlockadeRandomly(blockades);

    }

    private Pair<Blockade, Integer> getNearestBlockade(Set<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return null;
        }


        int minDistance = Integer.MAX_VALUE;
        Pair<Blockade, Integer> nearestBlockadePair = new Pair<Blockade, Integer>(blockades.iterator().next(), 0);
        int tempDistance;
        for (Blockade blockade : blockades) {
            tempDistance = findDistanceTo(blockade, selfX, selfY);
            if (tempDistance < minDistance) {
                minDistance = tempDistance;
                nearestBlockadePair = new Pair<Blockade, Integer>(blockade, minDistance);
            }
        }

        return nearestBlockadePair;
    }

    private Pair<Blockade, Integer> getBlockadeRandomly(Set<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return null;
        }
        Set<Blockade> blockadeSet = getInRangeBlockades(blockades);

        if (blockadeSet == null || blockadeSet.isEmpty()) {
            return getNearestBlockade(blockades);
        }


        int blockadeIndex = world.getPlatoonAgent().getRandom().nextInt(blockades.size());
        int i = 0;
        for (Blockade blockade : blockades) {
            if (i == blockadeIndex) {
                return new Pair<Blockade, Integer>(blockade, findDistanceTo(blockade, selfX, selfY));
            }
            i++;

        }
        return null;
    }


    private int findDistanceTo(Blockade b, int x, int y) {
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            if (d < best) {
                best = d;
            }

        }
        return (int) best;
    }


    private Set<Blockade> getInRangeBlockades(Set<Blockade> blockades) {

        if (blockades == null || blockades.isEmpty()) {
            return blockades;
        }

        Set<Blockade> inRange = new FastSet<Blockade>();
        for (Blockade blockade : blockades) {
            if (findDistanceTo(blockade, selfX, selfY) <= maxClearDistance) {
                inRange.add(blockade);
            }
        }

        return inRange;
    }
}
