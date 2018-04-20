package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.partitioning.Partition;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author Vahid Hooshagni
 */
public class MrlPartitionsLayer extends StandardViewLayer {
    private static final Color COLOUR = Color.BLUE.brighter();
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private boolean visible;

    public boolean showRoads;
    private Action showRoadsAction;

    public boolean fillRoads;
    private Action fillRoadsAction;

    public boolean fillBuildings;
    private Action fillBuildingsAction;

    public boolean pathsToRefuge;
    private Action pathsToRefugeAction;


    protected Random random;

    public static List<Partition> PARTITIONS = new ArrayList<Partition>();

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {

        g.setColor(COLOUR);
        g.setStroke(STROKE);
        ScreenTransform t = transform;
        Color c = Color.YELLOW;

        int npoints;
        Polygon shape_Building;
        Polygon shape_Road;
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        for (Partition partition : PARTITIONS) {
            int[] x = new int[partition.getPolygon().npoints];
            int[] y = new int[partition.getPolygon().npoints];
            for (int i = 0; i < partition.getPolygon().npoints; i++) {
                x[i] = t.xToScreen(partition.getPolygon().xpoints[i]);
                y[i] = t.yToScreen(partition.getPolygon().ypoints[i]);
                g.setColor(COLOUR.BLUE);
                g.drawString("(" + partition.getPolygon().xpoints[i] + "," + partition.getPolygon().ypoints[i] + ")", x[i], y[i]);

            }

            Polygon shape = new Polygon(x, y, partition.getPolygon().npoints);
            random = new Random(7 * partition.getId().getValue() * MrlViewer.randomValue);
            c = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);


            g.setColor(Color.GREEN);
            g.drawString(String.valueOf(partition.getId()), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY());
            g.setColor(c);
            g.draw(shape);

        }


        if (showRoads) {
            for (Partition p : PARTITIONS) {
                g.setColor(c);
                for (Road road : p.getRoads()) {
                    shape_Road = new Polygon();
                    npoints = road.getApexList().length;
                    for (int i = 0; i < npoints; i += 2) {
                        shape_Road.addPoint(t.xToScreen(road.getApexList()[i]), t.yToScreen(road.getApexList()[i + 1]));
                    }

                    if (!fillRoads) {
                        g.draw(shape_Road);
                    } else {
                        g.fill(shape_Road);
                    }
                }
                c = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
            }
        }

        if (fillBuildings) {
            renderFillBuildings(g, t, c);
        }

        if (pathsToRefuge) {
            renderPathsToRefuge(g, t, c);
        }


        return list;
    }

    private void renderFillBuildings(Graphics2D g, ScreenTransform t, Color c) {
        Polygon shape_Building;
        int npoints;
        for (Partition p : PARTITIONS) {
            g.setColor(c);
            for (MrlBuilding building : p.getBuildings()) {
                shape_Building = new Polygon();
                npoints = building.getSelfBuilding().getApexList().length;
                for (int i = 0; i < npoints; i += 2) {
                    shape_Building.addPoint(t.xToScreen(building.getSelfBuilding().getApexList()[i]), t.yToScreen(building.getSelfBuilding().getApexList()[i + 1]));
                }
                if (!fillBuildings) {
                    g.draw(shape_Building);
                } else {
                    g.fill(shape_Building);
                }
            }

            c = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
        }
    }

    private void renderPathsToRefuge(Graphics2D g, ScreenTransform t, Color c) {
        Polygon shape;
        int npoints;
        Area area;
        for (Partition p : PARTITIONS) {
            g.setColor(c);
            for (EntityID entityID : p.getRefugePathsToClearInPartition()) {
                shape = new Polygon();

                area = (Area) world.getEntity(entityID);
                for (int i = 0; i < area.getApexList().length; i += 2) {
                    shape.addPoint(t.xToScreen(area.getApexList()[i]), t.yToScreen(area.getApexList()[i + 1]));
                }

                g.fill(shape);
            }

            c = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
        }

    }

    @Override
    public void setVisible(boolean b) {
        visible = b;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public String getName() {
        return "Partitions";
    }

    @Override
    public void initialise(Config config) {
        showRoads = false;
        showRoadsAction = new ShowRoadsAction();
        fillRoads = false;
        fillRoadsAction = new FillRoadsAction();
        fillBuildings = false;
        fillBuildingsAction = new FillBuildingsAction();
        pathsToRefuge = false;
        pathsToRefugeAction = new PathsToRefugeAction();

    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(showRoadsAction));
        result.add(new JMenuItem(fillBuildingsAction));
        result.add(new JMenuItem(fillRoadsAction));
        result.add(new JMenuItem(pathsToRefugeAction));

        return result;
    }

    private final class ShowRoadsAction extends AbstractAction {

        public ShowRoadsAction() {
            super("show Roads");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRoads));
            putValue(Action.SMALL_ICON, showRoads ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showRoads = !showRoads;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRoads));
            putValue(Action.SMALL_ICON, showRoads ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }

    }

    private final class FillRoadsAction extends AbstractAction {

        public FillRoadsAction() {
            super("fill Roads");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(fillRoads));
            putValue(Action.SMALL_ICON, fillRoads ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fillRoads = !fillRoads;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(fillRoads));
            putValue(Action.SMALL_ICON, fillRoads ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }

    }

    private final class FillBuildingsAction extends AbstractAction {

        public FillBuildingsAction() {
            super("fill Buildings");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(fillBuildings));
            putValue(Action.SMALL_ICON, fillBuildings ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fillBuildings = !fillBuildings;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(fillBuildings));
            putValue(Action.SMALL_ICON, fillBuildings ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }

    }

    private final class PathsToRefugeAction extends AbstractAction {

        public PathsToRefugeAction() {
            super("Paths To Refuge");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(pathsToRefuge));
            putValue(Action.SMALL_ICON, pathsToRefuge ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            pathsToRefuge = !pathsToRefuge;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(pathsToRefuge));
            putValue(Action.SMALL_ICON, pathsToRefuge ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }

    }

}
