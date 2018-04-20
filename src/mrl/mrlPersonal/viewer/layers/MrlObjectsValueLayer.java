package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlBuilding;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.world.object.mrlZoneEntity.MrlZones;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: 6/23/11
 * Time: 12:49 PM
 */
public class MrlObjectsValueLayer extends StandardEntityViewLayer<Area> {
    public static Map<EntityID, MrlZones> ZONE_VALUE_MAP = Collections.synchronizedMap(new FastMap<EntityID, MrlZones>());
    public static Map<EntityID, Collection<StandardEntity>> VISITED_CIVILIAN_MAP = Collections.synchronizedMap(new FastMap<EntityID, Collection<StandardEntity>>());
    NumberFormat format = new DecimalFormat("#.000");
    public boolean showZoneValue = true;
    private RenderZoneValueAction zoneValueAction;
    public boolean showSearchValue;
    private RenderSearchValueAction searchValueAction;
    public boolean showBurningBuildingValue;
    private RenderBurningBuildingValueAction burningBuildingValueAction;
    public boolean showVisitedCivilianValue;
    private RenderVisitedCivilianValueAction visitedCivilianValueAction;

    public MrlObjectsValueLayer() {
        super(Area.class);
    }

    @Override
    public void initialise(Config config) {

        showZoneValue = false;
        zoneValueAction = new RenderZoneValueAction();
        showSearchValue = false;
        searchValueAction = new RenderSearchValueAction();
        showBurningBuildingValue = false;
        burningBuildingValueAction = new RenderBurningBuildingValueAction();
        showVisitedCivilianValue = false;
        visitedCivilianValueAction = new RenderVisitedCivilianValueAction();
    }

    @Override
    public Shape render(Area area, Graphics2D g, ScreenTransform t) {
        if (StaticViewProperties.selectedObject != null) {
            if (showZoneValue || showSearchValue || showBurningBuildingValue) {

                MrlZones zones = null;
                try {
                    zones = ZONE_VALUE_MAP.get(StaticViewProperties.selectedObject.getID());
                } catch (NullPointerException ignored) {
                }

                if (zones != null) {
                    for (MrlZone zone : zones) {
                        for (MrlBuilding building : zone) {

                            if (building.getSelfBuilding().equals(area)) {
                                if (showZoneValue) {
                                    drawInfo(g, t, (format.format(zone.getValue()) + " : " + zone.getId()), zone.getCenter(), area.getClass(), Color.WHITE);
                                } else if (showSearchValue) {
                                    drawInfo(g, t, (format.format(zone.getSearchValue()) + " : " + zone.getId()), zone.getCenter(), area.getClass(), Color.RED);
                                } else if (showBurningBuildingValue && building.BUILDING_VALUE > 0) {
                                    drawInfo(g, t, (format.format(building.BUILDING_VALUE) + " : " + building.getID().getValue()), new Point(building.getSelfBuilding().getX(), building.getSelfBuilding().getY()), area.getClass(), Color.CYAN);
                                }
                            }
                        }
                    }
                }
            } else if (showVisitedCivilianValue) {
                List<StandardEntity> civilians = new ArrayList<StandardEntity>();
                try {
                    civilians.addAll(VISITED_CIVILIAN_MAP.get(StaticViewProperties.selectedObject.getID()));
                } catch (NullPointerException ignored) {
                }

                Civilian civilian;
                if (!civilians.isEmpty()) {
                    for (StandardEntity entity : civilians) {
                        if (entity instanceof Civilian) {
                            civilian = (Civilian) entity;
                            if (civilian.isPositionDefined()) {
                                renderCivAction(g, t, civilian);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "VALUES";
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(zoneValueAction));
        result.add(new JMenuItem(searchValueAction));
        result.add(new JMenuItem(burningBuildingValueAction));
        result.add(new JMenuItem(visitedCivilianValueAction));

        return result;
    }

    private final class RenderZoneValueAction extends AbstractAction {

        public RenderZoneValueAction() {
            super("burning zone");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setZoneRenderInfo(!showZoneValue);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showZoneValue));
            putValue(Action.SMALL_ICON, showZoneValue ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setZoneRenderInfo(boolean render) {
        showZoneValue = render;
        zoneValueAction.update();
    }

    private final class RenderSearchValueAction extends AbstractAction {

        public RenderSearchValueAction() {
            super("search");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setSearchRenderInfo(!showSearchValue);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showSearchValue));
            putValue(Action.SMALL_ICON, showSearchValue ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setSearchRenderInfo(boolean render) {
        showSearchValue = render;
        searchValueAction.update();
    }

    private final class RenderBurningBuildingValueAction extends AbstractAction {

        public RenderBurningBuildingValueAction() {
            super("burning building");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setBurningBuildingRenderInfo(!showBurningBuildingValue);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showBurningBuildingValue));
            putValue(Action.SMALL_ICON, showBurningBuildingValue ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setBurningBuildingRenderInfo(boolean render) {
        showBurningBuildingValue = render;
        burningBuildingValueAction.update();
    }

    private final class RenderVisitedCivilianValueAction extends AbstractAction {

        public RenderVisitedCivilianValueAction() {
            super("visited civilian");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisitedCivilianRenderInfo(!showVisitedCivilianValue);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showVisitedCivilianValue));
            putValue(Action.SMALL_ICON, showVisitedCivilianValue ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setVisitedCivilianRenderInfo(boolean render) {
        showVisitedCivilianValue = render;
        visitedCivilianValueAction.update();
    }

    private void drawInfo(Graphics2D g, ScreenTransform t, String strInfo, Point location, Class clazz, Color color) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.getX());
            y = t.yToScreen(location.getY());
            if (clazz.equals(Building.class)) {
                g.setColor(color);
            }
            g.drawString(strInfo, x - 15, y + 4);
        }
    }

    private void renderCivAction(Graphics2D g, ScreenTransform t, Civilian civilian) {
        String strID = civilian.getID().toString();
        String strHP = "";
        String strDMG = "";
        String strBRD = "";

        if (civilian.isHPDefined()) {
            strHP = String.valueOf(civilian.getHP());
        }
        if (civilian.isDamageDefined()) {
            strDMG = String.valueOf(civilian.getDamage());
        }
        if (civilian.isBuriednessDefined()) {
            strBRD = String.valueOf(civilian.getBuriedness());
        }

        Pair<Integer, Integer> pair = civilian.getLocation(world);
        Point location = new Point(pair.first(), pair.second());
        g.setColor(Color.GREEN);
        drawInfo(g, t, strID, location, -13, 15);
        drawInfo(g, t, strHP, location, -16, -7);
        drawInfo(g, t, strDMG, location, 6, 5);
        drawInfo(g, t, strBRD, location, -25, 5);
    }

    private void drawInfo(Graphics2D g, ScreenTransform t, String strInfo, Point location, int changeXPos, int changeYPos) {
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.x);
            y = t.yToScreen(location.y);
            g.drawString(strInfo, x + changeXPos, y + changeYPos);
        }
    }
}
