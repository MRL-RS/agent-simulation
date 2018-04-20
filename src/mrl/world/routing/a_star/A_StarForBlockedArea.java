package mrl.world.routing.a_star;

import mrl.world.routing.grid.AreaGrids;
import mrl.world.routing.grid.Grid;
import rescuecore2.misc.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Oct 11, 2010
 * Time: 5:27:50 PM
 */
public class A_StarForBlockedArea {

    public A_StarForBlockedArea() {
    }

    /**
     * bedast avordane ye masir az grid ha ke agent betune bahash az in area obur kone.
     *
     * @param areaGrids       all grids of this area
     * @param sourceGrid      source
     * @param destinationGrid destination
     * @param isPoliceCheck   police get a best path and their blockades for clear
     * @return list of grids to go
     */
    public List<Grid> getShortestPathForBlockadeRoad(AreaGrids areaGrids, Grid sourceGrid, Grid destinationGrid, boolean isPoliceCheck) {
        /**
         * A Star Algorithm.
         */
        if (sourceGrid == null || destinationGrid == null) {
            return new ArrayList<Grid>();
        }

        ArrayList<Grid> open = new ArrayList<Grid>();
        ArrayList<Grid> closed = new ArrayList<Grid>();
        sourceGrid.setG(0);
        sourceGrid.setCost(0);
        sourceGrid.setDepth(0);
        sourceGrid.setParent(null);
        destinationGrid.setParent(null);
        open.add(sourceGrid);

        int maxDepth = 0;
        int maxSearchDist = areaGrids.getMaxSearchDistance();

        while ((maxDepth < maxSearchDist) && (open.size() != 0)) {

            Grid current = getMinCost(open);

            if (current.equals(destinationGrid)) {
                break;
            }

            open.remove(current);
            closed.add(current);

            for (Pair<Grid, Integer> neighbourPair : current.getNeighbours()) {
                Grid neighbour = neighbourPair.first();

                // dar closed nabashe va passabale bashe va te'dade hamsaye hash be andazei bashe ke ba kenare ham gozashtane oon ha beshe oboor kard.
                if (!closed.contains(neighbour) && (neighbour.isPassable() || isPoliceCheck)) {

                    int neighbourG = neighbourPair.second() + current.getG(); // neighbour weight

                    if (!open.contains(neighbour)) {

                        neighbour.setParent(current.getId());
                        neighbour.setHeuristic(getHeuristicDistance(neighbour, destinationGrid));
                        neighbour.setG(neighbourG);
                        neighbour.setCost(neighbour.getHeuristic() + neighbourG);
                        neighbour.setDepth(current.getDepth() + 1);

                        open.add(neighbour);

                        if (neighbour.getDepth() > maxDepth) {
                            maxDepth = neighbour.getDepth();
                        }

                    } else {

                        if (neighbour.getG() > neighbourG) {

                            neighbour.setParent(current.getId());
                            neighbour.setG(neighbourG);
                            neighbour.setCost(neighbour.getHeuristic() + neighbourG);
                            neighbour.setDepth(current.getDepth() + 1);

                            if (neighbour.getDepth() > maxDepth) {
                                maxDepth = neighbour.getDepth();
                            }
                        }
                    }
                }
            }
        }

        return getPairPathForBlockRoad(areaGrids, destinationGrid);
    }

    private int getHeuristicDistance(Grid grid, Grid destinationGrid) {
        // mohasebe fasele heuristic.
        return (Math.abs(grid.getPosition().first() - destinationGrid.getPosition().first())
                + Math.abs(grid.getPosition().second() - destinationGrid.getPosition().second()));
    }

    private Grid getMinCost(List<Grid> open) {
        // yaftane Grid-e ba'di ba tavajoh be kamtarin meghdare cost.
        Grid bestGrid = open.get(0);
        int minCost = open.get(0).getCost();

        for (Grid grid : open) {
            if (grid.getCost() < minCost) {
                minCost = grid.getCost();
                bestGrid = grid;
            }
        }

        return bestGrid;
    }

    private List<Grid> getPairPathForBlockRoad(AreaGrids areaGrids, Grid destinationGrid) {

        ArrayList<Grid> path = new ArrayList<Grid>();
        ArrayList<Grid> finalPath = new ArrayList<Grid>();
        Grid current = destinationGrid;

        path.add(current);

        while (current.getParent() != null) {
            current = areaGrids.getGrid(current.getParent());
            path.add(current);
        }

        for (int i = path.size() - 1; i >= 0; i--) {
            finalPath.add(path.get(i));
        }

        return finalPath;
    }
}