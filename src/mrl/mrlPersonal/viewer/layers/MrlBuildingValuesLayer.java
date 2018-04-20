package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MrlBuildingValuesLayer extends MrlAreaLayer<Building> {
    private boolean visible;
    public static Map<EntityID, List<MrlBuilding>> BUILDING_VALUES = new FastMap<EntityID, List<MrlBuilding>>();
    private static final Color valueColor = Color.WHITE;

    /**
     * Construct an area view layer.
     */
    public MrlBuildingValuesLayer() {
        super(Building.class);
    }

    @Override
    public void initialise(Config config) {
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        return new ArrayList<JMenuItem>();
    }


    @Override
    public Shape render(Building building, Graphics2D g, ScreenTransform t) {
        if (StaticViewProperties.selectedObject != null) {
            if (BUILDING_VALUES.get(StaticViewProperties.selectedObject.getID()) != null) {
                List<MrlBuilding> buildingValues = Collections.synchronizedList(BUILDING_VALUES.get(StaticViewProperties.selectedObject.getID()));
                g.setColor(valueColor);
                int x;
                int y;
                for (MrlBuilding b : buildingValues) {
                    if (!Double.isNaN(b.BUILDING_VALUE)) {
                        Point2D point = new math.geom2d.Point2D(b.getSelfBuilding().getShape().getBounds2D().getCenterX(), b.getSelfBuilding().getShape().getBounds2D().getCenterY());
                        x = t.xToScreen(point.getX());
                        y = t.yToScreen(point.getY());
                        g.drawString(String.valueOf(b.BUILDING_VALUE), x, y);
                    }
                }
            }
        }
        return null;
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
        return "Building Values";
    }
}

