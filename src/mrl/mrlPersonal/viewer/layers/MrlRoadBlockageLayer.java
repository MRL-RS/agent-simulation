package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlBlockade;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.view.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 13, 2010
 * Time: 5:42:05 PM
 */
public class MrlRoadBlockageLayer extends StandardEntityViewLayer<Blockade> {
    private static final int BLOCK_SIZE = 3;
    private static final int BLOCK_STROKE_WIDTH = 2;

    private static final Color COLOUR = Color.black;
    public static Set<MrlBlockade> MRL_BLOCKADE_SET = new FastSet<MrlBlockade>();
    public static Map<Road, Set<Blockade>> VIEWER_ROAD_BLOCKADES = new FastMap<Road, Set<Blockade>>();

    private boolean showRoadBlockade;

    private Action roadBlockadeAction;

    /**
     * Construct a road blockage rendering layer.
     */
    public MrlRoadBlockageLayer() {
        super(Blockade.class);
    }

    @Override
    public String getName() {
        return "Road blockages";
    }

    @Override
    public void initialise(Config config) {
        showRoadBlockade = false;
        roadBlockadeAction = new ShowRoadBlockadeAction();
    }

    @Override
    public Shape render(Blockade b, Graphics2D g, ScreenTransform t) {
        if (b.isPositionDefined()) {
            Road road = (Road) world.getEntity(b.getPosition());
            if (!VIEWER_ROAD_BLOCKADES.containsKey(road)) {
                VIEWER_ROAD_BLOCKADES.put(road, new FastSet<Blockade>());
            }
            Set<Blockade> blockSet = new HashSet<Blockade>(VIEWER_ROAD_BLOCKADES.get(road));
            blockSet.add(b);
            VIEWER_ROAD_BLOCKADES.put(road, blockSet);
        }
        int[] apexes = b.getApexes();
        int count = apexes.length / 2;
        int[] xs = new int[count];
        int[] ys = new int[count];
        for (int i = 0; i < count; ++i) {
            xs[i] = t.xToScreen(apexes[i * 2]);
            ys[i] = t.yToScreen(apexes[(i * 2) + 1]);
        }
        Polygon shape = new Polygon(xs, ys, count);
        if (b == StaticViewProperties.selectedObject) {
            g.setColor(Color.MAGENTA);
        } else {
            g.setColor(COLOUR);
        }
        g.fill(shape);
        return shape;
    }


    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
//        result.add(new JMenuItem(roadBlockadeAction));
        return result;
    }

    private final class ShowRoadBlockadeAction extends AbstractAction {
        public ShowRoadBlockadeAction() {
            super("mrl blockade");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRoadBlockade));
            putValue(Action.SMALL_ICON, showRoadBlockade ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showRoadBlockade = !showRoadBlockade;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showRoadBlockade));
            putValue(Action.SMALL_ICON, showRoadBlockade ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }
}