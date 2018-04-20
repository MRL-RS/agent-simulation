package mrl.mrlPersonal.viewer.layers;

import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.world.object.AirCell;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * @author Mostafa Shabani
 */
public class MrlAirCellLayer extends StandardViewLayer {
    private static final Color COLOUR = Color.BLUE.brighter();
    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private DecimalFormat df = new DecimalFormat("0.000000");
    private boolean visible;

    public boolean fillBuildings;
    private Action fillBuildingsAction;

    protected Random random;

    public static AirCell[][] AIR_CELL_TEMP = null;
    public static double MAX_X, MAX_Y;

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {

        g.setColor(Color.WHITE);
        g.setStroke(STROKE);
        ScreenTransform t = transform;

        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
//        double cellX = MrlFireBrigadeWorld.X_SAMPLE_SIZE;
//        double cellY = MrlFireBrigadeWorld.Y_SAMPLE_SIZE;

        if (AIR_CELL_TEMP == null) {
            return list;
        }
        for (int x = 0; x < AIR_CELL_TEMP.length; x++) {
            for (int y = 0; y < AIR_CELL_TEMP[x].length; y++) {
                if (fillBuildings) {
                    random = new Random((x + 3) * (x + 3) * (x + 3) * (y + 3) * (y + 3) * (y + 3) * MrlViewer.randomValue);
                    Color c = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
                    List<MrlBuilding> b = AIR_CELL_TEMP[x][y].getBuildings();
                    renderFillBuildings(g, t, c, b);
                }

//                g.setColor(COLOUR);
//                Polygon polygon = new Polygon();
//                polygon.addPoint(t.xToScreen(cellX * (x)), t.yToScreen(cellY * (y)));
//                polygon.addPoint(t.xToScreen(cellX * (x+1)), t.yToScreen(cellY * (y)));
//                polygon.addPoint(t.xToScreen(cellX * (x+1)), t.yToScreen(cellY * (y+1)));
//                polygon.addPoint(t.xToScreen(cellX * (x)), t.yToScreen(cellY * (y+1)));

//                g.draw(polygon);

//                g.drawString("(" + df.format(AIR_CELL_TEMP[x][y].getTemperature()) + ")", ((float) polygon.getBounds().getCenterX() - 20), (float) polygon.getBounds().getCenterY());
            }
        }

        return list;
    }

    private void renderFillBuildings(Graphics2D g, ScreenTransform t, Color c, List<MrlBuilding> b) {
        if (b == null) {
            return;
        }
        Polygon shape_Building;
        int npoints;
        g.setColor(c);

        for (MrlBuilding building : b) {
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
        return "Air Cells";
    }

    @Override
    public void initialise(Config config) {
        fillBuildings = false;
        fillBuildingsAction = new FillBuildingsAction();
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(fillBuildingsAction));
        return result;
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
}
