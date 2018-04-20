package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import javolution.util.FastSet;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.MrlBuilding;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Edge;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * User: mrl
 * Date: 3/29/12
 * Time: 7:47 PM
 */
public class MrlEstimatedLayer extends MrlAreaLayer<Building> {

    private static final Color OUTLINE_COLOUR = Color.GRAY.darker().darker();
    private static final Stroke STROKE = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    NumberFormat format = new DecimalFormat("#.000");
    private static final Color HEATING_COLOUR = Color.YELLOW; //1
    private static final Color BURNING_COLOUR = Color.ORANGE; //2
    private static final Color INFERNO_COLOUR = Color.RED; //3
    private static final Color WATER_DAMAGE_COLOUR = Color.BLUE; //4
    private static final Color MINOR_DAMAGE_COLOUR = new Color(10, 140, 210, 255); //Color.PINK; //5
    private static final Color MODERATE_DAMAGE_COLOUR = new Color(10, 70, 190, 255); //Color.MAGENTA; //6
    private static final Color SEVERE_DAMAGE_COLOUR = new Color(1, 50, 140, 255); //Color.CYAN;  //7
    private static final Color BURNT_OUT_COLOUR = Color.DARK_GRAY.darker(); //8
    private static final Color PROB = Color.PINK;

    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, java.util.List<MrlBuilding>> BURNING_BUILDINGS_MAP = Collections.synchronizedMap(new FastMap<EntityID, java.util.List<MrlBuilding>>());
    public static Set<EntityID> BURNING_BUILDINGS = new FastSet<EntityID>();

    private ScreenTransform transform = new ScreenTransform(0, 0, 0, 0);

    public boolean fillBuildings;
    private Action fillBuildingsAction;

    /**
     * Construct a building view layer.
     */

    public MrlEstimatedLayer() {
        super(Building.class);
    }

    @Override
    public String getName() {
        return "Estimated Burning Buildings";
    }

    @Override
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
//        transform = t;
//        g.setColor(OUTLINE_COLOUR);
//        g.setStroke(e.isPassable() ? ENTRANCE_STROKE : WALL_STROKE);
//        g.drawLine(t.xToScreen(e.getStartX()),
//                t.yToScreen(e.getStartY()),
//                t.xToScreen(e.getEndX()),
//                t.yToScreen(e.getEndY()));
    }

    @Override
    protected void paintShape(Building b, Polygon shape, Graphics2D g) {
        if (StaticViewProperties.selectedObject != null) {

            BURNING_BUILDINGS.clear();
            java.util.List<MrlBuilding> buildings = null;
            try {
                buildings = Collections.synchronizedList(BURNING_BUILDINGS_MAP.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }

            if (buildings != null) {
                showRealFieriness = true;
                for (MrlBuilding bb : buildings) {
                    if (bb.getEstimatedFieryness() > 0 && b.equals(bb.getSelfBuilding())) {
                        drawBurningBuildings(bb, shape, g);
                        BURNING_BUILDINGS.add(bb.getSelfBuilding().getID());
                        return;
                    } else if (bb.isProbablyOnFire() && b.equals(bb.getSelfBuilding())) {
                        g.setColor(PROB);
                        g.fill(shape);
                        //drawInfo(g, (format.format(bb.getEstimatedTemperature()) + " : " + bb.getID().getValue()), new Point(bb.getSelfBuilding().getX(), bb.getSelfBuilding().getY()), Color.CYAN);
                        return;
                    }
                }
            } else {
                showRealFieriness = false;
            }
        }
    }

    private void drawBurningBuildings(MrlBuilding b, Shape shape, Graphics2D g) {
        switch (b.getEstimatedFieryness()) {
            case 1:
                g.setColor(HEATING_COLOUR);
                break;
            case 2:
                g.setColor(BURNING_COLOUR);
                break;
            case 3:
                g.setColor(INFERNO_COLOUR);
                break;
            case 4:
                g.setColor(WATER_DAMAGE_COLOUR);
                break;
            case 5:
                g.setColor(MINOR_DAMAGE_COLOUR);
                break;
            case 6:
                g.setColor(MODERATE_DAMAGE_COLOUR);
                break;
            case 7:
                g.setColor(SEVERE_DAMAGE_COLOUR);
                break;
            case 8:
                g.setColor(BURNT_OUT_COLOUR);
                break;
        }
        if (fillBuildings) {
            g.fill(shape);
        } else {
            g.setStroke(STROKE);

            g.draw(shape);
        }
        g.setColor(Color.BLACK);
        g.drawString(b.getEstimatedTemperature() + "", b.getSelfBuilding().getX(), b.getSelfBuilding().getY());

    }

/*    private void drawFieriness(Building b, Polygon shape, Graphics2D g) {
        StandardEntityToPaint entityToPaint = StaticViewProperties.getPaintObject(b);
        if (entityToPaint != null) {
            g.setColor(entityToPaint.getColor());
            g.fill(shape);
            return;
        }
        if (b instanceof Refuge) {
            g.setColor(REFUGE_BUILDING_COLOR);
            if (b.isFierynessDefined() && b.getFieryness() > 0) {
                g.setColor(BURNING_REFUGE_COLOR);
            }
            g.fill(shape);
        } else if ((b instanceof AmbulanceCentre) || (b instanceof FireStation) || (b instanceof PoliceOffice)) {
            g.setColor(CENTER_BUILDING_COLOR);
            if (b.isFierynessDefined() && b.getFieryness() > 0) {
                g.setColor(BURNING_REFUGE_COLOR);
            }
            g.fill(shape);
        }
        if (!b.isFierynessDefined()) {
            return;
        }
        switch (b.getFierynessEnum()) {
            case UNBURNT:
                return;
            case HEATING:
                g.setColor(HEATING);
                break;
            case BURNING:
                g.setColor(BURNING);
                break;
            case INFERNO:
                g.setColor(INFERNO);
                break;
            case WATER_DAMAGE:
                g.setColor(WATER_DAMAGE);
                break;
            case MINOR_DAMAGE:
                g.setColor(MINOR_DAMAGE);
                break;
            case MODERATE_DAMAGE:
                g.setColor(MODERATE_DAMAGE);
                break;
            case SEVERE_DAMAGE:
                g.setColor(SEVERE_DAMAGE);
                break;
            case BURNT_OUT:
                g.setColor(BURNT_OUT);
                break;
            default:
                throw new IllegalArgumentException("Don't know how to render fieriness " + b.getFierynessEnum());
        }
        g.fill(shape);
    }

    private void drawBrokenness(Building b, Shape shape, Graphics2D g) {
        int brokenness = b.getBrokenness();
        // CHECK STYLE:OFF:MagicNumber
        int colour = Math.max(0, 135 - brokenness / 2);
        // CHECK STYLE:ON:MagicNumber
        g.setColor(new Color(colour, colour, colour));
        g.fill(shape);
    }*/

    @Override
    public void initialise(Config config) {
        fillBuildings = false;
        fillBuildingsAction = new FillBuildingsAction();
    }

    @Override
    public java.util.List<JMenuItem> getPopupMenuItems() {
        java.util.List<JMenuItem> result = new ArrayList<JMenuItem>();
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

    private void drawInfo(Graphics2D g, String strInfo, Point location, Color color) {
        ScreenTransform t = transform;
        int x;
        int y;
        if (strInfo != null) {
            x = t.xToScreen(location.getX());
            y = t.yToScreen(location.getY());
            /*if (clazz.equals(Building.class))*/
            {
                g.setColor(color);
            }
            g.drawString(strInfo, x - 15, y + 4);
        }
    }

}
