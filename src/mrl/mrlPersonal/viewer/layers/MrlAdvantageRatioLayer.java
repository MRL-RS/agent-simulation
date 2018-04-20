package mrl.mrlPersonal.viewer.layers;

import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MrlAdvantageRatioLayer extends MrlAreaLayer<Building> {
    public static Map<EntityID, Double> BUILDING_ADVANTAGE_RATIO = Collections.synchronizedMap(new HashMap<EntityID, Double>());
    private boolean showValuesInfo;
    private ShowValuesAction showValuesAction;

    /**
     * Construct a building view layer.
     */
    public MrlAdvantageRatioLayer() {
        super(Building.class);
    }

    @Override
    public void initialise(Config config) {
        showValuesInfo = true;
        showValuesAction = new ShowValuesAction();
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(showValuesAction));
        return result;
    }

    @Override
    public String getName() {
        return "Advantage Ratio";
    }

    @Override
    public Shape render(Building b, Graphics2D g, ScreenTransform screenTransform) {
        int color = (BUILDING_ADVANTAGE_RATIO.get(b.getID())).intValue() > 255 ? 255 : (BUILDING_ADVANTAGE_RATIO.get(b.getID())).intValue();
        g.setColor(new Color(255 - color * 6, 255 - color * 5, 255 - color * 10));
        java.util.List<Edge> edges = b.getEdges();
        if (edges.isEmpty()) {
            return null;
        }
        int count = edges.size();
        int[] xs = new int[count];
        int[] ys = new int[count];
        int i = 0;
        for (Edge e : edges) {
            xs[i] = screenTransform.xToScreen(e.getStartX());
            ys[i] = screenTransform.yToScreen(e.getStartY());
            ++i;
        }
        Polygon shape = new Polygon(xs, ys, count);
        g.fill(shape);
        g.setColor(Color.black);
        g.drawString(BUILDING_ADVANTAGE_RATIO.get(b.getID()).toString(), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY());

        if (showValuesInfo) {

        }
        return null;
    }

    private void setShowValuesInfo(boolean showValuesInfo) {
        this.showValuesInfo = showValuesInfo;
        showValuesAction.update();
    }

    @Override
    protected void paintShape(Building b, Polygon shape, Graphics2D g) {

    }

    private final class ShowValuesAction extends AbstractAction {
        public ShowValuesAction() {
            super("Show Values");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setShowValuesInfo(!showValuesInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showValuesInfo));
            putValue(Action.SMALL_ICON, showValuesInfo ? Icons.TICK : Icons.CROSS);
        }
    }

}
