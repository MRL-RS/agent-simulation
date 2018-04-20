package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.partitioning.Partition;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * User: Vahid Hooshangi
 */
public class MrlSubClusterLayer extends MrlAreaLayer<Building> {
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private boolean visible;
    protected Random random;

    public static List<Partition> Partitions = null;
    public static double point[] = new double[4];

    private boolean buildingInfo;
    private RenderBuildingInfoAction buildingInfoAction;


    /**
     * Construct an area view layer.
     */
    public MrlSubClusterLayer() {
        super(Building.class);
    }

    @Override
    public void initialise(Config config) {
        buildingInfo = false;
        buildingInfoAction = new RenderBuildingInfoAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(buildingInfoAction));

        return result;

    }


    @Override
    protected void paintShape(Building area, Polygon p, Graphics2D g) {


    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {
        g.setStroke(STROKE);
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        Color COLOUR;


        for (Partition partition : Partitions) {
            for (Partition subPartition : partition.getSubPartitions()) {
                int[] x = new int[subPartition.getPolygon().npoints];
                int[] y = new int[subPartition.getPolygon().npoints];
                for (int i = 0; i < subPartition.getPolygon().npoints; i++) {
                    x[i] = t.xToScreen(subPartition.getPolygon().xpoints[i]);
                    y[i] = t.yToScreen(subPartition.getPolygon().ypoints[i]);
                }

                Polygon shape = new Polygon(x, y, subPartition.getPolygon().npoints);
                random = new Random(7 * partition.getId().getValue() * MrlViewer.randomValue);
                COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
                g.setColor(COLOUR);


                g.drawString(String.valueOf(partition.getId()), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY());
                g.draw(shape);
            }

        }


        if (buildingInfo) {
            renderBuilding(Partitions, g, t);
        }

        return list;
    }

    private void renderBuilding(List<Partition> subPartition, Graphics2D g, ScreenTransform t) {

        Color color = Color.red;
        int[] xp;
        int[] yp;
        int count;
        int i;
        Polygon shape;

        for (Partition partition : subPartition) {
            g.setColor(color);
            for (MrlBuilding building : partition.getBuildings()) {
                Building b = (Building) world.getEntity(building.getID());
                count = b.getEdges().size();
                xp = new int[count];
                yp = new int[count];

                i = 0;

                for (Edge e : b.getEdges()) {
                    xp[i] = t.xToScreen(e.getStartX());
                    yp[i] = t.yToScreen(e.getStartY());
                    ++i;
                }
                shape = new Polygon(xp, yp, count);
                g.fill(shape);
            }
            random = new Random(partition.getId().getValue() * 1000 * partition.getId().getValue() * 1000 * System.currentTimeMillis());
            color = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
        }

    }

    private final class RenderBuildingInfoAction extends AbstractAction {
        public RenderBuildingInfoAction() {
            super("building info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setBuildingRenderInfo(!buildingInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(buildingInfo));
            putValue(Action.SMALL_ICON, buildingInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setBuildingRenderInfo(boolean render) {
        buildingInfo = render;
        buildingInfoAction.update();
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
        return "Sub Clusters";
    }
}
