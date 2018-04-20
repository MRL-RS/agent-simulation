package mrl.firebrigade.tools;

import kernel.AgentProxy;
import kernel.Perception;
import mrl.world.MrlWorld;
import rescuecore2.GUIComponent;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.view.*;
import rescuecore2.view.RenderedObject;
import rescuecore2.view.ViewComponent;
import rescuecore2.view.ViewListener;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.WorldModel;
import rescuecore2.worldmodel.properties.IntProperty;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Line of sight perception.
 */
public class MrlLineOfSightPerception implements Perception, GUIComponent {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlLineOfSightPerception.class);

    private static final int DEFAULT_VIEW_DISTANCE = 30000;
    private static final int DEFAULT_HP_PRECISION = 1000;
    private static final int DEFAULT_DAMAGE_PRECISION = 100;
    private static final int DEFAULT_RAY_COUNT = 72;

    private static final String VIEW_DISTANCE_KEY = "perception.los.max-distance";
    private static final String RAY_COUNT_KEY = "perception.los.ray-count";
    private static final String HP_PRECISION_KEY = "perception.los.precision.hp";
    private static final String DAMAGE_PRECISION_KEY = "perception.los.precision.damage";

    private static final IntersectionSorter INTERSECTION_SORTER = new IntersectionSorter();

    private int viewDistance;
    private int hpPrecision;
    private int damagePrecision;
    private int rayCount;
    private List<MrlRay> rays = new ArrayList<MrlRay>();

    private MrlWorld world;
    private Config config;

    private LOSView view;

    /**
     * Create a LineOfSightPerception object.
     */
    public MrlLineOfSightPerception() {
    }

    @Override
    public void initialise(Config newConfig, WorldModel<? extends Entity> model) {
//        world = StandardWorldModel.createStandardWorldModel(model);
        world = (MrlWorld)model;
        this.config = newConfig;
        viewDistance = config.getIntValue(VIEW_DISTANCE_KEY, DEFAULT_VIEW_DISTANCE);
        hpPrecision = config.getIntValue(HP_PRECISION_KEY, DEFAULT_HP_PRECISION);
        damagePrecision = config.getIntValue(DAMAGE_PRECISION_KEY, DEFAULT_DAMAGE_PRECISION);
        rayCount = config.getIntValue(RAY_COUNT_KEY, DEFAULT_RAY_COUNT);
        view = null;
    }

    @Override
    public String toString() {
        return "Line of sight perception";
    }

    @Override
    public JComponent getGUIComponent() {
        if (view == null) {
            view = new LOSView();
            view.refresh();
        }
        return view;
    }

    @Override
    public String getGUIComponentName() {
        return "Line of sight";
    }

    @Override
    public void setTime(int timestep) {
        if (view != null) {
            view.clear();
            view.refresh();
        }
    }

    public List<EntityID> getVisibleAreas(EntityID areaID) {
        Area area = (Area) world.getEntity(areaID);
        List<EntityID> result = new ArrayList<EntityID>();
        // Look for objects within range
        Pair<Integer, Integer> location = area.getLocation(world);
        if (location != null) {
            Point2D point = new Point2D(location.first(), location.second());
            Collection<StandardEntity> nearby = world.getObjectsInRange(location.first(), location.second(), viewDistance);
            Collection<StandardEntity> visible = findVisibleAreas(area, point, nearby);
            for (StandardEntity next : visible) {
                if (next instanceof Area) {
                    result.add(next.getID());
                }
            }
        }
        return result;
    }

    @Override
    public ChangeSet getVisibleEntities(AgentProxy agent) {
        StandardEntity agentEntity = (StandardEntity) agent.getControlledEntity();
        Logger.debug("Finding visible braveCircles.tools.entities for " + agentEntity);
        ChangeSet result = new ChangeSet();
        // Look for objects within range
        Pair<Integer, Integer> location = agentEntity.getLocation(world);
        if (location != null) {
            Point2D point = new Point2D(location.first(), location.second());
            Collection<StandardEntity> nearby = world.getObjectsInRange(location.first(), location.second(), viewDistance);
            Collection<StandardEntity> visible = findVisible(agentEntity, point, nearby);
            for (StandardEntity next : visible) {
                StandardEntityURN urn = next.getStandardURN();
                switch (urn) {
                    case ROAD:
                        addRoadProperties((Road) next, result);
                        break;
                    case BUILDING:
                    case REFUGE:
                    case FIRE_STATION:
                    case AMBULANCE_CENTRE:
                    case POLICE_OFFICE:
                        addBuildingProperties((Building) next, result);
                        break;
                    case CIVILIAN:
                    case FIRE_BRIGADE:
                    case AMBULANCE_TEAM:
                    case POLICE_FORCE:
                        // Always send all properties of the agent-controlled object
                        if (next == agentEntity) {
                            addSelfProperties((Human) next, result);
                        } else {
                            addHumanProperties((Human) next, result);
                        }
                        break;
                    case BLOCKADE:
                        addBlockadeProperties((Blockade) next, result);
                        break;
                    default:
                        // Ignore other types
                        break;
                }
            }
        }
        if (view != null) {
            view.repaint();
        }
        return result;
    }

    public List<MrlRay> getRays() {
        return rays;
    }

    private void addRoadProperties(Road road, ChangeSet result) {
        addAreaProperties(road, result);
        // Only update blockades
        result.addChange(road, road.getBlockadesProperty());
        // Also update each blockade
        if (road.isBlockadesDefined()) {
            for (EntityID id : road.getBlockades()) {
                Blockade blockade = (Blockade) world.getEntity(id);
                if (blockade == null) {
                    Logger.error("Blockade " + id + " is null!");
                    Logger.error(road.getFullDescription());
                } else {
                    addBlockadeProperties(blockade, result);
                }
            }
        }
    }

    private void addBuildingProperties(Building building, ChangeSet result) {
        addAreaProperties(building, result);
        // Update TEMPERATURE, FIERYNESS and BROKENNESS
        result.addChange(building, building.getTemperatureProperty());
        result.addChange(building, building.getFierynessProperty());
        result.addChange(building, building.getBrokennessProperty());
    }

    private void addAreaProperties(Area area, ChangeSet result) {
    }

    private void addFarBuildingProperties(Building building, ChangeSet result) {
        // Update FIERYNESS only
        result.addChange(building, building.getFierynessProperty());
    }

    private void addHumanProperties(Human human, ChangeSet result) {
        // Update POSITION, X, Y, DIRECTION, STAMINA, BURIEDNESS, HP, DAMAGE
        result.addChange(human, human.getPositionProperty());
        result.addChange(human, human.getXProperty());
        result.addChange(human, human.getYProperty());
        result.addChange(human, human.getDirectionProperty());
        result.addChange(human, human.getStaminaProperty());
        result.addChange(human, human.getBuriednessProperty());
        // Round HP and damage
        IntProperty hp = (IntProperty) human.getHPProperty().copy();
        roundProperty(hp, hpPrecision);
        result.addChange(human, hp);
        IntProperty damage = (IntProperty) human.getDamageProperty().copy();
        roundProperty(damage, damagePrecision);
        result.addChange(human, damage);
    }

    private void addSelfProperties(Human human, ChangeSet result) {
        // Update human properties and POSITION_HISTORY
        addHumanProperties(human, result);
        result.addChange(human, human.getPositionHistoryProperty());
        // Un-round hp and damage
        result.addChange(human, human.getHPProperty());
        result.addChange(human, human.getDamageProperty());
        if (human instanceof FireBrigade) {
            FireBrigade fb = (FireBrigade) human;
            result.addChange(fb, fb.getWaterProperty());
        }
    }

    private void addBlockadeProperties(Blockade blockade, ChangeSet result) {
        result.addChange(blockade, blockade.getXProperty());
        result.addChange(blockade, blockade.getYProperty());
        result.addChange(blockade, blockade.getPositionProperty());
        result.addChange(blockade, blockade.getApexesProperty());
        result.addChange(blockade, blockade.getRepairCostProperty());
    }

    private void roundProperty(IntProperty p, int precision) {
        if (precision != 1 && p.isDefined()) {
            p.setValue(round(p.getValue(), precision));
        }
    }

    private int round(int value, int precision) {
        int remainder = value % precision;
        value -= remainder;
        if (remainder >= precision / 2) {
            value += precision;
        }
        return value;
    }

    private Collection<StandardEntity> findVisibleAreas(Area area, Point2D location, Collection<StandardEntity> nearby) {
        Collection<LineInfo> lines = getAllLines(nearby);
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Collection<StandardEntity> result = new HashSet<StandardEntity>();
        rays.clear();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(viewDistance);
            Ray ray = new Ray(new Line2D(location, vector), lines);
            rays.add(new MrlRay(ray.getRay()));
            for (LineInfo hit : ray.getLinesHit()) {
                StandardEntity e = hit.getEntity();
                result.add(e);
            }
        }
        return result;
    }

    private Collection<StandardEntity> findVisible(StandardEntity agentEntity, Point2D location, Collection<StandardEntity> nearby) {
        Logger.debug("Finding visible braveCircles.tools.entities from " + location);
        Logger.debug(nearby.size() + " nearby braveCircles.tools.entities");
        Collection<LineInfo> lines = getAllLines(nearby);
        // Cast rays
        // CHECKSTYLE:OFF:MagicNumber
        double dAngle = Math.PI * 2 / rayCount;
        // CHECKSTYLE:ON:MagicNumber
        Collection<StandardEntity> result = new HashSet<StandardEntity>();
        for (int i = 0; i < rayCount; ++i) {
            double angle = i * dAngle;
            Vector2D vector = new Vector2D(Math.sin(angle), Math.cos(angle)).scale(viewDistance);
            Ray ray = new Ray(new Line2D(location, vector), lines);

            for (LineInfo hit : ray.getLinesHit()) {
                StandardEntity e = hit.getEntity();
                result.add(e);
            }
            if (view != null) {
                view.addRay(agentEntity, ray);
            }
        }
        // Now look for humans
        for (StandardEntity next : nearby) {
            if (next instanceof Human) {
                Human h = (Human) next;
                if (canSee(agentEntity, location, h, lines)) {
                    result.add(h);
                }
            }
        }
        // Add self
        result.add(agentEntity);
        Logger.debug(agentEntity + " can see " + result);
        return result;
    }

    private boolean canSee(StandardEntity agent, Point2D location, Human h, Collection<LineInfo> lines) {
        if (h.isXDefined() && h.isYDefined()) {
            int x = h.getX();
            int y = h.getY();
            Point2D humanLocation = new Point2D(x, y);
            Ray ray = new Ray(new Line2D(location, humanLocation), lines);
            if (ray.getVisibleLength() >= 1) {
                if (view != null) {
                    view.addRay(agent, ray);
                }
                return true;
            }
        } else if (h.isPositionDefined()) {
            if (h.getPosition().equals(agent.getID())) {
                return true;
            }
            Entity e = world.getEntity(h.getPosition());
            if (e instanceof AmbulanceTeam) {
                return canSee(agent, location, (Human) e, lines);
            }
        }
        return false;
    }

    private Collection<LineInfo> getAllLines(Collection<StandardEntity> entities) {
        Collection<LineInfo> result = new HashSet<LineInfo>();
        for (StandardEntity next : entities) {
            if (next instanceof Building) {
                for (Edge edge : ((Building) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, !edge.isPassable()));
                }
            }
            if (next instanceof Road) {
                for (Edge edge : ((Road) next).getEdges()) {
                    Line2D line = edge.getLine();
                    result.add(new LineInfo(line, next, false));
                }
            } else if (next instanceof Blockade) {
                int[] apexes = ((Blockade) next).getApexes();
                List<Point2D> points = GeometryTools2D.vertexArrayToPoints(apexes);
                List<Line2D> lines = GeometryTools2D.pointsToLines(points, true);
                for (Line2D line : lines) {
                    result.add(new LineInfo(line, next, false));
                }
            } else {
                continue;
            }
        }
        return result;
    }

    private class LOSView extends JPanel {
        private transient StandardWorldModelViewer viewer;
        private transient Collection<Ray> rays;
        private transient Map<StandardEntity, Collection<Ray>> sources;
        private transient StandardEntity selected;

        public LOSView() {
            super(new BorderLayout());
            viewer = new StandardWorldModelViewer();
            viewer.removeAllLayers();
            viewer.addLayer(new BuildingLayer());
            viewer.addLayer(new RoadLayer());
            viewer.addLayer(new RoadBlockageLayer());
            viewer.addLayer(new HumanLayer());
            viewer.addLayer(new RayLayer());
            rays = new ArrayList<Ray>();
            sources = new LazyMap<StandardEntity, Collection<Ray>>() {
                @Override
                public Collection<Ray> createValue() {
                    return new HashSet<Ray>();
                }
            };
            selected = null;
            viewer.addViewListener(new ViewListener() {
                @Override
                public void objectsClicked(ViewComponent v, List<RenderedObject> objects) {
                    selected = null;
                    for (RenderedObject o : objects) {
                        if (o.getObject() instanceof Human) {
                            selected = (StandardEntity) o.getObject();
                            viewer.repaint();
                        }
                    }
                }

                @Override
                public void objectsRollover(ViewComponent v, List<RenderedObject> objects) {
                }
            });
            add(viewer, BorderLayout.CENTER);
        }

        public void clear() {
            synchronized (rays) {
                rays.clear();
                sources.clear();
            }
        }

        public void addRay(StandardEntity source, Ray ray) {
            synchronized (rays) {
                rays.add(ray);
                sources.get(source).add(ray);
            }
        }

        public void refresh() {
            viewer.view(world);
        }

        private class RayLayer extends StandardViewLayer {
            @Override
            public Collection<RenderedObject> render(Graphics2D g, ScreenTransform transform, int width, int height) {
                Collection<Ray> toDraw = new HashSet<Ray>();
                synchronized (rays) {
                    if (selected == null) {
                        toDraw.addAll(rays);
                    } else {
                        toDraw.addAll(sources.get(selected));
                    }
                }
                g.setColor(Color.CYAN);
                for (Ray next : toDraw) {
                    Line2D line = next.getRay();
                    Point2D origin = line.getOrigin();
                    Point2D end = line.getPoint(next.getVisibleLength());
                    int x1 = transform.xToScreen(origin.getX());
                    int y1 = transform.yToScreen(origin.getY());
                    int x2 = transform.xToScreen(end.getX());
                    int y2 = transform.yToScreen(end.getY());
                    g.drawLine(x1, y1, x2, y2);
                }
                return new ArrayList<RenderedObject>();
            }

            @Override
            public String getName() {
                return "Line of sight rays";
            }
        }
    }
}