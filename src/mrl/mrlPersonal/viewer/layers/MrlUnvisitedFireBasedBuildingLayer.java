package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.StandardEntityToPaint;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import rescuecore2.config.Config;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Erfan Jazeb Nikoo
 */
public class MrlUnvisitedFireBasedBuildingLayer extends MrlAreaLayer<Building> {

    private static final Color HEATING = new Color(176, 176, 56, 128);
    private static final Color BURNING = new Color(204, 122, 50, 128);
    private static final Color INFERNO = new Color(160, 52, 52, 128);
    private static final Color WATER_DAMAGE = new Color(50, 120, 130, 128);
    private static final Color MINOR_DAMAGE = new Color(100, 140, 210, 128);
    private static final Color MODERATE_DAMAGE = new Color(100, 70, 190, 128);
    private static final Color SEVERE_DAMAGE = new Color(80, 60, 140, 128);
    private static final Color BURNT_OUT = new Color(0, 0, 0, 255);

    private static final Color OUTLINE_COLOUR = Color.GRAY.darker().darker();

    private static final Color REFUGE_BUILDING_COLOR = Color.GREEN.darker();
    private static final Color CENTER_BUILDING_COLOR = Color.WHITE.brighter().brighter();
    private static final Color BURNING_REFUGE_COLOR = Color.RED.darker().darker();

    private static final Color COLOR = Color.CYAN;
    private static final Stroke WALL_STROKE = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke ENTRANCE_STROKE = new BasicStroke(0.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static Map<EntityID, Set<Building>> UNVISITED_FIRE_BASED_BUILDINGS_MAP = Collections.synchronizedMap(new FastMap<EntityID, Set<Building>>());


    /**
     * Construct a building view layer.
     */
    public MrlUnvisitedFireBasedBuildingLayer() {
        super(Building.class);
    }

    @Override
    public String getName() {
        return "UnVisited Fire Based Buildings";
    }

    @Override
    protected void paintEdge(Edge e, Graphics2D g, ScreenTransform t) {
        g.setColor(OUTLINE_COLOUR);
        g.setStroke(e.isPassable() ? ENTRANCE_STROKE : WALL_STROKE);
        g.drawLine(t.xToScreen(e.getStartX()),
                t.yToScreen(e.getStartY()),
                t.xToScreen(e.getEndX()),
                t.yToScreen(e.getEndY()));
    }

    @Override
    protected void paintShape(Building b, Polygon shape, Graphics2D g) {
        if (StaticViewProperties.selectedObject != null) {

            Set<Building> unvisited = null;
            try {
                unvisited = Collections.synchronizedSet(UNVISITED_FIRE_BASED_BUILDINGS_MAP.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
            }

            if (unvisited != null && unvisited.contains(b)) {
                drawUnvisited(shape, g);
            }
        }
    }

    private void drawUnvisited(Polygon shape, Graphics2D g) {
        g.setColor(COLOR);
        g.fill(shape);
    }

    private void drawFieriness(Building b, Polygon shape, Graphics2D g) {
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
    }

    @Override
    public void initialise(Config config) {
    }

}
