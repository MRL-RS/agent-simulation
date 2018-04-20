package mrl.mrlPersonal.viewer.layers;

import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.view.Icons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: Pooya Deldar Gohardani
 * Date: 6/2/12
 * Time: 9:39 PM
 */
public class MrlBlockadeLayer extends StandardEntityViewLayer<Blockade> {

    public MrlBlockadeLayer() {
        super(Blockade.class);
    }

    private boolean repairInfo;
    private RenderInfoAction repairInfoAction;


    @Override
    public void initialise(Config config) {
        super.initialise(config);

        repairInfo = false;
        repairInfoAction = new RenderInfoAction();

    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(repairInfoAction));

        return result;

    }

    @Override
    public Shape render(Blockade blockade, Graphics2D graphics2D, ScreenTransform screenTransform) {

        if (repairInfo) {
            renderInfoAction(blockade, graphics2D, screenTransform);
        }

        return null;
    }


    private void renderInfoAction(Blockade blockade, Graphics2D graphics2D, ScreenTransform screenTransform) {

        String strID = blockade.getID().toString();
        String strRepairCost = String.valueOf(blockade.getRepairCost());
        graphics2D.setColor(Color.GREEN);
        Pair<Integer, Integer> location = blockade.getLocation(world);
        drawInfo(graphics2D, screenTransform, strID, location, -13, 15);
        drawInfo(graphics2D, screenTransform, strRepairCost, location, -16, -7);


    }

    private void drawInfo(Graphics2D g, ScreenTransform t, String strInfo, Pair<Integer, Integer> location, int changeXPos, int changeYPos) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.first());
            y = t.yToScreen(location.second());
            g.drawString(strInfo, x + changeXPos, y + changeYPos);
        }
    }


    @Override
    public String getName() {
        return "Blockade";
    }

    private final class RenderInfoAction extends AbstractAction {
        public RenderInfoAction() {
            super("Blockade Information");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setRepairRenderInfo(!repairInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(repairInfo));
            putValue(Action.SMALL_ICON, repairInfo ? Icons.TICK : Icons.CROSS);
        }
    }


    public void setRepairRenderInfo(boolean render) {
        repairInfo = render;
        repairInfoAction.update();
    }


}
