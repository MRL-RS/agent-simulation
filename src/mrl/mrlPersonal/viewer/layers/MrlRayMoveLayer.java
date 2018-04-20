package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import math.geom2d.conic.Circle2D;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * @author Mostafa Movahedi
 */
public class MrlRayMoveLayer extends MrlPreRoutingPartitionsLayer {
    private static final Color SCALED_BLOCKADE_COLOR = Color.orange.darker().darker();
    private static final Color OBSTACLES_COLOR = Color.YELLOW;
    private static final Color ESCAPE_POINTS_COLOR = new Color(155, 51, 91);
    private static final Color MOVE_POINT_COLOR = new Color(39, 146, 142);
    private static final Stroke STROKE = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static final Map<EntityID, Polygon> SCALED_BLOCKADE_MAP = Collections.synchronizedMap(new FastMap<>());
    public static final Map<EntityID, List<Polygon>> OBSTACLES_MAP = Collections.synchronizedMap(new FastMap<>());
    public static final Map<EntityID, List<Point2D>> ESCAPE_POINTS_MAP = Collections.synchronizedMap(new FastMap<>());
    public static final Map<EntityID, List<Point2D>> MOVE_POINTS_MAP = Collections.synchronizedMap(new FastMap<>());
    public static final Map<EntityID, List<Line2D>> BOUND_MAP = Collections.synchronizedMap(new FastMap<>());


    private boolean scaledBlockadesInfo;
    private ScaledBlockadesAction scaledBlockadesAction;

    private boolean obstaclesInfo;
    private ObstaclesAction obstaclesAction;

    private boolean escapePointsInfo;
    private EscapePointsAction escapePointsAction;


    public MrlRayMoveLayer() {
        scaledBlockadesInfo = true;
        scaledBlockadesAction = new ScaledBlockadesAction();
        obstaclesInfo = true;
        obstaclesAction = new ObstaclesAction();

        escapePointsInfo = true;
        escapePointsAction = new EscapePointsAction();

    }

    @Override
    public String getName() {
        return "Ray Move";
    }


    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<>();
        result.add(new JMenuItem(scaledBlockadesAction));
        result.add(new JMenuItem(obstaclesAction));
        result.add(new JMenuItem(escapePointsAction));
        return result;
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {


        if (StaticViewProperties.selectedObject != null && SCALED_BLOCKADE_MAP.get(StaticViewProperties.selectedObject.getID()) != null) {
            try {
                Polygon poly = SCALED_BLOCKADE_MAP.get(StaticViewProperties.selectedObject.getID());
                List<Line2D> boundLines = BOUND_MAP.get(StaticViewProperties.selectedObject.getID());
                if (scaledBlockadesInfo && poly != null) {
                    renderScaledBlockadeAction(g, t, poly);
                    if(boundLines!=null) {
                        renderBoundLinesAction(g, t, boundLines);
                    }
                }


                List<Polygon> obstacles = OBSTACLES_MAP.get(StaticViewProperties.selectedObject.getID());
                if (obstacles != null) {
                    obstacles = Collections.synchronizedList(obstacles);
                    if (obstaclesInfo) {
                        renderObstaclesAction(g, t, obstacles);
                    }
                }

                List<Point2D> escapePoints = ESCAPE_POINTS_MAP.get(StaticViewProperties.selectedObject.getID());
                List<Point2D> movePoints = MOVE_POINTS_MAP.get(StaticViewProperties.selectedObject.getID());
                if (escapePoints != null && movePoints != null) {
                    escapePoints = Collections.synchronizedList(escapePoints);
                    if (escapePointsInfo) {
                        renderEscapePointAction(g, t, escapePoints, movePoints);
                    }
                }

            } catch (NullPointerException ignored) {
                ignored.printStackTrace();
            }


        }
        return new ArrayList<>();
    }


    public static int[] listToArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
            array[i] = list.get(i);
        return array;
    }

    private void renderScaledBlockadeAction(Graphics2D g, ScreenTransform t, Polygon blockadePoly) {
        g.setStroke(STROKE);
        g.setColor(SCALED_BLOCKADE_COLOR);
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
//        for (Polygon polygon : blockadePoly) {
        xs.clear();
        ys.clear();

        for (int x : blockadePoly.xpoints) {
            xs.add(t.xToScreen(x));
        }
        for (int y : blockadePoly.ypoints) {
            ys.add(t.yToScreen(y));
        }

        Polygon poly = new Polygon(listToArray(xs), listToArray(ys), blockadePoly.npoints);
        g.draw(poly);

//        }

    }


    private void renderBoundLinesAction(Graphics2D g, ScreenTransform t, List<Line2D> boundLines) {
        g.setStroke(STROKE);
        g.setColor(SCALED_BLOCKADE_COLOR);
        for (Line2D line : boundLines) {
            g.drawLine(
                    t.xToScreen(line.getOrigin().getX()),
                    t.yToScreen(line.getOrigin().getY()),
                    t.xToScreen(line.getEndPoint().getX()),
                    t.yToScreen(line.getEndPoint().getY())
            );
        }
    }

    private void renderObstaclesAction(Graphics2D g, ScreenTransform t, List<Polygon> obstacles) {
        g.setStroke(STROKE);
        g.setColor(OBSTACLES_COLOR);
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (Polygon polygon : obstacles) {
            xs.clear();
            ys.clear();

            for (int x : polygon.xpoints) {
                xs.add(t.xToScreen(x));
            }
            for (int y : polygon.ypoints) {
                ys.add(t.yToScreen(y));
            }

            Polygon poly = new Polygon(listToArray(xs), listToArray(ys), polygon.npoints);
            g.draw(poly);

        }

    }

    private void renderEscapePointAction(Graphics2D g, ScreenTransform t, List<Point2D> escapePoints, List<Point2D> movePoint) {
        g.setStroke(STROKE);
        g.setColor(ESCAPE_POINTS_COLOR);
        int x, y;
        for (Point2D point : escapePoints) {

            x = t.xToScreen(point.getX());
            y = t.yToScreen(point.getY());

            Circle2D circle2D = new Circle2D(x, y, 2d);

            Color temp = g.getColor();
            circle2D.fill(g);
            g.setColor(temp);
        }
        g.setColor(MOVE_POINT_COLOR);

        for (Point2D point : movePoint) {

            x = t.xToScreen(point.getX());
            y = t.yToScreen(point.getY());

            Circle2D circle2D = new Circle2D(x, y, 4d);

            Color temp = g.getColor();
            circle2D.fill(g);
            g.setColor(temp);
        }


    }

    private final class ScaledBlockadesAction extends AbstractAction {

        public ScaledBlockadesAction() {
            super("scaled blockades");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setScaledBlockadesInfo(!scaledBlockadesInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(scaledBlockadesInfo));
            putValue(Action.SMALL_ICON, scaledBlockadesInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setScaledBlockadesInfo(boolean render) {
        scaledBlockadesInfo = render;
        scaledBlockadesAction.update();
    }

    private final class ObstaclesAction extends AbstractAction {

        public ObstaclesAction() {
            super("obstacles");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setObstaclesInfo(!obstaclesInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(obstaclesInfo));
            putValue(Action.SMALL_ICON, obstaclesInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setObstaclesInfo(boolean render) {
        obstaclesInfo = render;
        obstaclesAction.update();
    }


    private final class EscapePointsAction extends AbstractAction {

        public EscapePointsAction() {
            super("Escape Points");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setEscapePointsActionInfo(!escapePointsInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(escapePointsInfo));
            putValue(Action.SMALL_ICON, escapePointsInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setEscapePointsActionInfo(boolean render) {
        escapePointsInfo = render;
        escapePointsAction.update();
    }

}
