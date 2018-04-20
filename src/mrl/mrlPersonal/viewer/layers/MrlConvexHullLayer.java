package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import math.geom2d.conic.Circle2D;
import mrl.common.ConvexHull;
import mrl.common.clustering.Cluster;
import mrl.common.clustering.ConvexObject;
import mrl.common.clustering.FireCluster;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.DirectionObject;
import mrl.world.object.MrlBuilding;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sajjadsalehi
 * Date: 12/16/11
 * Time: 6:59 PM
 * Edited by Mostafa Shabani
 * Rided by Vahid hooshangi
 * Edited by Mostafa Movahedi
 */
public class MrlConvexHullLayer extends MrlPreRoutingPartitionsLayer {
    private static final Color ConColour = Color.CYAN;
    private static final Color ConDyingColour = Color.BLACK;
    private static final Color ConExpandableColour = Color.MAGENTA;
    private static final Color BORDER_DIRECTION_ENTIY_Colour = Color.orange;
    private static final Color HIGH_VALUE_BUILDINGS_Colour = new Color(128, 0, 122);
    private static final Color LOW_VALUE_BUILDINGS_Colour = new Color(227, 177, 221);
    private static final Color EDGE_BUILDINGS_Colour = new Color(227, 227, 227);
    private static final Color BORDER_ENTIY_Colour = Color.WHITE;
    private static final Color IGNORED_BORDER_ENTIY_Colour = Color.cyan;
    private static final Color BORDER_MAP_Colour = Color.RED;
    private static final Color TARGET_COLOR = Color.PINK;
    private static final Color STAND_NEAR_TARGET = new Color(Color.BLUE.getRed(), Color.BLUE.getBlue(), Color.BLUE.getGreen(), 100);
    private static final Color SMALL_BORDER_COLOUR = Color.magenta;
    private static final Color BIG_BORDER_COLOUR = Color.yellow;
    private static final Stroke STROKE = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke STROKE_DIRECTION = new BasicStroke(5, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    public static final Map<EntityID, List<FireCluster>> CONVEX_HULLS_MAP = Collections.synchronizedMap(new FastMap<EntityID, List<FireCluster>>());

    public static final Map<EntityID, List<Pair<Point2D, String>>> FIRE_CLUSTER_CONDITIONS = new FastMap<EntityID, List<Pair<Point2D, String>>>();

    public static Map<EntityID, Pair<Point, ConvexObject>> CENTER_POINT = Collections.synchronizedMap(new FastMap<EntityID, Pair<Point, ConvexObject>>());
    public static Map<EntityID, StandardEntity> BUILDINGS = new FastMap<EntityID, StandardEntity>();//TODO :just for test
    public static List<Polygon> BIG_BORDER_HULLS = new ArrayList<Polygon>();
    public static Map<EntityID, List<StandardEntity>> IGNORED_BORDER_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, List<StandardEntity>>());
    public static Map<EntityID, List<StandardEntity>> BORDER_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, List<StandardEntity>>());
    public static Map<EntityID, List<StandardEntity>> BORDER_DIRECTION_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, List<StandardEntity>>());
    public static Map<EntityID, List<MrlBuilding>> HIGH_VALUE_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, List<MrlBuilding>>());
    public static Map<EntityID, List<MrlBuilding>> LOW_VALUE_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, List<MrlBuilding>>());
    public static Map<EntityID, List<MrlBuilding>> EDGE_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, List<MrlBuilding>>());
    public static Map<EntityID, Set<StandardEntity>> BEST_PLACE_TO_STAND = Collections.synchronizedMap(new FastMap<EntityID, Set<StandardEntity>>());
    public static Map<EntityID, List<Building>> MY_TARGET = Collections.synchronizedMap(new FastMap<EntityID, List<Building>>());
    public static Map<EntityID, List<StandardEntity>> STAND = Collections.synchronizedMap(new FastMap<EntityID, List<StandardEntity>>());
    public static Map<EntityID, Set<MrlBuilding>> EXPLORE_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, Set<MrlBuilding>>());
    public static Map<EntityID, Set<Area>> EXPLORE_POSITIONS = Collections.synchronizedMap(new FastMap<EntityID, Set<Area>>());
    public static Map<EntityID, Area> EXPLORE_TARGET = Collections.synchronizedMap(new FastMap<EntityID, Area>());
    public static Map<EntityID, List<DirectionObject>> DIRECTION_POLYGONS = Collections.synchronizedMap(new FastMap<EntityID, List<DirectionObject>>());
    public static int EXTINGUISH_RANGE;

    public static Map<EntityID, Set<EntityID>> BORDER_MAP_BUILDINGS = Collections.synchronizedMap(new FastMap<EntityID, Set<EntityID>>());
    public static ConvexHull BIG_Whole_BORDER_HULL = new ConvexHull();
    public static Polygon SMALL_Whole_BORDER_HULL = new Polygon();

    public static List<Polygon> SMALL_BORDER_HULLS = new ArrayList<Polygon>();

    private boolean fireClusterConditionInfo;
    private FireClusterConditionAction fireClusterConditionAction;

    private boolean ignoredBorderEntitiesInfo;
    private IgnoredBorderEntitiesAction ignoredBorderEntitiesAction;

    private boolean borderEntitiesInfo;
    private BorderEntitiesAction borderEntitiesAction;

    private boolean borderDirectionsInfo;
    private BorderDirectionEntitiesAction borderDirectionEntitiesAction;

    private boolean highValueBuildingsInfo;
    private HighValueBuildingsAction highValueBuildingsAction;

    private boolean lowValueBuildingsInfo;
    private LowValueBuildingsAction lowValueBuildingsAction;

    private boolean edgeBuildingsInfo;
    private EdgeBuildingsAction edgeBuildingsAction;

    private boolean borderHullsInfo;
    private BorderHullsAction borderHullsAction;

    private boolean othersInfo;
    private OthersAction othersAction;

    private boolean borderMapBuildingsInfo;
    private BorderMapBuildingsAction borderMapBuildingsAction;

    private boolean showTargetInfo;
    private ShowTargetAction showTargetAction;

    private boolean showAgentInThisCluster;
    private ShowAgentInThisClusterAction showAgentInThisClusterAction;

    private boolean showExploreData;
    private ShowExploreDataAction showExploreDataAction;

    private boolean showNewDirectionData;
    private ShowNewDirectionDataAction showNewDirectionDataAction;


    public MrlConvexHullLayer() {
        fireClusterConditionInfo = true;
        fireClusterConditionAction = new FireClusterConditionAction();
        ignoredBorderEntitiesInfo = true;
        ignoredBorderEntitiesAction = new IgnoredBorderEntitiesAction();
        borderEntitiesInfo = false;
        borderEntitiesAction = new BorderEntitiesAction();
        borderHullsInfo = false;
        borderHullsAction = new BorderHullsAction();
        borderDirectionsInfo = true;
        borderDirectionEntitiesAction = new BorderDirectionEntitiesAction();
        highValueBuildingsInfo = true;
        highValueBuildingsAction = new HighValueBuildingsAction();
        lowValueBuildingsInfo = true;
        lowValueBuildingsAction = new LowValueBuildingsAction();
        edgeBuildingsInfo = true;
        edgeBuildingsAction = new EdgeBuildingsAction();
        othersInfo = false;
        othersAction = new OthersAction();
        borderMapBuildingsInfo = false;
        borderMapBuildingsAction = new BorderMapBuildingsAction();
        showTargetInfo = false;
        showTargetAction = new ShowTargetAction();
        showAgentInThisCluster = false;
        showAgentInThisClusterAction = new ShowAgentInThisClusterAction();
        showExploreData = false;
        showExploreDataAction = new ShowExploreDataAction();
        showNewDirectionData = false;
        showNewDirectionDataAction = new ShowNewDirectionDataAction();
    }

    @Override
    public String getName() {
        return "Convex Hull";
    }


    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(fireClusterConditionAction));
        result.add(new JMenuItem(borderEntitiesAction));
        result.add(new JMenuItem(ignoredBorderEntitiesAction));
        result.add(new JMenuItem(borderHullsAction));
        result.add(new JMenuItem(othersAction));
        result.add(new JMenuItem(showNewDirectionDataAction));
        result.add(new JMenuItem(borderDirectionEntitiesAction));
        result.add(new JMenuItem(highValueBuildingsAction));
        result.add(new JMenuItem(lowValueBuildingsAction));
        result.add(new JMenuItem(edgeBuildingsAction));
        result.add(new JMenuItem(borderMapBuildingsAction));
        result.add(new JMenuItem(showTargetAction));
        result.add(new JMenuItem(showAgentInThisClusterAction));
        result.add(new JMenuItem(showExploreDataAction));
        return result;
    }

    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {

        for (StandardEntity entity : world.getEntitiesOfType(StandardEntityURN.BUILDING)) {
            BUILDINGS.put(entity.getID(), entity);
        }

        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();


        List<Pair<Point2D, String>> fireClusterConditions = new ArrayList<Pair<Point2D, String>>();
        List<FireCluster> clusters = new ArrayList<FireCluster>();
        List<DirectionObject> directionPolygons = new FastList<DirectionObject>();
        List<Polygon> smallPolygons = new ArrayList<Polygon>();
        List<Polygon> bigPolygons = new ArrayList<Polygon>();
        List<StandardEntity> ignoredBorderBuildings = new ArrayList<StandardEntity>();
        List<StandardEntity> borderBuildings = new ArrayList<StandardEntity>();
        List<StandardEntity> borderDirectionBuildings = new ArrayList<StandardEntity>();
        List<MrlBuilding> highValueBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> lowValueBuildings = new ArrayList<MrlBuilding>();
        List<MrlBuilding> edgeBuildings = new ArrayList<MrlBuilding>();
        Set<EntityID> borderMapBuildings = new FastSet<EntityID>();
        List<Building> target = new ArrayList<Building>();
        Set<StandardEntity> pathToFire = new HashSet<StandardEntity>();
        List<StandardEntity> standEntity = new ArrayList<StandardEntity>();
        //Building target;
//        ConvexHull borderMapConvex = new ConvexHull();

        if (StaticViewProperties.selectedObject != null && CONVEX_HULLS_MAP.get(StaticViewProperties.selectedObject.getID()) != null) {
            try {
                if (FIRE_CLUSTER_CONDITIONS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    fireClusterConditions = Collections.synchronizedList(FIRE_CLUSTER_CONDITIONS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (CONVEX_HULLS_MAP.get(StaticViewProperties.selectedObject.getID()) != null) {
                    clusters = Collections.synchronizedList(CONVEX_HULLS_MAP.get(StaticViewProperties.selectedObject.getID()));
                }
                if (DIRECTION_POLYGONS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    directionPolygons = Collections.synchronizedList(DIRECTION_POLYGONS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (SMALL_BORDER_HULLS != null) {
                    smallPolygons = Collections.synchronizedList(SMALL_BORDER_HULLS);
                }
                if (BIG_BORDER_HULLS != null) {
                    bigPolygons = Collections.synchronizedList(BIG_BORDER_HULLS);
                }
                if (BORDER_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    borderBuildings = Collections.synchronizedList(BORDER_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (IGNORED_BORDER_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    ignoredBorderBuildings = Collections.synchronizedList(IGNORED_BORDER_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (BORDER_DIRECTION_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    borderDirectionBuildings = Collections.synchronizedList(BORDER_DIRECTION_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (HIGH_VALUE_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    highValueBuildings = Collections.synchronizedList(HIGH_VALUE_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (LOW_VALUE_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    lowValueBuildings = Collections.synchronizedList(LOW_VALUE_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (EDGE_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    edgeBuildings = Collections.synchronizedList(EDGE_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (BORDER_MAP_BUILDINGS.get(StaticViewProperties.selectedObject.getID()) != null) {
                    borderMapBuildings = Collections.synchronizedSet(BORDER_MAP_BUILDINGS.get(StaticViewProperties.selectedObject.getID()));
                }
                if (MY_TARGET.get(StaticViewProperties.selectedObject.getID()) != null) {
                    target = Collections.synchronizedList(MY_TARGET.get(StaticViewProperties.selectedObject.getID()));
                }
                if (BEST_PLACE_TO_STAND.get(StaticViewProperties.selectedObject.getID()) != null) {
                    pathToFire = Collections.synchronizedSet(BEST_PLACE_TO_STAND.get(StaticViewProperties.selectedObject.getID()));
                }
                if (STAND.get(StaticViewProperties.selectedObject.getID()) != null) {
                    standEntity = Collections.synchronizedList(STAND.get(StaticViewProperties.selectedObject.getID()));
                }
                //borderMapConvex = Collections.synchronizedMap(BORDER_HULL.get(StaticViewProperties.selectedObject.getID()));
            } catch (NullPointerException ignored) {
                ignored.printStackTrace();
            }

            if (showNewDirectionData) {
                RenderNewDirectionAction(g, t, directionPolygons);
            }
            if (borderEntitiesInfo) {
                RenderBorderEntitiesAction(g, t, borderBuildings);
            }
            if (ignoredBorderEntitiesInfo) {
                RenderIgnoredBorderEntitiesAction(g, t, ignoredBorderBuildings);
            }

            if (borderHullsInfo) {
                RenderBorderHullsAction(g, t, smallPolygons, bigPolygons);
            }

            if (borderMapBuildingsInfo) {
                renderBorderMapBuildings(g, t, borderMapBuildings);
            }

            if (borderDirectionsInfo) {
                RenderBorderDirectionEntitiesAction(g, t, borderDirectionBuildings);
            }

            if (highValueBuildingsInfo) {
                RenderBuildingsAction(g, t, highValueBuildings, HIGH_VALUE_BUILDINGS_Colour);
            }
            if (lowValueBuildingsInfo) {
                RenderBuildingsAction(g, t, lowValueBuildings, LOW_VALUE_BUILDINGS_Colour);
            }
            if (edgeBuildingsInfo) {
                RenderBuildingsAction(g, t, edgeBuildings, EDGE_BUILDINGS_Colour);
            }
            if (showTargetInfo) {
//            if (!target.isEmpty() && !standEntity.isEmpty()) {
                renderTarget(g, t, target/*.get(target.size()-1)*/, standEntity/*.get(standEntity.size()-1)*/, pathToFire);
//            }
            }
            if (fireClusterConditionInfo) {
                RenderFireClusterConditionAction(g, t, fireClusterConditions);
            }

            if (othersInfo) {

                Pair<Point, ConvexObject> pair = CENTER_POINT.get(StaticViewProperties.selectedObject.getID());
                if (pair != null) {

                    RenderOthers(g, t, pair.first(), pair.second());
                }
            }

            if (showExploreData) {
                renderExploreData(g, t, StaticViewProperties.selectedObject.getID());
            }

            if (clusters != null && !clusters.isEmpty()) {
                for (FireCluster cluster : clusters) {
                    xs.clear();
                    ys.clear();

                    for (int x : cluster.getConvexHullObject().getConvexPolygon().xpoints) {
                        xs.add(t.xToScreen(x));
                    }
                    for (int y : cluster.getConvexHullObject().getConvexPolygon().ypoints) {
                        ys.add(t.yToScreen(y));
                    }

                    Polygon poly = new Polygon(listToArray(xs), listToArray(ys), cluster.getConvexHullObject().getConvexPolygon().npoints);
                    g.setColor(ConColour);
                    if (cluster.isDying()) {
                        g.setColor(ConDyingColour);
                    }
                    if (!cluster.isExpandableToCenterOfMap()) {
                        g.setColor(ConExpandableColour);
                    }
                    g.setStroke(STROKE);
                    g.draw(poly);

                    g.drawString(String.valueOf(cluster.getId()), (int) poly.getBounds2D().getCenterX(), (int) poly.getBounds2D().getCenterY());

//                    if (othersInfo) {
//                        RenderOthers(g, t, cluster.getConvexHullObject());
//                    }
                    if (showAgentInThisCluster) {
                        Polygon pp = cluster.getConvexHullObject().getConvexPolygon();
                        double x = Math.min(pp.getBounds2D().getWidth(), pp.getBounds2D().getHeight());
                        double scale = (x + EXTINGUISH_RANGE + (EXTINGUISH_RANGE / 3)) / x;
                        Polygon bigPolyForAgents = Cluster.scalePolygon(poly, scale);
                        g.setColor(new Color(200, 150, 200));
                        g.draw(bigPolyForAgents);
                    }
                }
            }
        }
        return list;
    }

    private void renderExploreData(Graphics2D g, ScreenTransform t, EntityID agent) {
        Set<Area> areas = EXPLORE_POSITIONS.get(agent);
        Area target = EXPLORE_TARGET.get(agent);
        Set<MrlBuilding> buildings = EXPLORE_BUILDINGS.get(agent);
        if (areas == null || buildings == null) {
            return;
        }
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Polygon poly;
        g.setColor(new Color(150, 190, 135));
        for (MrlBuilding building : buildings) {
            xs.clear();
            ys.clear();
            for (int i = 0; i < building.getSelfBuilding().getApexList().length; i += 2) {
                xs.add(t.xToScreen(building.getSelfBuilding().getApexList()[i]));
                ys.add(t.yToScreen(building.getSelfBuilding().getApexList()[i + 1]));
            }
            poly = new Polygon(listToArray(xs), listToArray(ys), building.getSelfBuilding().getApexList().length / 2);
            g.draw(poly);
        }
        g.setColor(new Color(180, 0, 255));
        for (Area area : areas) {
            g.drawOval(t.xToScreen(area.getX()), t.yToScreen(area.getY()), 4, 4);
        }
        Pair<Integer, Integer> location = world.getEntity(agent).getLocation(world);
        if (target != null) {
            g.drawLine(t.xToScreen(location.first()), t.yToScreen(location.second()), t.xToScreen(target.getX()), t.yToScreen(target.getY()));
        }
    }

    public void renderTarget(Graphics2D g, ScreenTransform t, List<Building> targetList, List<StandardEntity> standList, Set<StandardEntity> stations) {
        g.setStroke(STROKE);
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Polygon poly;
/*

        Building building;
        Road road;
        g.setColor(STAND_NEAR_TARGET);

        for (StandardEntity entity : stations) {
            xs.clear();
            ys.clear();

            if (entity instanceof Building) {
                building = (Building) entity;
                for (int i = 0; i < building.getApexList().length; i += 2) {
                    xs.add(t.xToScreen(building.getApexList()[i]));
                    ys.add(t.yToScreen(building.getApexList()[i + 1]));
                }
                poly = new Polygon(listToArray(xs), listToArray(ys), building.getApexList().length / 2);

                g.fill(poly);
            } else if (entity instanceof Road) {
                road = (Road) entity;
                for (int i = 0; i < road.getApexList().length; i += 2) {
                    xs.add(t.xToScreen(road.getApexList()[i]));
                    ys.add(t.yToScreen(road.getApexList()[i + 1]));
                }
                poly = new Polygon(listToArray(xs), listToArray(ys), road.getApexList().length / 2);

                g.fill(poly);
            }
        }

        xs.clear();
        ys.clear();
        if (!standList.isEmpty()) {
            StandardEntity stand = standList.get(standList.size() - 1);
            g.setColor(Color.YELLOW);
            if (stand != null) {
                if (stand instanceof Road) {
                    Road rd = (Road) stand;
                    for (int i = 0; i < rd.getApexList().length; i += 2) {
                        xs.add(t.xToScreen(rd.getApexList()[i]));
                        ys.add(t.yToScreen(rd.getApexList()[i + 1]));
                    }
                    poly = new Polygon(listToArray(xs), listToArray(ys), rd.getApexList().length / 2);
                    g.fill(poly);
                } else if (stand instanceof Building) {
                    Building bl = (Building) stand;
                    for (int i = 0; i < bl.getApexList().length; i += 2) {
                        xs.add(t.xToScreen(bl.getApexList()[i]));
                        ys.add(t.yToScreen(bl.getApexList()[i + 1]));
                    }
                    poly = new Polygon(listToArray(xs), listToArray(ys), bl.getApexList().length / 2);
                    g.fill(poly);
                }
            }
        }
*/

        xs.clear();
        ys.clear();
        g.setColor(TARGET_COLOR);
        if (!targetList.isEmpty()) {
            Building target = targetList.get(targetList.size() - 1);
            if (target != null) {
                for (int i = 0; i < target.getApexList().length; i += 2) {
                    xs.add(t.xToScreen(target.getApexList()[i]));
                    ys.add(t.yToScreen(target.getApexList()[i + 1]));
                }
                poly = new Polygon(listToArray(xs), listToArray(ys), target.getApexList().length / 2);
                g.fill(poly);
                int d = t.xToScreen(target.getX() + EXTINGUISH_RANGE) - t.xToScreen(target.getX());
                Circle2D circle2D = new Circle2D(t.xToScreen(target.getX()), t.yToScreen(target.getY()), d, true);
                circle2D.draw(g);
            }
        }
    }

    private void RenderNewDirectionAction(Graphics2D g, ScreenTransform t, List<DirectionObject> directions) {
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        for (DirectionObject directionObject : directions) {
            Polygon polygon = directionObject.getPolygon();
            xs.clear();
            ys.clear();

            for (int x : polygon.xpoints) {
                xs.add(t.xToScreen(x));
            }
            for (int y : polygon.ypoints) {
                ys.add(t.yToScreen(y));
            }

            Polygon poly = new Polygon(listToArray(xs), listToArray(ys), polygon.npoints);
            g.setStroke(STROKE);
            g.setColor(new Color(127, 172, 217));
            g.draw(poly);
            g.setColor(new Color(120, 250, 170));
            Font font = g.getFont();
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(directionObject.getSide() + " - refs" + directionObject.getRefugeNo() + " - gass" + directionObject.getGasStationNo()
                    , (int) poly.getBounds2D().getCenterX(), (int) poly.getBounds2D().getCenterY());
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString(String.valueOf(directionObject.getValue().intValue()), (int) poly.getBounds2D().getCenterX(), (int) poly.getBounds2D().getCenterY() + 17);
            g.setFont(font);
        }

    }

    public void renderBorderMapBuildings(Graphics2D g, ScreenTransform t, Set<EntityID> borderMapBuildings) {
        Building building;
        g.setColor(BORDER_MAP_Colour);
        g.setStroke(STROKE_DIRECTION);
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Polygon poly;

        poly = BIG_Whole_BORDER_HULL.convex();
        g.draw(poly);

        poly = SMALL_Whole_BORDER_HULL;
        g.draw(poly);

        for (EntityID entityID : borderMapBuildings) {
            StandardEntity entity = world.getEntity(entityID);
            building = (Building) entity;

            xs.clear();
            ys.clear();

            for (int i = 0; i < building.getApexList().length; i += 2) {
                xs.add(t.xToScreen(building.getApexList()[i]));
                ys.add(t.yToScreen(building.getApexList()[i + 1]));
            }
            poly = new Polygon(listToArray(xs), listToArray(ys), building.getApexList().length / 2);

            g.draw(poly);
        }


    }

    public static int[] listToArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < list.size(); i++)
            array[i] = list.get(i);
        return array;
    }

    private void RenderFireClusterConditionAction(Graphics2D g, ScreenTransform t, List<Pair<Point2D, String>> conditions) {
        g.setColor(Color.WHITE);
        int x;
        int y;
        for (Pair<Point2D, String> condition : conditions) {
            Point2D point = condition.first();
            x = t.xToScreen(point.getX());
            y = t.yToScreen(point.getY());
            g.drawString(condition.second(), x, y);
        }
    }

    private void RenderBorderEntitiesAction(Graphics2D g, ScreenTransform t, List<StandardEntity> border_entities) {
        Building building;
        g.setColor(BORDER_ENTIY_Colour);
        g.setStroke(STROKE);
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        Polygon poly;
        for (StandardEntity entity : border_entities) {
            building = (Building) entity;

            xs.clear();
            ys.clear();

            for (int i = 0; i < building.getApexList().length; i += 2) {
                xs.add(t.xToScreen(building.getApexList()[i]));
                ys.add(t.yToScreen(building.getApexList()[i + 1]));
            }
            poly = new Polygon(listToArray(xs), listToArray(ys), building.getApexList().length / 2);

            g.draw(poly);
        }
    }

    private void RenderIgnoredBorderEntitiesAction(Graphics2D g, ScreenTransform t, List<StandardEntity> ignoredBorder_entities) {
        Building building;
        g.setColor(IGNORED_BORDER_ENTIY_Colour);
        g.setStroke(STROKE);
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        Polygon poly;
        for (StandardEntity entity : ignoredBorder_entities) {
            building = (Building) entity;

            xs.clear();
            ys.clear();

            for (int i = 0; i < building.getApexList().length; i += 2) {
                xs.add(t.xToScreen(building.getApexList()[i]));
                ys.add(t.yToScreen(building.getApexList()[i + 1]));
            }
            poly = new Polygon(listToArray(xs), listToArray(ys), building.getApexList().length / 2);

            g.fill(poly);
        }
    }

    private void RenderBorderDirectionEntitiesAction(Graphics2D g, ScreenTransform t, List<StandardEntity> border_direction_entities) {
        Building building;
        g.setColor(BORDER_DIRECTION_ENTIY_Colour);
        g.setStroke(STROKE_DIRECTION);
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Polygon poly;
        for (StandardEntity entity : border_direction_entities) {
            building = (Building) entity;

            xs.clear();
            ys.clear();

            for (int i = 0; i < building.getApexList().length; i += 2) {
                xs.add(t.xToScreen(building.getApexList()[i]));
                ys.add(t.yToScreen(building.getApexList()[i + 1]));
            }
            poly = new Polygon(listToArray(xs), listToArray(ys), building.getApexList().length / 2);

            g.draw(poly);
        }
    }

    private void RenderBuildingsAction(Graphics2D g, ScreenTransform t, List<MrlBuilding> buildings, Color color) {
        Building building;
        g.setColor(color);
        g.setStroke(STROKE_DIRECTION);
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        Polygon poly;
        for (MrlBuilding mrlBuilding : buildings) {
            building = mrlBuilding.getSelfBuilding();

            xs.clear();
            ys.clear();

            for (int i = 0; i < building.getApexList().length; i += 2) {
                xs.add(t.xToScreen(building.getApexList()[i]));
                ys.add(t.yToScreen(building.getApexList()[i + 1]));
            }
            poly = new Polygon(listToArray(xs), listToArray(ys), building.getApexList().length / 2);
//            g.fill(poly);
//            g.draw(poly);
        }
    }

    private void RenderOthers(Graphics2D g, ScreenTransform t, Point centerPoint, ConvexObject convexObject) {
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        if (convexObject.FIRST_POINT != null) {
            Set<Line2D> lines = Collections.synchronizedSet((convexObject.CONVEX_INTERSECT_LINES == null ? new FastSet<Line2D>() : convexObject.CONVEX_INTERSECT_LINES));
            if (lines != null) {
                g.setColor(Color.RED);
                g.setStroke(new BasicStroke(9));
                for (Line2D line : lines) {
                    g.drawLine(t.xToScreen(line.getX1()), t.yToScreen(line.getY1()), t.xToScreen(line.getX2()), t.yToScreen(line.getY2()));
                }
            }

            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(Color.WHITE);
            Circle2D circle2D = new Circle2D(t.xToScreen(centerPoint.x), t.yToScreen(centerPoint.y), 10, true);
            circle2D.draw(g);
            circle2D = new Circle2D(t.xToScreen(convexObject.CONVEX_POINT.x), t.yToScreen(convexObject.CONVEX_POINT.y), 5, true);
            circle2D.draw(g);
            g.drawLine(t.xToScreen(centerPoint.x), t.yToScreen(centerPoint.y), t.xToScreen(convexObject.CONVEX_POINT.x), t.yToScreen(convexObject.CONVEX_POINT.y));

            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(Color.GREEN);
            g.drawLine(t.xToScreen(convexObject.FIRST_POINT.x), t.yToScreen(convexObject.FIRST_POINT.y), t.xToScreen(convexObject.SECOND_POINT.x), t.yToScreen(convexObject.SECOND_POINT.y));

            if (convexObject.CONVEX_INTERSECT_POINTS != null) {
                for (Point2D point : convexObject.CONVEX_INTERSECT_POINTS) {
                    circle2D = new Circle2D(t.xToScreen(point.getX()), t.yToScreen(point.getY()), 7, true);
                    circle2D.draw(g);
                }
            }
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
            g.drawLine(t.xToScreen(convexObject.FIRST_POINT.x), t.yToScreen(convexObject.FIRST_POINT.y), t.xToScreen(convexObject.CONVEX_POINT.x), t.yToScreen(convexObject.CONVEX_POINT.y));
            g.drawLine(t.xToScreen(convexObject.SECOND_POINT.x), t.yToScreen(convexObject.SECOND_POINT.y), t.xToScreen(convexObject.CONVEX_POINT.x), t.yToScreen(convexObject.CONVEX_POINT.y));
            g.drawLine(t.xToScreen(convexObject.FIRST_POINT.x), t.yToScreen(convexObject.FIRST_POINT.y), t.xToScreen(convexObject.OTHER_POINT1.x), t.yToScreen(convexObject.OTHER_POINT1.y));
            g.drawLine(t.xToScreen(convexObject.SECOND_POINT.x), t.yToScreen(convexObject.SECOND_POINT.y), t.xToScreen(convexObject.OTHER_POINT2.x), t.yToScreen(convexObject.OTHER_POINT2.y));
            g.drawLine(t.xToScreen(convexObject.OTHER_POINT1.x), t.yToScreen(convexObject.OTHER_POINT1.y), t.xToScreen(convexObject.OTHER_POINT2.x), t.yToScreen(convexObject.OTHER_POINT2.y));
        }
        if (convexObject.DIRECTION_POLYGON != null) {
            Polygon polys = convexObject.DIRECTION_POLYGON;

            xs.clear();
            ys.clear();
            for (int x : polys.xpoints) {
                xs.add(t.xToScreen(x));
            }
            for (int y : polys.ypoints) {
                ys.add(t.yToScreen(y));
            }
            Polygon polygon = new Polygon(listToArray(xs), listToArray(ys), polys.npoints);

            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            g.setColor(Color.BLACK);
            /*g.draw(polygon);*/
        }
    }

    private void RenderBorderHullsAction(Graphics2D g, ScreenTransform t, List<Polygon> smallBorderHulls, List<Polygon> bigBorderHulls) {
        g.setStroke(STROKE);
        g.setColor(SMALL_BORDER_COLOUR);
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        List<Integer> xs = new ArrayList<Integer>();
        List<Integer> ys = new ArrayList<Integer>();
        for (Polygon polygon : smallBorderHulls) {
            xs.clear();
            ys.clear();

            for (int x : polygon.xpoints) {
                xs.add(t.xToScreen(x));
            }
            for (int y : polygon.ypoints) {
                ys.add(t.yToScreen(y));
            }

            Polygon poly = new Polygon(listToArray(xs), listToArray(ys), polygon.npoints);
            g.draw(poly);
        }

        g.setColor(BIG_BORDER_COLOUR);

        for (Polygon polygon : bigBorderHulls) {
            xs.clear();
            ys.clear();

            for (int x : polygon.xpoints) {
                xs.add(t.xToScreen(x));
            }
            for (int y : polygon.ypoints) {
                ys.add(t.yToScreen(y));
            }

            Polygon poly = new Polygon(listToArray(xs), listToArray(ys), polygon.npoints);
            g.draw(poly);
        }
    }

    private final class BorderEntitiesAction extends AbstractAction {
        public BorderEntitiesAction() {
            super("border entities");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setBorderEntitiesInfo(!borderEntitiesInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(borderEntitiesInfo));
            putValue(Action.SMALL_ICON, borderEntitiesInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setBorderEntitiesInfo(boolean render) {
        borderEntitiesInfo = render;
        borderEntitiesAction.update();
    }

    private final class IgnoredBorderEntitiesAction extends AbstractAction {
        public IgnoredBorderEntitiesAction() {
            super("ignored border entities");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setIgnoredBorderEntitiesInfo(!ignoredBorderEntitiesInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(ignoredBorderEntitiesInfo));
            putValue(Action.SMALL_ICON, ignoredBorderEntitiesInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setIgnoredBorderEntitiesInfo(boolean render) {
        ignoredBorderEntitiesInfo = render;
        ignoredBorderEntitiesAction.update();
    }

    private final class HighValueBuildingsAction extends AbstractAction {
        public HighValueBuildingsAction() {
            super("High Value Buildings");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setHighValueBuildingsInfo(!highValueBuildingsInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(highValueBuildingsInfo));
            putValue(Action.SMALL_ICON, highValueBuildingsInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setHighValueBuildingsInfo(boolean render) {
        highValueBuildingsInfo = render;
        highValueBuildingsAction.update();
    }


    private final class LowValueBuildingsAction extends AbstractAction {
        public LowValueBuildingsAction() {
            super("Low Value Buildings");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setLowValueBuildingsInfo(!lowValueBuildingsInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(lowValueBuildingsInfo));
            putValue(Action.SMALL_ICON, lowValueBuildingsInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setLowValueBuildingsInfo(boolean render) {
        lowValueBuildingsInfo = render;
        lowValueBuildingsAction.update();
    }


    private final class EdgeBuildingsAction extends AbstractAction {
        public EdgeBuildingsAction() {
            super("Edge Buildings");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setEdgeBuildingsInfo(!edgeBuildingsInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(edgeBuildingsInfo));
            putValue(Action.SMALL_ICON, edgeBuildingsInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setEdgeBuildingsInfo(boolean render) {
        edgeBuildingsInfo = render;
        edgeBuildingsAction.update();
    }


    private final class FireClusterConditionAction extends AbstractAction {

        public FireClusterConditionAction() {
            super("Fire Cluster Condition");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setFireClusterConditionInfo(!fireClusterConditionInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(fireClusterConditionInfo));
            putValue(Action.SMALL_ICON, fireClusterConditionInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setFireClusterConditionInfo(boolean render) {
        fireClusterConditionInfo = render;
        fireClusterConditionAction.update();
    }

    private final class BorderHullsAction extends AbstractAction {

        public BorderHullsAction() {
            super("border hulls");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setBorderHullsInfo(!borderHullsInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(borderHullsInfo));
            putValue(Action.SMALL_ICON, borderHullsInfo ? Icons.TICK : Icons.CROSS);
        }

    }

    public void setBorderHullsInfo(boolean render) {
        borderHullsInfo = render;
        borderHullsAction.update();
    }

    private final class OthersAction extends AbstractAction {

        public OthersAction() {
            super("Direction Info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setOthersInfo(!othersInfo);
            component.repaint();
        }

        public void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(othersInfo));
            putValue(Action.SMALL_ICON, othersInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setOthersInfo(boolean render) {
        othersInfo = render;
        othersAction.update();
    }

    private class BorderDirectionEntitiesAction extends AbstractAction {

        public BorderDirectionEntitiesAction() {
            super("Border Dir Info");
            update();
        }

        private void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(borderDirectionsInfo));
            putValue(Action.SMALL_ICON, borderDirectionsInfo ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setBorderDirectionsInfo(!borderDirectionsInfo);
            component.repaint();
        }
    }

    public void setBorderDirectionsInfo(boolean render) {
        borderDirectionsInfo = render;
        borderDirectionEntitiesAction.update();
    }

    private class BorderMapBuildingsAction extends AbstractAction {

        public BorderMapBuildingsAction() {
            super("Border Map Info");
            update();
        }

        public void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(borderMapBuildingsInfo));
            putValue(Action.SMALL_ICON, borderMapBuildingsInfo ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setBorderMapBuildingsInfo(!borderMapBuildingsInfo);
            component.repaint();
        }
    }

    public void setBorderMapBuildingsInfo(boolean render) {
        borderMapBuildingsInfo = render;
        borderMapBuildingsAction.update();
    }

    private class ShowTargetAction extends AbstractAction {

        public ShowTargetAction() {
            super("Show Target");
            update();
        }

        public void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showTargetInfo));
            putValue(Action.SMALL_ICON, showTargetInfo ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setShowTargetInfo(!showTargetInfo);
            component.repaint();
        }
    }

    public void setShowTargetInfo(boolean render) {
        showTargetInfo = render;
        showTargetAction.update();
    }

    private class ShowAgentInThisClusterAction extends AbstractAction {

        public ShowAgentInThisClusterAction() {
            super("Show cluster agents");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showAgentInThisCluster));
            putValue(Action.SMALL_ICON, showAgentInThisCluster ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showAgentInThisCluster = !showAgentInThisCluster;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showAgentInThisCluster));
            putValue(Action.SMALL_ICON, showAgentInThisCluster ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private class ShowExploreDataAction extends AbstractAction {

        public ShowExploreDataAction() {
            super("Explore data");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showExploreData));
            putValue(Action.SMALL_ICON, showExploreData ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showExploreData = !showExploreData;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showExploreData));
            putValue(Action.SMALL_ICON, showExploreData ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private class ShowNewDirectionDataAction extends AbstractAction {

        public ShowNewDirectionDataAction() {
            super("new Direction");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showNewDirectionData));
            putValue(Action.SMALL_ICON, showNewDirectionData ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showNewDirectionData = !showNewDirectionData;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showNewDirectionData));
            putValue(Action.SMALL_ICON, showNewDirectionData ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

}
