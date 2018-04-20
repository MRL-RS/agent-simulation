package mrl.world.routing.grid;

import mrl.MrlPersonalData;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.helper.AreaHelper;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Oct 17, 2010
 * Time: 12:46:56 PM
 */
public class AreaGrids implements MRLConstants {

    private List<Grid> grids = new ArrayList<Grid>();
    private int maxSearchDistance;
    int idGenerator = 0;
    int gridSize;

    public AreaGrids(Area area, int gridSize) {
        this.gridSize = gridSize;
        createAreaGrids(area);
    }

    private void createAreaGrids(Area area) {
        /**
         * har area ro aval bar asase bozorgtarin edgesh rotate mikone ta amood beshe.
         * ba'd grid bandi mikone. ke age te'dade grid ha basize mamooli bishtar az 6 ta shod size ro dobarabar mikone.
         * injoori te'dade grid ha kam mishe.
         * ba'd az grid bandi grid ha ro dobare rotate karde va sare jaye avalesh barmigardoone.
         */

        // ROTATE AREA
        int[] allApexes = area.getApexList();
        int count = allApexes.length / 2;
        int[] xs = new int[count];
        int[] ys = new int[count];

        double cx = area.getShape().getBounds().getCenterX();
        double cy = area.getShape().getBounds().getCenterY();

        double alpha = 0;
        Edge longestEdge = AreaHelper.getLongestEdge(area);
        double xStart = longestEdge.getStartX();
        double yStart = longestEdge.getStartY();
        double xEnd = longestEdge.getEndX();
        double yEnd = longestEdge.getEndY();

        double numerator = yEnd - yStart;
        double denominator = xEnd - xStart;
        double mRoad = 0;

        AffineTransform at = AffineTransform.getTranslateInstance(0, 0);
        Polygon pol = new Polygon();
//        Shape shape;
        Shape rotatedShape;

        for (int i = 0; i < count; ++i) {
            xs[i] = allApexes[i * 2];
            ys[i] = allApexes[i * 2 + 1];
        }

        for (int i = 0; i < count; i++) {
            pol.addPoint(xs[i], ys[i]);
        }

        if (denominator != 0) {

            mRoad = numerator / denominator;
            alpha = Math.toDegrees(Math.atan(mRoad));

            if (alpha > 0 && alpha < 45) {
                // nothing to do
            } else if (alpha > 45) {
                alpha = alpha - 90;
            } else if (alpha < 0 && alpha > (-45)) {
                // nothing to do
            } else if (alpha < (-45)) {
                alpha = 90 + alpha;  // because alpha is negative.
            }
            alpha = (-1) * alpha;
        }

        if (mRoad == 0 || alpha == -90) {
            alpha = 0;
        }
        alpha = Math.toRadians(alpha);

        at.rotate(alpha, cx, cy);
        rotatedShape = at.createTransformedShape(pol);
        // end rotate area

        // baraye bargardandane grid ha be sare jaye asli.
        alpha = (-1) * alpha;

        // create grids an reverse them.
        createGrids(area, rotatedShape, at, alpha, cx, cy);
    }

    public void createGrids(Area area, Shape areaShape, AffineTransform at, double alpha, double cx, double cy) {

        // create grids
        Rectangle2D areaBound = areaShape.getBounds2D();
        int startX = (int) areaBound.getX(); // shape x
        int startY = (int) areaBound.getY(); // shape y
        double width = areaBound.getWidth(); // shape width
        double height = areaBound.getHeight(); // shape height
        double finalX = startX + width; // shape final x
        double finalY = startY + height; // shape final y

        int widthNumber = (int) (width / gridSize); // chand ta grid dar in arz mitavan gharar dad.
        int heightNumber = (int) (height / gridSize); // chnd grid mitavan dar ba in ertefa' gharar dad.

        // age kamtarin te'dade grid ha bishtar az 6 bashe size grid ha 2 barabar mishe.
        if (width < height) {
            if (widthNumber > 10) {
                widthNumber = (int) (width / (gridSize * 2));
                if (widthNumber < 10) {
                    widthNumber = 10;
                }
//                heightNumber = (int) (height / (gridSize * 2));
            }
        } else {
            if (heightNumber > 10) {
//                widthNumber = (int) (width / (gridSize * 2));
                heightNumber = (int) (height / (gridSize * 2));
                if (heightNumber < 10) {
                    heightNumber = 10;
                }
            }
        }

//        if (width < height) {
//            if (widthNumber >= 9) {
//                widthNumber = (int) (width / (gridSize * 2));
//                heightNumber = (int) (height / (gridSize * 2));
//            }
//        } else {
//            if (heightNumber >= 9) {
//                widthNumber = (int) (width / (gridSize * 2));
//                heightNumber = (int) (height / (gridSize * 2));
//            }
//        }

        // hade aghal yek grid dorost mikone.
        if (widthNumber < 1) {
            widthNumber = 1;
        }
        if (heightNumber < 1) {
            heightNumber = 1;
        }

        // arze grid ha
        int widthDist = (int) (width / widthNumber);
        // toole grid ha
        int heightDist = (int) (height / heightNumber);

        int widthCounter;
        int heightCounter = 0;
        int thisGridFinalY;
        int thisGridFinalX;

        // az (startX, startY) shoroo karde va be soorate ofoghi ba tavajoh be size grid ha edame mide.
        for (int y = startY; y < finalY; y += heightDist) {
            heightCounter++;
            widthCounter = 0;
            thisGridFinalY = y + heightDist;

            for (int x = startX; x < finalX; x += widthDist) {
                widthCounter++;

                int id = idGenerator;

                if (!areaShape.contains((x + (widthDist / 2)), (y + (heightDist / 2)))) {
                    continue;
                }

                Pair<Integer, Integer> position = new Pair<Integer, Integer>((x + (widthDist / 2)), (y + (heightDist / 2)));

                Grid grid = new Grid(id, area.getID(), position);

                thisGridFinalX = x + widthDist;

                if (widthCounter == widthNumber) {
                    thisGridFinalX = (int) Math.round(finalX + 0.5);
                }
                if (heightCounter == heightNumber) {
                    thisGridFinalY = (int) Math.round(finalY + 0.5);
                }

                grid.addVertex(new Pair<Integer, Integer>(x, y));// 0 - southwest vertex
                grid.addVertex(new Pair<Integer, Integer>(thisGridFinalX, y));// 1 - southeast vertex
                grid.addVertex(new Pair<Integer, Integer>(thisGridFinalX, thisGridFinalY));// 2 - northeast vertex
                grid.addVertex(new Pair<Integer, Integer>(x, thisGridFinalY));// 3 - northwest vertex

//                if (areaShape.contains(position.first(), position.second())) {
                addGrid(grid);
                idGenerator++;
//                }
            }
        }

        rotateGrids(at, alpha, cx, cy);

        gridNeighbours(area, widthNumber);

        // ba tavajoh be te'dade grid ha maximum size search baraye A* bedast miayad.
        maxSearchDistance = grids.size();

        MrlPersonalData.VIEWER_DATA.setAllGrids(area.getID(), grids);

    }

    private void rotateGrids(AffineTransform at, double alpha, double cx, double cy) {
        for (Grid grid : grids) {
            List<Pair<Double, Double>> points = new ArrayList<Pair<Double, Double>>();
            List<Pair<Double, Double>> rPoints;

            points.add(new Pair<Double, Double>((double) grid.getPosition().first(), (double) grid.getPosition().second()));

            for (Pair<Integer, Integer> pair : grid.getVertices()) {
                points.add(new Pair<Double, Double>((double) pair.first(), (double) pair.second()));
            }

            rPoints = rotatePoints(points, at, alpha, cx, cy);

            grid.setPosition(new Pair<Integer, Integer>((int) Math.round(rPoints.get(0).first()), (int) Math.round((rPoints.get(0).second()))));
            rPoints.remove(0);

            grid.getVertices().clear();
            for (Pair<Double, Double> pair : rPoints) {
                grid.addVertex(new Pair<Integer, Integer>((int) Math.round(pair.first()), (int) Math.round(pair.second())));
            }
        }
    }

    private List<Pair<Double, Double>> rotatePoints(List<Pair<Double, Double>> points, AffineTransform at, double alpha, double cx, double cy) {
        /**
         * liste point ha ra gerefte va anha ra ba estefade az affineTransform, alpha, mabda' (cx, cy) rotate midahad.
         */
        List<Pair<Double, Double>> rotatedPoints = new ArrayList<Pair<Double, Double>>();
        Point2D point = new Point(), rotatedPoint = new Point();

        for (Pair<Double, Double> pair : points) {

            point.setLocation(pair.first(), pair.second());
            at = AffineTransform.getRotateInstance(alpha, cx, cy);
            rotatedPoint = at.transform(point, rotatedPoint);

            rotatedPoints.add(new Pair<Double, Double>(rotatedPoint.getX(), rotatedPoint.getY()));
        }
        return rotatedPoints;
    }

    private void gridNeighbours(Area area, int widthNumber) {
        // add grid neighbours
        for (Grid grid : getGrids()) {
            // peyda kadane hamsaye ha va ezafe kardan be grid ha.
            addGridNeighbours(grid, widthNumber);
            // ezafe kardane edge haye passable va area haye neighbour be grid.
            addGridAreaNeighbour(grid, area);
        }
    }

    private void addGridNeighbours(Grid grid, int widthNumber) {
        /**
         * aval bar asase id, grid haye khasi ra entekhab karde.
         * ba'd check mikonim ke vertex haye anha baham hamkhani dashte bashand.
         * masalan age hamsaye samte rasti ra bekhahim bedast avarim bayad
         *  vertex rast-paeen in grid ba vertex chap-paeen grid hamsaye yeki bashad.
         * va ...
         */
        int gridId = grid.getId();
        int neighbourId;
        int dist;

        for (Grid neighbour : grids) {

            neighbourId = neighbour.getId();
            Pair<Grid, Integer> neib;
            if ((neighbourId > gridId && neighbourId < (gridId + (2 * widthNumber)))
                    || (neighbourId < gridId && neighbourId > (gridId - (2 * widthNumber)))) {
                boolean flag = false;
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 4; j++) {
                        if (grid.getVertices().get(i).equals(neighbour.getVertices().get(j))) {
                            dist = Util.distance(neighbour.getPosition(), grid.getPosition());
                            neib = new Pair<Grid, Integer>(neighbour, dist);
                            grid.addNeighbour(neib);
                            neib = new Pair<Grid, Integer>(grid, dist);
                            neighbour.addNeighbour(neib);
                            flag = true;
                            break;
                        }
                    }
                    if (flag) {
                        break;
                    }
                }
            }
//            // north neighbour and east neighbour
//            if (neighbourId >= (gridId + 1) && neighbourId < (gridId + (2 * widthNumber))) {
//
//                if (grid.getVertices().get(2).equals(neighbour.getVertices().get(1))
//                        || grid.getVertices().get(2).equals(neighbour.getVertices().get(0))
//                        || grid.getVertices().get(3).equals(neighbour.getVertices().get(1))
//                        || grid.getVertices().get(3).equals(neighbour.getVertices().get(0))) {
//                    dist = Util.distance(neighbour.getPosition(), grid.getPosition());
//                    neib = new Pair<Grid, Integer>(neighbour, dist);
//                    grid.addNeighbour(neib);
//                    neib = new Pair<Grid, Integer>(grid, dist);
//                    neighbour.addNeighbour(neib);
//                }
//            }
//            // south neighbour and west neighbour
//            if (neighbourId <= (gridId - 1) && neighbourId > (gridId - (2 * widthNumber))) {
//
//                if (grid.getVertices().get(0).equals(neighbour.getVertices().get(3))
//                        || grid.getVertices().get(0).equals(neighbour.getVertices().get(2))
//                        || grid.getVertices().get(1).equals(neighbour.getVertices().get(3))
//                        || grid.getVertices().get(1).equals(neighbour.getVertices().get(2))) {
//                    dist = Util.distance(neighbour.getPosition(), grid.getPosition());
//                    neib = new Pair<Grid, Integer>(neighbour, dist);
//                    grid.addNeighbour(neib);
//                    neib = new Pair<Grid, Integer>(grid, dist);
//                    neighbour.addNeighbour(neib);
//
//                }
//            }
//           // east neighbour
//            if (neighbourId == (gridId + 1)) {
//
//                if (grid.getVertices().get(1).equals(neighbour.getVertices().get(0))
//                        && grid.getVertices().get(2).equals(neighbour.getVertices().get(3))) {
//                    dist = Util.distance(neighbour.getPosition(), grid.getPosition());
//                    neib = new Pair<Grid, Integer>(neighbour, dist);
//                    grid.addNeighbour(neib);
//                    neib = new Pair<Grid, Integer>(grid, dist);
//                    neighbour.addNeighbour(neib);
//                }
//            }
//            //west neighbour
//            if (neighbourId == (gridId - 1)) {
//
//                if (grid.getVertices().get(0).equals(neighbour.getVertices().get(1))
//                        && grid.getVertices().get(3).equals(neighbour.getVertices().get(2))) {
//                    dist = Util.distance(neighbour.getPosition(), grid.getPosition());
//                    neib = new Pair<Grid, Integer>(neighbour, dist);
//                    grid.addNeighbour(neib);
//                    neib = new Pair<Grid, Integer>(grid, dist);
//                    neighbour.addNeighbour(neib);
//                }
//            }
        }
    }

    private void addGridAreaNeighbour(Grid grid, Area area) {
        /**
         * ba estefade az polygon har grid mitavan fahmid ke in grid rooye kodam edge ha gharar darad.
         * agar aan edge passable bashan neighbour aan edge areaNeighbour in grid ham hast.
         */

        Polygon gridPolygon = new Polygon();

        for (int i = 0; i < 4; i++) {
            gridPolygon.addPoint(grid.getVertices().get(i).first(), grid.getVertices().get(i).second());
        }

        grid.setPolygon(gridPolygon);

        for (Edge edge : area.getEdges()) {
            if (edge.isPassable()) {
                if (gridPolygon.getBounds2D().intersectsLine(edge.getStartX(), edge.getStartY(), edge.getEndX(), edge.getEndY())) {

                    grid.addNeighbourAreaId(edge.getNeighbour());
                }
            }
        }
    }

    public void addGrid(Grid grid) {
        this.grids.add(grid);
    }

    public List<Grid> getGrids() {
        return grids;
    }

    public Grid getGrid(Integer id) {
        for (Grid grid : grids) {
            if (grid.getId() == id) {
                return grid;
            }
        }
        return null;
    }

    public int getMaxSearchDistance() {
        return maxSearchDistance;
    }
}
