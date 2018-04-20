package mrl.world.routing.grid;

import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.police.MrlPoliceForce;
import mrl.world.MrlWorld;
import mrl.world.routing.pathPlanner.CheckAreaPassable;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;

/**
 * Created by Mostafa  Shabani.
 * Date: Oct 31, 2010
 * Time: 11:33:20 AM
 */
public class UpdateGridPassable implements MRLConstants {

    private MrlWorld world;
    private int gridSize = MIN_GRID_SIZE;

//    private CheckAreaPassable areaPassable;

    public UpdateGridPassable(CheckAreaPassable checkAreaPassable) {
        this.world = checkAreaPassable.getWorld();
        if (world.getSelf() instanceof MrlPoliceForce) {
            gridSize = 900;
        } else {
            gridSize = 600;
        }
//        this.areaPassable = checkAreaPassable;
    }

    public void update(Area area, AreaGrids areaGrids, boolean flag) {
        /**
         * hazf kardane grid haei ke ba blockade ha intersect darand.
         * be in sorat ke az blockade ye polygon dorost mikonim.
         * sepas position-e grid ha ke hamoon markazeshon mishe ro ...
         * + or - yek meghdari mikonim ta be hashie haye grid ha nazdiktar bashim.
         * hala age in point ha bioftant too blockadePolygon ke impassable mishan.
         *
         * flag vase ine ke befahmim aya bayad az grid haei ke hata meghdare kami block daran ham sarfe nazar konim ya na
         */

        if (!area.isBlockadesDefined()) {
            return;
        }

        if (area.getBlockades().isEmpty()) {
            // age area Blockade nadash
            for (Grid grid : areaGrids.getGrids()) {
                grid.setPassable(true, null);
            }
        } else {

            for (Grid grid : areaGrids.getGrids()) {
                // avval hameye grid haro passable mikonim ta age ba blockade intersect dasht impassable beshe.
                grid.setPassable(true, null);
                grid.resetIntersectCounter();
            }

            Blockade blockade;
            int[] allApexes;
            int counter;
            Polygon blockadePoly;
            int[] xs;
            int[] ys;
            double gridSize = -1;
            double extra = (int) Math.round(gridSize / 2.3);
            boolean hasIntersect;
            int x;
            int y;

            for (EntityID blockadeId : area.getBlockades()) {
                // vase har blockadi ye polygon dorost mikonim.
                blockade = (Blockade) world.getEntity(blockadeId);

                allApexes = blockade.getApexes();
                if (allApexes != null) {
                    counter = allApexes.length / 2;

                    blockadePoly = new Polygon();
                    xs = new int[counter];
                    ys = new int[counter];

                    for (int i = 0; i < counter; ++i) {
                        xs[i] = allApexes[i * 2];
                        ys[i] = allApexes[(i * 2) + 1];
                    }

                    for (int i = 0; i < counter; ++i) {
                        // dare polygon-e blockade dorost mishe.
                        blockadePoly.addPoint(allApexes[i * 2], allApexes[i * 2 + 1]);
                    }

                    for (Grid grid : areaGrids.getGrids()) {
                        // hala done done grid ha check mishan.
                        hasIntersect = false;
                        // ------------------------------- test 1
                        if (gridSize == -1) {
                            gridSize = Util.min(Util.distance(grid.getVertices().get(0), grid.getVertices().get(1)),
                                    Util.distance(grid.getVertices().get(1), grid.getVertices().get(2)));
                            extra = (int) Math.round(gridSize / 2.1);
                        }

                        if (blockadePoly.contains(grid.getPosition().first(), grid.getPosition().second())) {
                            hasIntersect = true;
                        }

                        if (!hasIntersect) {
                            x = grid.getPosition().first();
                            y = grid.getPosition().second();


                            if (blockadePoly.contains((x + extra), (y))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x + extra), (y - extra))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x), (y - extra))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x - extra), (y - extra))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x - extra), (y))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x - extra), (y + extra))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x), (y + extra))) {
                                grid.increaseIntersectCounter();
                            }
                            if (blockadePoly.contains((x + extra), (y + extra))) {
                                grid.increaseIntersectCounter();
                            }

                            if ((grid.getIntersectCounter() > 1 && gridSize < (gridSize * 1.5)) || grid.getIntersectCounter() > 4) {
                                hasIntersect = true;
                            }
                        }

                        //--------------------------------------------- raveshe kolli va kamel
//                        int[] xs = new int[counter];
//                        int[] ys = new int[counter];
//
//                        for (int i = 0; i < counter; ++i) {
//                            xs[i] = allApexes[i * 2];
//                            ys[i] = allApexes[(i * 2) + 1];
//                        }
                        // check kardane markaze grid.
//                        if (blockadePoly.contains(grid.getPosition().first(), grid.getPosition().second())) {
//                            hasIntersect = true;
//                        }
                        // check kardane tamame apex haye blockade.
//                            for (int i = 0; i < (counter - 1); ++i) {
//                                if (grid.getPolygon().getBounds2D().intersectsLine(xs[i], ys[i], xs[i + 1], ys[i + 1])) {
//                                    hasIntersect = true;
//                                    break;
//                                }
//                            }
//                            if (grid.getPolygon().getBounds2D().intersectsLine(xs[0], ys[0], xs[counter - 1], ys[counter - 1])) {
//                                hasIntersect = true;
//                            }

                        if (flag && !hasIntersect && grid.isOnPassableEdge()) {

                            for (Pair<Integer, Integer> pair : grid.getVertices()) {

                                if (blockadePoly.contains(pair.first(), pair.second())) {
                                    grid.increaseIntersectCounter();
                                }
                            }

                            if ((grid.getIntersectCounter() > 0 && gridSize < (gridSize * 1.3)) || grid.getIntersectCounter() > 2) {
                                hasIntersect = true;
                            }
                        }

                        if (hasIntersect) {
                            grid.setPassable(false, blockadeId);
                        }
                    }
                }
            }
        }
    }

    public int getGridSize() {
        return gridSize;
    }
}
