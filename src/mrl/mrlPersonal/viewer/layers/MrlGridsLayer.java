package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.world.routing.grid.Grid;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Sep 13, 2010
 * Time: 6:13:35 PM
 */
public class MrlGridsLayer extends StandardEntityViewLayer<Area> {

    private Color P_COLOUR = Color.WHITE;// grid color
    private Color G_COLOUR = Color.WHITE.darker().darker();// grid graph color
    private static final Stroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    static EntityID firstAreaId;
    protected Random random;

    // only baraye didane grid ha dar viewer.
    public static Map<EntityID, List<Grid>> ALL_GRIDS = Collections.synchronizedMap(new FastMap<EntityID, List<Grid>>());

    private Action gridGraphAction;
    private boolean gridGraphView = false;

    public MrlGridsLayer() {
        super(Area.class);
    }

    @Override
    public String getName() {
        return "Grids";
    }

    @Override
    public void initialise(Config config) {
        gridGraphAction = new GridGraphAction();
        gridGraphView = false;
    }

    @Override
    public Shape render(Area area, Graphics2D g, ScreenTransform t) {

        g.setStroke(STROKE);

        if (firstAreaId == null) {
            firstAreaId = area.getID();
        }
        ArrayList<Grid> temp = new ArrayList<Grid>();

        // for inke faghat yebar draw kone.
        if (firstAreaId == area.getID()) {
            EntityID areaId = null;
            try {
                for (List<Grid> gridList : ALL_GRIDS.values()) {
                    if (gridList != null) {
                        temp.addAll(gridList);
                    }
                }
                for (Grid grid : temp) {
                    if (grid.isPassable()) {
                        if (areaId == null || grid.getSelfAreaId() != areaId) {
                            areaId = grid.getSelfAreaId();
                            random = new Random(areaId.getValue() * areaId.getValue() * MrlViewer.randomValue);
                            P_COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);

                        }
                        g.setColor(P_COLOUR);

                        int x0 = grid.getVertices().get(0).first();
                        int y0 = grid.getVertices().get(0).second();
                        int x1 = grid.getVertices().get(1).first();
                        int y1 = grid.getVertices().get(1).second();
                        int x2 = grid.getVertices().get(2).first();
                        int y2 = grid.getVertices().get(2).second();
                        int x3 = grid.getVertices().get(3).first();
                        int y3 = grid.getVertices().get(3).second();


                        g.drawLine(t.xToScreen(x0), t.yToScreen(y0), t.xToScreen(x1), t.yToScreen(y1));
                        g.drawLine(t.xToScreen(x1), t.yToScreen(y1), t.xToScreen(x2), t.yToScreen(y2));
                        g.drawLine(t.xToScreen(x2), t.yToScreen(y2), t.xToScreen(x3), t.yToScreen(y3));
                        g.drawLine(t.xToScreen(x3), t.yToScreen(y3), t.xToScreen(x0), t.yToScreen(y0));

                        if (gridGraphView) {
                            g.setColor(G_COLOUR);

                            int xc = grid.getPosition().first();
                            int yc = grid.getPosition().second();

                            Grid gridNeib;

                            for (Pair<Grid, Integer> neib : grid.getNeighbours()) {

                                gridNeib = neib.first();

                                if (gridNeib.isPassable()) {
                                    int xn = gridNeib.getPosition().first();
                                    int yn = gridNeib.getPosition().second();
                                    g.drawLine(t.xToScreen(xc), t.yToScreen(yc), t.xToScreen(xn), t.yToScreen(yn));
                                }
                            }
                        }
                    }
                }
            } catch (ConcurrentModificationException ignored) {
            }
        }
        return null;
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(gridGraphAction));
        return result;
    }

    private final class GridGraphAction extends AbstractAction {
        public GridGraphAction() {
            super("view grids graph");
            putValue(Action.SELECTED_KEY, gridGraphView);
            putValue(Action.SMALL_ICON, gridGraphView ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            gridGraphView = !gridGraphView;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(gridGraphView));
            putValue(Action.SMALL_ICON, gridGraphView ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }
}
