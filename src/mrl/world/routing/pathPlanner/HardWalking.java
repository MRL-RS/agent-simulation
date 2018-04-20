package mrl.world.routing.pathPlanner;

import mrl.common.CommandException;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.EdgeHelper;
import mrl.helper.HumanHelper;
import mrl.world.MrlWorld;
import mrl.world.routing.graph.Node;
import mrl.world.routing.grid.AreaGrids;
import mrl.world.routing.grid.Grid;
import mrl.world.routing.grid.UpdateGridPassable;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa  Shabani.
 * Date: Oct 21, 2010
 * Time: 12:57:32 PM
 */
public class HardWalking implements MRLConstants {

    private MrlWorld world;
    private PathPlanner pathPlanner;
    private CheckAreaPassable areaPassable;
    private UpdateGridPassable updateGridPassable;

    private List<Pair<Integer, Integer>> hardWalkingPairs = new ArrayList<Pair<Integer, Integer>>();
    private boolean continueHardWalking = false;
    //    private int stuckTime = 0;
    private EntityID lastPositionId = null;
    private Pair<Integer, Integer> lastAgentXY = null;

    public HardWalking(PathPlanner pathPlanner) {
        this.world = pathPlanner.getWorld();
        this.pathPlanner = pathPlanner;
        this.areaPassable = pathPlanner.getAreaPassably();
        this.updateGridPassable = new UpdateGridPassable(pathPlanner.getAreaPassably());
    }

    public boolean isContinueHardWalking() {
        return continueHardWalking;
    }

    public boolean isStuck(Area selfPosition, Human self) {
        // check kardane inke aya stuck shode ya ne.
        if (((selfPosition.getID().equals(lastPositionId)) && (pathPlanner.lastMoveTime + 1 == world.getTime()) && isHardWalkingCondition(self))
                || isContinueHardWalking()) {
            return true;
        } else {
            lastPositionId = selfPosition.getID();
            return false;
        }
    }

    public boolean isHardWalkingCondition(Human self) {
        /**
         * in vase ine ke age oboor az ye area 2 cycle tool keshid eshtebahi isStuck tashkhis nade.
         */
        if (HumanHelper.isBuried(self) || world.getPlatoonAgent().isStuck()) {
            // age agent buried bood ke dige hichi.
            return false;
        }
        Pair<Integer, Integer> agentXY = self.getLocation(world);

        if (lastAgentXY == null) {
            lastAgentXY = agentXY;
        } else {
            //agar faseleye harekate agent dar 1 cycle kamtar az size yek grid bashe is stuck mishavad.
            int dist = Util.distance(lastAgentXY, agentXY);
            if ((dist < (MIN_GRID_SIZE * 3))) {
                return true;
            } else {
                lastAgentXY = agentXY;
            }
            /*
            // age bishtar az size grid haket karde bood vali agar 1/2 max move in cycle harekat nakarde bood stuckTime ++ mishe
            // va age in halat bish az 2 cycle tool bekeshe is stuck mishavad.
            if (!isContinueHardWalking() && (dist < MEAN_VELOCITY_OF_MOVING / 2)) {
                stuckTime++;
            } else if (stuckTime > 1) {
                stuckTime = 0;
                return true;
            } else {
                stuckTime = 0;
            }     */
        }
        return false;
    }

    public void hardWalking(Area selfPosition, List<EntityID> path) throws CommandException {
        /**
         * in method vase zamanie ke yek agent besoorate mamooli natoone az yek area oboor kone.
         * aval vase oon area grid dorostkarde va az haman mahali ke hast yek grid path mizanim ta az aan area oboor konad.
         * albate agar aan area ghabele oboor bashe.
         * age ghabele oboor nabashe node marboot be in area v area-e ba'di impassable mishavad.
         */
        if (!hardWalkingPairs.isEmpty() && continueHardWalking) {
            // edame dadan be hard walking

            if (Util.distance(lastAgentXY, world.getSelfLocation()) < 10) {
                // age grid listi ke bedast umade be har dalili ghabele obur nabud be position-e ghabli bar migarde.
                hardWalkingPairs.clear();

                EntityID previousPosition = pathPlanner.getPreviousPosition();
                if (previousPosition != null) {
                    Area preArea = (Area) world.getEntity(previousPosition);
                    pathPlanner.move(preArea, IN_TARGET, true);
                }
            } else {
                hardWalkingMove(selfPosition);
            }
        } else if (continueHardWalking) {
            // payane hard walking
            continueHardWalking = false;

        } else {
            // peida kardane hard walk points v shoroo be harekat.

            EntityID targetId = getNextArea(path, selfPosition.getID());

            if (targetId == null) {
                targetId = pathPlanner.getPreviousPosition();
            }
            if (targetId == null) {
                return;
            }

            Area target = (Area) world.getEntity(targetId);

//            world.printData("----------------------------------------------");
            continueHardWalking = true;
            hardWalkingPairs = getHardWalkingPairs(selfPosition, target);
//            System.out.println(world.getSelf() + " pos:" + selfPosition + "  first hardWalkingPairs : " + hardWalkingPairs);

            if (hardWalkingPairs.size() > 0) {
                /*
                // ezafe kardane hard walk path-e area ba'di.
                // albate age area ba;di blockade dashte bashe ya inke entrance bashe.

                EntityID secondNextAreaId = getNextArea(path, targetId);
                if (target.isBlockadesDefined() && target.getBlockades().size() > 0 && secondNextAreaId != null) {

                    List<Pair<Integer, Integer>> nextAreaPath;

                    nextAreaPath = areaPassable.getHardWalkPath(selfPosition.getID(), secondNextAreaId);

                    if (nextAreaPath == null) {
                        // age in area ghablan check nashode bood alan check mishe.
                        areaPassable.checkAreaPassable(target);
                        nextAreaPath = areaPassable.getHardWalkPath(selfPosition.getID(), secondNextAreaId);
                    }

                    if (nextAreaPath != null) {
                        // hard walk path-e in area be ghabli ezafe mishe.
                        hardWalkingPairs.addAll(nextAreaPath);
                    } else {
                        hardWalkingPairs.clear();
                    }
//                    System.out.println(world.getSelf() + "  next Area:" + targetId.getValue() + "  with block  path : " + nextAreaPath+" hardWalkingPairs:"+hardWalkingPairs);

                } else {*/
                // age area ba'di blockade nadasht mire be nazdiktarin gride oon area.
                AreaGrids nextAreaGrids = areaPassable.getAreaGrids(targetId);
                int distanceToEdgeCenter;
                int minDistance = Integer.MAX_VALUE;
                Grid selectedGrid = null;
                Pair<Integer, Integer> xYPair = hardWalkingPairs.get(hardWalkingPairs.size() - 1);

                for (Grid grid : nextAreaGrids.getGrids()) {

                    if (grid.isPassable()) {
                        distanceToEdgeCenter = Util.distance(grid.getPosition(), xYPair);

                        if (distanceToEdgeCenter < minDistance) {
                            selectedGrid = grid;
                            minDistance = distanceToEdgeCenter;
                        }
                    }
                }
                if (selectedGrid != null) {
                    hardWalkingPairs.add(selectedGrid.getPosition());
                }
//                    System.out.println(world.getSelf() + "  next Area:" + targetId.getValue() + " without block  one Grid : " + selectedGrid.getId()+" hardWalkingPairs:"+hardWalkingPairs);

                //}

                // ezafe kardane yek point vase obur az entrance.
                if (hardWalkingPairs.size() > 0) {
                    addAfterEntrancePoint(target);
                }
            }

            if (hardWalkingPairs.size() == 0) {
                // age natoonest path peida kone node marboote ro impassable mikone.
                // va be area-e ghabli-e khodesh barmigarde.
                EntityID previousPosition = pathPlanner.getPreviousPosition();
                if (previousPosition != null) {
                    Node preNode = pathPlanner.getGraph().getNodeBetweenAreas(selfPosition.getID(), previousPosition, null);
                    if (preNode != null) {
                        areaPassable.setPassablyCommonMyEdge(selfPosition.getID(), pathPlanner.getGraph().getNodeBetweenAreas(selfPosition.getID(), targetId, null), preNode, false);
//                        hardWalkingPairs.add(preNode.getPosition());
//                        System.out.println(" set edge impassable   " + preNode.getNeighbourAreaIds());
                    }
                    Area preArea = (Area) world.getEntity(previousPosition);
//                    System.out.println(" move to previous area " + previousPosition.getValue() + "................");
                    pathPlanner.move(preArea, IN_TARGET, true);
//                    pathPlanner.moveToPoint(previousPosition, preArea.getX(), preArea.getY());
                } else {
//                    world.printData("naaaaaaaaaaaaaaaaaaaaaa");
                }
            }

            if (hardWalkingPairs.size() > 0) {
                // avalin harekat.
                Pair<Integer, Integer> nextPoint;
                nextPoint = hardWalkingPairs.get(0);
                hardWalkingPairs.remove(0);
                pathPlanner.moveToPoint(selfPosition.getID(), nextPoint.first(), nextPoint.second());
            }
        }
    }

    private void addAfterEntrancePoint(Area area) {
        // age area-e ba'di entrance bood mire be area-e ba'd az entrance.
        // in vase mavaghei khube ke agent ha too entrance ha gir mikonan.
        EntityID nextAreaId = null;
        boolean isEntrance = false;

        for (EntityID id : area.getNeighbours()) {
            if (world.getEntity(id) instanceof Building) {
                isEntrance = true;
            } else {
                nextAreaId = id;
            }
        }

        if (isEntrance) {
            Grid selectedGrid;
            AreaGrids areaGrids;
            Pair<Integer, Integer> xYPair;
            int distanceToEdgeCenter;
            int minDistance = Integer.MAX_VALUE;

            if (nextAreaId != null) {
                selectedGrid = null;
                xYPair = hardWalkingPairs.get(hardWalkingPairs.size() - 1);
                areaGrids = areaPassable.getAreaGrids(nextAreaId);

                for (Grid grid : areaGrids.getGrids()) {

                    distanceToEdgeCenter = Util.distance(grid.getPosition(), xYPair);

                    if (distanceToEdgeCenter < minDistance) {
                        selectedGrid = grid;
                        minDistance = distanceToEdgeCenter;
                    }
                }
                if (selectedGrid != null) {
                    hardWalkingPairs.add(selectedGrid.getPosition());
                }
//                System.out.println(world.getSelf() + "  after entrance:" + nextAreaId.getValue() + " one Grid : " + selectedGrid.getId()+" hardWalkingPairs:"+hardWalkingPairs);
            }
        }
    }

    private void hardWalkingMove(Area selfPosition) throws CommandException {
        /**
         *  in method baraye sari' angam dadane hard walk neveshte shode.
         * be insorat ke agar chang grid poshte sare ham bashand az hameye anha dar yek harekat oboor mikonad.
         */
        List<Pair<Integer, Integer>> toRemove = new ArrayList<Pair<Integer, Integer>>();
        Pair<Integer, Integer> nextPoint;
        double m0;
        double m;
        double xStart = world.getSelfLocation().first();
        double yStart = world.getSelfLocation().second();
        double xEnd = hardWalkingPairs.get(0).first();
        double yEnd = hardWalkingPairs.get(0).second();

        int pairSize = hardWalkingPairs.size();
        Pair<Integer, Integer> pair;
        double numerator = yEnd - yStart;
        double denominator = xEnd - xStart;

        toRemove.add(hardWalkingPairs.get(0));
        nextPoint = hardWalkingPairs.get(0);

        if (denominator != 0) {
            if (numerator == 0) {
                for (int i = 1; i < pairSize; i++) {
                    pair = hardWalkingPairs.get(i);
                    numerator = pair.second() - yStart;
                    if (numerator < 200) {
                        toRemove.add(pair);
                        nextPoint = pair;
                        yStart = pair.second();
                    } else {
                        break;
                    }
                }
            } else {
                m0 = Math.toDegrees(Math.atan(numerator / denominator));
                for (int i = 1; i < pairSize; i++) {
                    pair = hardWalkingPairs.get(i);
                    numerator = pair.second() - yStart;
                    denominator = pair.first() - xStart;
                    if (denominator != 0) {
                        m = Math.toDegrees(Math.atan(numerator / denominator));
                        if (Math.abs(m0 - m) < 20) {
                            toRemove.add(pair);
                            nextPoint = pair;
                            yStart = pair.second();
                            xStart = pair.first();
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
        } else {
            for (int i = 1; i < pairSize; i++) {
                pair = hardWalkingPairs.get(i);
                denominator = pair.first() - xStart;
                if (denominator < 100) {
                    toRemove.add(pair);
                    nextPoint = pair;
                    xStart = pair.first();
                } else {
                    break;
                }
            }
        }

        if (nextPoint != null) {
//            world.printData(" ------------------ LONG HARD WALK toRemove size = "+toRemove.size()+" hardWalkPair size = "+hardWalkingPairs.size());
            hardWalkingPairs.removeAll(toRemove);
            pathPlanner.moveToPoint(selfPosition.getID(), nextPoint.first(), nextPoint.second());
        }
    }

    private List<Pair<Integer, Integer>> getHardWalkingPairs(Area selfPosition, Area targetArea) {
        /**
         * in method aval nazdiktarin grid be agent ro peyda mikone.
         * ba'd nazdiktarin grid be vasate edge target ro peyda mikone.
         * hala ba estefade az in source grid va destination grid path peyda mikone.
         * akhare sar ham ye A* khoshgel mizane.
         */
        List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>();
        AreaGrids areaGrids = areaPassable.getAreaGrids(selfPosition.getID());
        EntityID targetId = targetArea.getID();
        Pair<Integer, Integer> selfXY = world.getSelfLocation();

        if (areaGrids == null) {
            // dorost kardane grid vase in area-ei ke hast.
            areaGrids = new AreaGrids(selfPosition, updateGridPassable.getGridSize());
            areaPassable.addVisitedAreaGrid(selfPosition, areaGrids);
        }

        updateGridPassable.update(selfPosition, areaGrids, false);
        Grid selfGrid = areaPassable.getBestGrid(areaGrids.getGrids(), selfXY, null);

        if (selfGrid == null) {
            // too area-e khodesh grid peida nakarde. pas mire soraghe area-e ba'di(target).
            return list;
        }

        Grid targetGrid;
        Pair<Integer, Integer> targetBiggestEdgeXY = targetArea.getLocation(world);
        int size, maxSize = 0;

        // inja markaze bozorgtarin edge too area-e khodesh ke neighbouresh target bashe ro bedast miare.
        for (Edge edge : selfPosition.getEdges()) {
            if (edge.isPassable()) {
                if (edge.getNeighbour().equals(targetId)) {

                    size = Math.round(EdgeHelper.getEdgeLength(edge));
                    if (size > maxSize) {
                        targetBiggestEdgeXY = EdgeHelper.getEdgeMiddle(edge);
                        maxSize = size;
                    }
                }
            }
        }

        // age too area-e khodesh self grid ro peida karde bashe inja miad. age na false.
        targetGrid = areaPassable.getBestGrid(areaGrids.getGrids(), targetBiggestEdgeXY, targetId);

        if (selfGrid.equals(targetGrid)) {
            list.add(selfGrid.getPosition());
        } else {
            // ba'd az bedast avordane target grid too areae khodesh pair path ro bedast miare.
            list = areaPassable.getHardWalkPairPath(areaGrids, selfGrid, targetGrid);
            if (list.size() < 2) {
                list.clear();
            }
        }
        return list;
    }

    private EntityID getNextArea(List<EntityID> path, EntityID selfPositionId) {
        /**
         * too in pathi ke dasht bahash move mizad area-e ba'di ke alan toosh hast ro bedast miare.
         */
        boolean flag = false;

        for (EntityID id : path) {
            if (flag) {
                return id;
            } else if (selfPositionId.equals(id)) {
                flag = true;
            }
        }
        return null;
    }
}
