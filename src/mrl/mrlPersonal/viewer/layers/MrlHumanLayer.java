package mrl.mrlPersonal.viewer.layers;


import com.poths.rna.data.Point;
import javolution.util.FastMap;
import math.geom2d.conic.Circle2D;
import mrl.MrlPersonalData;
import mrl.ambulance.targetSelector.AmbulanceTarget;
import mrl.common.MRLConstants;
import mrl.common.Util;
import mrl.mrlPersonal.viewer.StandardEntityToPaint;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.partitioning.Partition;
import mrl.police.clear.GuideLine;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.*;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * Created by Mostafa Shabani.
 * Date: Dec 10, 2010
 * Time: 6:53:53 PM
 */
public class MrlHumanLayer extends StandardEntityViewLayer<Human> {
    private static org.apache.log4j.Logger Logger = org.apache.log4j.Logger.getLogger(MrlHumanLayer.class);

    public static Map<EntityID, Set<EntityID>> UNDEFINED_POSITION_CIVILIANS = new HashMap<EntityID, Set<EntityID>>();  //map(agentID => positionIDs)
    public static Map<EntityID, Set<EntityID>> DEFINED_POSITION_CIVILIANS = new HashMap<EntityID, Set<EntityID>>();    //map(agentID => positionIDs)
    public static Map<EntityID, GuideLine> AGENT_GUIDELINE_MAP = new HashMap<EntityID, GuideLine>();
    public static Map<EntityID, List<Line2D>> AGENT_CLEARLINES_MAP = new HashMap<EntityID, List<Line2D>>();

    public static Map<EntityID, Set<Pair<Integer, Integer>>> HEARD_POSITIONS = new HashMap<EntityID, Set<Pair<Integer, Integer>>>();
    private static final int SIZE = 10;
    private static int SAY_RANGE;
    private static int VIEW_RANGE;
    private static int CLEAR_RANGE;
    private static int AGENT_SIZE = 500;
    private static int EXTINGUISH_RANGE;

    private static final int HP_MAX = 10000;
    private static final int HP_INJURED = 7500;
    private static final int HP_CRITICAL = 1000;

    private static final String ICON_SIZE_KEY = "view.standard.human.icons.size";
    private static final String USE_ICONS_KEY = "view.standard.human.icons.use";
    private static final int DEFAULT_ICON_SIZE = 32;

    private static final HumanSorter HUMAN_SORTER = new HumanSorter();

    private static final Color CIVILIAN_COLOUR = Color.GREEN;
    private static final Color FIRE_BRIGADE_COLOUR = Color.RED;
    private static final Color POLICE_FORCE_COLOUR = Color.BLUE;
    private static final Color AMBULANCE_TEAM_COLOUR = Color.WHITE;
    private static final Color DEAD_COLOUR = Color.YELLOW.brighter();
    public static int CLEAR_RADIUS;
    public static final int DEFAULT_CLEAR_RADIUS = 2000;

    public static int TIME = 0;

    private static final Stroke STROKE = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke STROKE_DEFAULT = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

    private int iconSize;
    private Map<String, Map<State, Icon>> icons;
    private boolean useIcons;
    private Action useIconsAction;

    private boolean atInfo;
    private RenderATInfoAction atInfoAction;
    private boolean fbInfo;
    private RenderFBInfoAction fbInfoAction;
    private boolean pfInfo;
    private RenderPFInfoAction pfInfoAction;
    private boolean civInfo;
    public boolean showHeardPositions;
    public boolean showCivilianPosition;
    public boolean hideDeadCivilian;
    private RenderCivInfoAction civInfoAction;
    private boolean visibleAll;
    private VisibleAllInfoAction visibleAllInfoAction;
    private boolean lineOfSight;
    private LineOfSightAction lineOfSightAction;

    private boolean sayRange;
    private SayRangeAction sayRangeAction;

    private boolean viewRange;
    private ViewRangeAction viewRangeAction;

    private boolean clearRange;
    private ClearRangeAction clearRangeAction;

    private boolean agentSize;
    private AgentSizeAction agentSizeAction;

    private boolean agentLocation;
    private AgentLocationAction agentLocationAction;

    private boolean extinguishRange;
    private ExtinguishRangeAction extinguishRangeAction;

    private boolean rescueRegionRange;
    private RescueRegionRangeAction rescueRegionRangeAction;

    private boolean targetToGo;
    private TargetToGoAction targetToGoAction;

    private boolean allAmbulanceTargetsToGo;
    private AllAmbulanceTargetsToGoAction allAmbulanceTargetsToGoAction;

    private boolean allFireBrigadeTargetsToGo;
    private AllFireBrigadeTargetsToGoAction allFireBrigadeTargetsToGoAction;

    private boolean showPFGuideline;
    private PFGuidelineAction pfGuidelineAction;

    public static int RESCUE_RANGE;
    public static Map<EntityID, EntityID> TARGETS_TO_GO_MAP = Collections.synchronizedMap(new FastMap<EntityID, EntityID>());
    public static Map<EntityID, EntityID> AMBULANCE_TARGETS_TO_GO_MAP = Collections.synchronizedMap(new FastMap<EntityID, EntityID>());
    public static Map<EntityID, EntityID> FIRE_BRIGADE_TARGETS_TO_GO_MAP = Collections.synchronizedMap(new FastMap<EntityID, EntityID>());

    public static Map<EntityID, Map<EntityID,AmbulanceTarget>> AMBUULANCE_TARGET_MAP= Collections.synchronizedMap(new FastMap<>());



    /**
     * Construct a human view layer.
     */
    public MrlHumanLayer() {
        super(Human.class);
        iconSize = DEFAULT_ICON_SIZE;

    }

    public static void setTime(int time) {
        MrlHumanLayer.TIME = time;
    }

    @Override
    public void initialise(Config config) {
        iconSize = config.getIntValue(ICON_SIZE_KEY, DEFAULT_ICON_SIZE);
        icons = new FastMap<String, Map<State, Icon>>();
        useIcons = config.getBooleanValue(USE_ICONS_KEY, false);
        CLEAR_RADIUS = config.getIntValue(MRLConstants.CLEAR_RADIUS_KEY, DEFAULT_CLEAR_RADIUS);
        icons.put(StandardEntityURN.FIRE_BRIGADE.toString(), generateIconMap("FireBrigade"));
        icons.put(StandardEntityURN.AMBULANCE_TEAM.toString(), generateIconMap("AmbulanceTeam"));
        icons.put(StandardEntityURN.POLICE_FORCE.toString(), generateIconMap("PoliceForce"));
        icons.put(StandardEntityURN.CIVILIAN.toString() + "-Male", generateIconMap("Civilian-Male"));
        icons.put(StandardEntityURN.CIVILIAN.toString() + "-Female", generateIconMap("Civilian-Female"));
        useIconsAction = new UseIconsAction();

        atInfo = false;
        atInfoAction = new RenderATInfoAction();
        pfInfo = false;
        pfInfoAction = new RenderPFInfoAction();
        fbInfo = false;
        fbInfoAction = new RenderFBInfoAction();
        civInfo = false;
        civInfoAction = new RenderCivInfoAction();
        visibleAll = false;
        visibleAllInfoAction = new VisibleAllInfoAction();
        lineOfSight = false;
        lineOfSightAction = new LineOfSightAction();
        sayRange = false;
        sayRangeAction = new SayRangeAction();
        showHeardPositions = false;
        showHeardPositionAction = new ShowHeardPositionsAction();
        showCivilianPosition = false;
        civilianPositionAction = new CivilianPositionAction();
        hideDeadCivilian = false;
        deadCivilianAction = new DeadCivilianAction();
        viewRange = false;
        viewRangeAction = new ViewRangeAction();

        clearRange = false;
        clearRangeAction = new ClearRangeAction();

        agentSize = false;
        agentSizeAction = new AgentSizeAction();

        agentLocation = true;
        agentLocationAction = new AgentLocationAction();

        extinguishRange = false;
        extinguishRangeAction = new ExtinguishRangeAction();

        rescueRegionRange = false;
        rescueRegionRangeAction = new RescueRegionRangeAction();

        targetToGo = true;
        targetToGoAction = new TargetToGoAction();

        allAmbulanceTargetsToGo = false;
        allAmbulanceTargetsToGoAction = new AllAmbulanceTargetsToGoAction();

        allFireBrigadeTargetsToGo = true;
        allFireBrigadeTargetsToGoAction = new AllFireBrigadeTargetsToGoAction();

        showPFGuideline = true;
        pfGuidelineAction = new PFGuidelineAction();

        SAY_RANGE = config.getIntValue(MRLConstants.VOICE_RANGE_KEY);
        VIEW_RANGE = config.getIntValue(MRLConstants.MAX_VIEW_DISTANCE_KEY);
        CLEAR_RANGE = config.getIntValue(MRLConstants.MAX_CLEAR_DISTANCE_KEY);
        EXTINGUISH_RANGE = config.getIntValue(MRLConstants.MAX_EXTINGUISH_DISTANCE_KEY);
    }

    @Override
    public String getName() {
        return "Humans";
    }

    @Override
    public Shape render(Human h, Graphics2D g, ScreenTransform t) {
        // Don't draw humans in ambulances
        Pair<Integer, Integer> location = getLocation(h);
        if (location == null) {
            return null;
        }

        if (hideDeadCivilian) {
            if (h instanceof Civilian) {
                if (h.isHPDefined() && h.getHP() == 0) {
                    return null;
                }
            }
        }
        if (atInfo) {
            renderATAction(h, g, t, location);
        }
        if (fbInfo) {
            renderFBAction(h, g, t, location);
        }
        if (pfInfo) {
            renderPFAction(h, g, t, location);
        }
        if (civInfo) {
            renderCivAction(h, g, t, location);
        }

        if (h.isPositionDefined() && (world.getEntity(h.getPosition()) instanceof AmbulanceTeam)) {
            return null;
        }

        int x = t.xToScreen(location.first());
        int y = t.yToScreen(location.second());
        Shape shape;
        Icon icon = useIcons ? getIcon(h) : null;
        if (icon == null) {
            shape = new Ellipse2D.Double(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
            if (h == StaticViewProperties.selectedObject) {
                if (sayRange) {
                    g.setColor(Color.GREEN);
                    renderSayRange(g, t, h.getLocation(world));
                    g.setColor(Color.MAGENTA);
                }
                if (viewRange) {
                    g.setStroke(new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                    g.setColor(new Color(0, 128, 255));
                    renderViewRange(g, t, h.getLocation(world));
                    g.setStroke(STROKE_DEFAULT);
                    g.setColor(Color.MAGENTA);
                }
                if (agentSize) {
                    g.setColor(Color.YELLOW);
                    renderAgentSize(g, t, h.getLocation(world));
                    g.setColor(Color.MAGENTA);
                }
                if (clearRange && h instanceof PoliceForce) {
                    g.setColor(Color.blue);
                    renderClearRange(g, t, h.getLocation(world));
                    g.setColor(Color.MAGENTA);
                }
                if (extinguishRange && h instanceof FireBrigade) {
                    renderExtinguishRange(g, t, h.getLocation(world));
                }

                if (rescueRegionRange && h instanceof AmbulanceTeam) {
                    renderRescueRegionRange(g, t, h.getLocation(world));
                }

                if (showPFGuideline) {
                    if (StaticViewProperties.selectedObject != null) {
                        GuideLine guideLine = AGENT_GUIDELINE_MAP.get(StaticViewProperties.selectedObject.getID());
                        if (guideLine != null) {
                            int x1 = t.xToScreen(guideLine.getOrigin().getX());
                            int x2 = t.xToScreen(guideLine.getEndPoint().getX());
                            int y1 = t.yToScreen(guideLine.getOrigin().getY());
                            int y2 = t.yToScreen(guideLine.getEndPoint().getY());
                            g.setColor(Color.BLUE);
                            g.drawLine(x1, y1, x2, y2);
                            for(EntityID id : guideLine.getAreas()){
                                StandardEntity entity = world.getEntity(id);
                                if(entity instanceof Area){
                                    Area area = (Area) entity;

                                    List<Point> pointList = Util.getPointList(area.getApexList());
                                    for (int i =0 ; i < pointList.size()-1;i++){
                                        Point p1 = pointList.get(i);
                                        Point p2 = pointList.get(i+1);
                                        g.setColor(Color.YELLOW);
                                        g.drawLine(
                                                t.xToScreen(p1.getX()),
                                                t.yToScreen(p1.getY()),
                                                t.xToScreen(p2.getX()),
                                                t.yToScreen(p2.getY())
                                        );
                                    }


                                }
                            }

                        }

                        List<Line2D> clearLines = AGENT_CLEARLINES_MAP.get(StaticViewProperties.selectedObject.getID());
                        if (clearLines != null) {
                            for (Line2D line2D : clearLines) {
                                int x1 = t.xToScreen(line2D.getOrigin().getX());
                                int x2 = t.xToScreen(line2D.getEndPoint().getX());
                                int y1 = t.yToScreen(line2D.getOrigin().getY());
                                int y2 = t.yToScreen(line2D.getEndPoint().getY());
                                g.setColor(Color.WHITE);
                                g.drawLine(x1, y1, x2, y2);
                            }
                        }

                    }


                }


                if (targetToGo) {
                    renderTargetToGo(g, t, h.getLocation(world));
                }

                if (allAmbulanceTargetsToGo && h instanceof AmbulanceTeam) {
                    renderAllTargetsToGo(g, t, AMBULANCE_TARGETS_TO_GO_MAP);
                }

                if (allFireBrigadeTargetsToGo && h instanceof FireBrigade) {
                    renderAllTargetsToGo(g, t, FIRE_BRIGADE_TARGETS_TO_GO_MAP);
                }


                if (agentLocation) {
                    defaultCircle(h, g, t);
                }

                if (showHeardPositions) {
                    if (StaticViewProperties.selectedObject != null && StaticViewProperties.selectedObject instanceof Human) {
                        if (MrlHumanLayer.HEARD_POSITIONS.containsKey(StaticViewProperties.selectedObject.getID())) {
                            Set<Pair<Integer, Integer>> pairs = MrlHumanLayer.HEARD_POSITIONS.get(StaticViewProperties.selectedObject.getID());
                            for (Pair<Integer, Integer> pair : pairs) {
                                Circle2D circle2D = new Circle2D(t.xToScreen(pair.first()), t.yToScreen(pair.second()), 14d);
                                Color temp = g.getColor();
                                g.setColor(Color.WHITE);
                                circle2D.draw(g);
                                g.setColor(temp);
                            }
                        }
                    }
                }
            } else {
                g.setColor(adjustColour(getColour(h), h.getHP()));
            }
            g.fill(shape);
            if (showCivilianPosition) {
                if (StaticViewProperties.selectedObject != null &&
                        UNDEFINED_POSITION_CIVILIANS.containsKey(StaticViewProperties.selectedObject.getID()) &&
                        UNDEFINED_POSITION_CIVILIANS.get(StaticViewProperties.selectedObject.getID()).contains(h.getID())) {
                    Circle2D circle2D = new Circle2D(t.xToScreen(h.getX()), t.yToScreen(h.getY()), 13d, true);
                    g.setColor(Color.RED);
                    circle2D.draw(g);
                } else if (StaticViewProperties.selectedObject != null &&
                        DEFINED_POSITION_CIVILIANS.containsKey(StaticViewProperties.selectedObject.getID()) &&
                        DEFINED_POSITION_CIVILIANS.get(StaticViewProperties.selectedObject.getID()).contains(h.getID())) {
                    Circle2D circle2D = new Circle2D(t.xToScreen(h.getX()), t.yToScreen(h.getY()), 13d, true);
                    g.setColor(Color.GREEN);
                    circle2D.draw(g);
                }
            }


        } else {
            x -= icon.getIconWidth() / 2;
            y -= icon.getIconHeight() / 2;
            shape = new Rectangle2D.Double(x, y, icon.getIconWidth(), icon.getIconHeight());
            icon.paintIcon(null, g, x, y);
        }
        return shape;
    }

    private void renderTargetToGo(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {

        if (StaticViewProperties.selectedObject.getID() != null) {
            EntityID targetToGoID = TARGETS_TO_GO_MAP.get(StaticViewProperties.selectedObject.getID());
            if (targetToGoID != null) {
                StandardEntity targetEntity = world.getEntity(targetToGoID);
                int x1, y1, x2, y2;


                x1 = targetEntity.getLocation(world).first();
                y1 = targetEntity.getLocation(world).second();
                x2 = location.first();
                y2 = location.second();

                g.setStroke(STROKE);
                g.setColor(Color.YELLOW);
                g.drawLine(t.xToScreen(x1), t.yToScreen(y1), t.xToScreen(x2), t.yToScreen(y2));
                g.setStroke(STROKE_DEFAULT);
                g.setColor(Color.MAGENTA);

            }
        }

    }


    private void renderAllTargetsToGo(Graphics2D g, ScreenTransform t, Map<EntityID, EntityID> allTargets) {

        if (allTargets != null) {
            for (EntityID agentID : allTargets.keySet()) {
                EntityID targetToGoID = allTargets.get(agentID);
                if (targetToGoID != null) {
                    StandardEntity targetEntity = world.getEntity(targetToGoID);
                    StandardEntity agentEntity = world.getEntity(agentID);

                    int x1, y1, x2, y2;


                    x1 = targetEntity.getLocation(world).first();
                    y1 = targetEntity.getLocation(world).second();
                    x2 = agentEntity.getLocation(world).first();
                    y2 = agentEntity.getLocation(world).second();

//                    g.setStroke(STROKE);
                    g.setColor(Color.YELLOW);
                    g.drawLine(t.xToScreen(x1), t.yToScreen(y1), t.xToScreen(x2), t.yToScreen(y2));


                }
            }
        }
    }


    private void defaultCircle(Human h, Graphics2D g, ScreenTransform t) {
        g.setColor(Color.MAGENTA);
        Circle2D circle2D = new Circle2D(t.xToScreen(h.getX()), t.yToScreen(h.getY()), 18d, true);
        circle2D.draw(g);
    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();
        result.add(new JMenuItem(useIconsAction));
        result.add(new JMenuItem("   ----------INFO----------"));
        result.add(new JMenuItem(atInfoAction));
        result.add(new JMenuItem(fbInfoAction));
        result.add(new JMenuItem(pfInfoAction));
        result.add(new JMenuItem(civInfoAction));
        result.add(new JMenuItem(lineOfSightAction));
        result.add(new JMenuItem(sayRangeAction));
        result.add(new JMenuItem(showHeardPositionAction));
        result.add(new JMenuItem(civilianPositionAction));
        result.add(new JMenuItem(deadCivilianAction));
        result.add(new JMenuItem(viewRangeAction));
        result.add(new JMenuItem(clearRangeAction));
        result.add(new JMenuItem(agentLocationAction));
        result.add(new JMenuItem(agentSizeAction));
        result.add(new JMenuItem(visibleAllInfoAction));
        result.add(new JMenuItem(extinguishRangeAction));
        result.add(new JMenuItem(rescueRegionRangeAction));
        result.add(new JMenuItem(targetToGoAction));
        result.add(new JMenuItem(pfGuidelineAction));
        result.add(new JMenuItem(allAmbulanceTargetsToGoAction));
        result.add(new JMenuItem(allFireBrigadeTargetsToGoAction));

        return result;
    }

    @Override
    protected void postView() {
        Collections.sort(entities, HUMAN_SORTER);
    }

    /**
     * Get the location of a human.
     *
     * @param h The human to look up.
     * @return The location of the human.
     */
    protected Pair<Integer, Integer> getLocation(Human h) {
        return h.getLocation(world);
    }

    private Map<State, Icon> generateIconMap(String type) {
        Map<State, Icon> result = new EnumMap<State, Icon>(State.class);
        for (State state : State.values()) {
            String resourceName = "rescuecore2/standard/view/" + type + "-" + state.toString() + "-" + iconSize + "x" + iconSize + ".png";
            URL resource = MrlHumanLayer.class.getClassLoader().getResource(resourceName);
            if (resource == null) {
                Logger.warn("Couldn't find resource: " + resourceName);
            } else {
                result.put(state, new ImageIcon(resource));
            }
        }
        return result;
    }

    private Color getColour(Human h) {
        StandardEntityToPaint entityToPaint = StaticViewProperties.getPaintObject(h);
        if (entityToPaint != null) {
            return entityToPaint.getColor();
        }
//        if (h == StaticViewProperties.selectedObject)
//            return Color.MAGENTA;

        switch (h.getStandardURN()) {
            case CIVILIAN:
                return CIVILIAN_COLOUR;
            case FIRE_BRIGADE:
                return FIRE_BRIGADE_COLOUR;
            case AMBULANCE_TEAM:
                return AMBULANCE_TEAM_COLOUR;
            case POLICE_FORCE:
                return POLICE_FORCE_COLOUR;
            default:
                throw new IllegalArgumentException("Don't know how to draw humans of type " + h.getStandardURN());
        }
    }

    private Color adjustColour(Color c, int hp) {
        if (hp == 0) {
            return DEAD_COLOUR;
        }
        if (hp < HP_CRITICAL) {
            c = c.darker();
        }
        if (hp < HP_INJURED) {
            c = c.darker();
        }
        if (hp < HP_MAX) {
            c = c.darker();
        }
        return c;
    }

    private Icon getIcon(Human h) {
        State state = getState(h);
        Map<State, Icon> iconMap;
        switch (h.getStandardURN()) {
            case CIVILIAN:
                boolean male = h.getID().getValue() % 2 == 0;
                if (male) {
                    iconMap = icons.get(StandardEntityURN.CIVILIAN.toString() + "-Male");
                } else {
                    iconMap = icons.get(StandardEntityURN.CIVILIAN.toString() + "-Female");
                }
                break;
            default:
                iconMap = icons.get(h.getStandardURN().toString());
        }
        if (iconMap == null) {
            return null;
        }
        return iconMap.get(state);
    }

    private State getState(Human h) {
        int hp = h.getHP();
        if (hp <= 0) {
            return State.DEAD;
        }
        if (hp <= HP_CRITICAL) {
            return State.CRITICAL;
        }
        if (hp <= HP_INJURED) {
            return State.INJURED;
        }
        return State.HEALTHY;
    }

    private enum State {
        HEALTHY {
            @Override
            public String toString() {
                return "Healthy";
            }
        },
        INJURED {
            @Override
            public String toString() {
                return "Injured";
            }
        },
        CRITICAL {
            @Override
            public String toString() {
                return "Critical";
            }
        },
        DEAD {
            @Override
            public String toString() {
                return "Dead";
            }
        }
    }

    private Action showHeardPositionAction;

    private final class ShowHeardPositionsAction extends AbstractAction {
        public ShowHeardPositionsAction() {
            super("show heard positions");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showHeardPositions));
            putValue(Action.SMALL_ICON, showHeardPositions ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showHeardPositions = !showHeardPositions;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showHeardPositions));
            putValue(Action.SMALL_ICON, showHeardPositions ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private Action civilianPositionAction;

    private final class CivilianPositionAction extends AbstractAction {
        public CivilianPositionAction() {
            super("civilian position state");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showCivilianPosition));
            putValue(Action.SMALL_ICON, showCivilianPosition ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showCivilianPosition = !showCivilianPosition;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showCivilianPosition));
            putValue(Action.SMALL_ICON, showCivilianPosition ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private Action deadCivilianAction;

    private final class DeadCivilianAction extends AbstractAction {
        public DeadCivilianAction() {
            super("hide dead civilians");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(hideDeadCivilian));
            putValue(Action.SMALL_ICON, hideDeadCivilian ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            hideDeadCivilian = !hideDeadCivilian;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(hideDeadCivilian));
            putValue(Action.SMALL_ICON, hideDeadCivilian ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private static final class HumanSorter implements Comparator<Human>, java.io.Serializable {
        @Override
        public int compare(Human h1, Human h2) {
            if (h1 instanceof Civilian && !(h2 instanceof Civilian)) {
                return -1;
            }
            if (h2 instanceof Civilian && !(h1 instanceof Civilian)) {
                return 1;
            }
            return h1.getID().getValue() - h2.getID().getValue();
        }
    }

    private final class UseIconsAction extends AbstractAction {
        public UseIconsAction() {
            super("Use icons");
            putValue(Action.SELECTED_KEY, Boolean.valueOf(useIcons));
            putValue(Action.SMALL_ICON, useIcons ? Icons.TICK : Icons.CROSS);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            useIcons = !useIcons;
            putValue(Action.SELECTED_KEY, Boolean.valueOf(useIcons));
            putValue(Action.SMALL_ICON, useIcons ? Icons.TICK : Icons.CROSS);
            component.repaint();
        }
    }

    private final class RenderATInfoAction extends AbstractAction {
        public RenderATInfoAction() {
            super("Ambulance Team Info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setATRenderInfo(!atInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(atInfo));
            putValue(Action.SMALL_ICON, atInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class RenderFBInfoAction extends AbstractAction {
        public RenderFBInfoAction() {
            super("Fire Brigade Info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setFBRenderInfo(!fbInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(fbInfo));
            putValue(Action.SMALL_ICON, fbInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class RenderPFInfoAction extends AbstractAction {
        public RenderPFInfoAction() {
            super("Police Force Info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setPFRenderInfo(!pfInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(pfInfo));
            putValue(Action.SMALL_ICON, pfInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class RenderCivInfoAction extends AbstractAction {
        public RenderCivInfoAction() {
            super("Civilian Info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setCivRenderInfo(!civInfo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(civInfo));
            putValue(Action.SMALL_ICON, civInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class VisibleAllInfoAction extends AbstractAction {
        public VisibleAllInfoAction() {
            super("Visible All Info");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisibleInfo(!visibleAll);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(visibleAll));
            putValue(Action.SMALL_ICON, visibleAll ? Icons.TICK : Icons.CROSS);

            if (visibleAll) {
                renderVisibleAllInfoTrue();
            } else {
                renderVisibleAllInfoFalse();
            }
            atInfoAction.update();
            fbInfoAction.update();
            pfInfoAction.update();
            civInfoAction.update();
        }
    }

    private final class LineOfSightAction extends AbstractAction {
        public LineOfSightAction() {
            super("Line Of Sight");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setLineOfSight(!lineOfSight);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(lineOfSight));
            putValue(Action.SMALL_ICON, lineOfSight ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class SayRangeAction extends AbstractAction {
        public SayRangeAction() {
            super("Say Range");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setSayRange(!sayRange);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(sayRange));
            putValue(Action.SMALL_ICON, sayRange ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class ViewRangeAction extends AbstractAction {
        public ViewRangeAction() {
            super("view Range");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setViewRange(!viewRange);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(viewRange));
            putValue(Action.SMALL_ICON, viewRange ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class ClearRangeAction extends AbstractAction {
        public ClearRangeAction() {
            super("Clear Range");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setClearRange(!clearRange);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(clearRange));
            putValue(Action.SMALL_ICON, clearRange ? Icons.TICK : Icons.CROSS);
        }
    }


    private final class ExtinguishRangeAction extends AbstractAction {

        public ExtinguishRangeAction() {
            super("Extinguish Range");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setExtinguishRange(!extinguishRange);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(extinguishRange));
            putValue(Action.SMALL_ICON, extinguishRange ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class RescueRegionRangeAction extends AbstractAction {

        public RescueRegionRangeAction() {
            super("Rescue Region Range");
            update();
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            setRescueRegionRange(!rescueRegionRange);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(rescueRegionRange));
            putValue(Action.SMALL_ICON, rescueRegionRange ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class TargetToGoAction extends AbstractAction {

        public TargetToGoAction() {
            super("Target To Go");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setTargetToGo(!targetToGo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(targetToGo));
            putValue(Action.SMALL_ICON, targetToGo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class PFGuidelineAction extends AbstractAction {

        public PFGuidelineAction() {
            super("PF Guideline");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setPFGuideline(!showPFGuideline);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(showPFGuideline));
            putValue(Action.SMALL_ICON, showPFGuideline ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class AllAmbulanceTargetsToGoAction extends AbstractAction {

        public AllAmbulanceTargetsToGoAction() {
            super("All Ambulance Targets To Go");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setAllAmbulanceTargetsToGo(!allAmbulanceTargetsToGo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(allAmbulanceTargetsToGo));
            putValue(Action.SMALL_ICON, allAmbulanceTargetsToGo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class AllFireBrigadeTargetsToGoAction extends AbstractAction {

        public AllFireBrigadeTargetsToGoAction() {
            super("All Fire Brigade Targets To Go");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setAllFireBrigadeTargetsToGo(!allFireBrigadeTargetsToGo);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(allFireBrigadeTargetsToGo));
            putValue(Action.SMALL_ICON, allFireBrigadeTargetsToGo ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class AgentSizeAction extends AbstractAction {
        public AgentSizeAction() {
            super("Agent Size");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setAgentSize(!agentSize);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(agentSize));
            putValue(Action.SMALL_ICON, agentSize ? Icons.TICK : Icons.CROSS);
        }
    }

    private final class AgentLocationAction extends AbstractAction {
        public AgentLocationAction() {
            super("Agent Location");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setAgentLocation(!agentLocation);
            component.repaint();
        }

        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(agentLocation));
            putValue(Action.SMALL_ICON, agentLocation ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setATRenderInfo(boolean render) {
        atInfo = render;
        atInfoAction.update();
    }

    public void setFBRenderInfo(boolean render) {
        fbInfo = render;
        fbInfoAction.update();
    }

    public void setPFRenderInfo(boolean render) {
        pfInfo = render;
        pfInfoAction.update();
    }

    public void setCivRenderInfo(boolean render) {
        civInfo = render;
        civInfoAction.update();
    }

    public void setVisibleInfo(boolean render) {
        visibleAll = render;
        visibleAllInfoAction.update();
    }

    public void setLineOfSight(boolean render) {
        lineOfSight = render;
        lineOfSightAction.update();
    }

    public void setSayRange(boolean render) {
        sayRange = render;
        sayRangeAction.update();
    }

    public void setViewRange(boolean render) {
        viewRange = render;
        viewRangeAction.update();
    }

    public void setClearRange(boolean render) {
        clearRange = render;
        clearRangeAction.update();
    }

    public void setAgentSize(boolean render) {
        agentSize = render;
        agentSizeAction.update();
    }

    public void setAgentLocation(boolean render) {
        agentLocation = render;
        agentLocationAction.update();
    }

    public void setExtinguishRange(boolean render) {
        extinguishRange = render;
        extinguishRangeAction.update();
    }

    public void setRescueRegionRange(boolean render) {
        rescueRegionRange = render;
        rescueRegionRangeAction.update();
    }

    public void setTargetToGo(boolean render) {
        targetToGo = render;
        targetToGoAction.update();
    }

    public void setPFGuideline(boolean render) {
        showPFGuideline = render;
        pfGuidelineAction.update();
    }

    public void setAllAmbulanceTargetsToGo(boolean render) {
        allAmbulanceTargetsToGo = render;
        allAmbulanceTargetsToGoAction.update();
    }

    public void setAllFireBrigadeTargetsToGo(boolean render) {
        allFireBrigadeTargetsToGo = render;
        allFireBrigadeTargetsToGoAction.update();
    }

    private void renderATAction(Human human, Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {

        if (human instanceof AmbulanceTeam) {
            String strID = human.getID().toString();
            g.setColor(Color.WHITE.brighter());
            drawInfo(g, t, strID, location, -13, 25);
//            String s = String.valueOf(MrlPlatoonAgent.VALUE_FOR_DISPLAY_IN_VIEWER.get(human.getID()));
            String s = "null";
            if (human.isBuriednessDefined()) {
                s = String.valueOf(human.getBuriedness());
            }
            if (!s.equals("null")) {
                drawInfo(g, t, s, location, -16, -15);
            }
            if (lineOfSight) {
                renderLineOfSight(human, g, t, location);
            }
        }
    }

    private void renderFBAction(Human human, Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {

        if (human instanceof FireBrigade) {
            String strID = human.getID().toString();
            g.setColor(Color.RED.darker());
            drawInfo(g, t, strID, location, -13, 25);
//            String s = String.valueOf(MrlPlatoonAgent.VALUE_FOR_DISPLAY_IN_VIEWER.get(human.getID()));
            String s = "null";
            if (human.isBuriednessDefined()) {
                s = String.valueOf(human.getBuriedness());
            }
            if (!s.equals("null")) {
                drawInfo(g, t, s, location, -16, -15);
            }
            if (lineOfSight) {
                renderLineOfSight(human, g, t, location);
            }
        }
    }

    private void renderPFAction(Human human, Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {

        if (human instanceof PoliceForce) {
            String strID = human.getID().toString();
            g.setColor(Color.BLUE.brighter());
            drawInfo(g, t, strID, location, -13, 25);
            String s = "null";
            if (human.isBuriednessDefined()) {
                s = String.valueOf(human.getBuriedness());
            }
//            String s = String.valueOf(MrlPlatoonAgent.VALUE_FOR_DISPLAY_IN_VIEWER.get(human.getID()));
            if (!s.equals("null")) {
                drawInfo(g, t, s, location, -16, -15);
            }
            if (lineOfSight) {
                renderLineOfSight(human, g, t, location);
            }
        }
    }

    private int printTime = 0;
    private void renderCivAction(Human human, Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {

        AmbulanceTarget ambulanceTarget=null;
        if(AMBUULANCE_TARGET_MAP!=null && !AMBUULANCE_TARGET_MAP.isEmpty() && StaticViewProperties.selectedObject instanceof AmbulanceTeam){
            Map<EntityID, AmbulanceTarget> ambulanceTargetMap = AMBUULANCE_TARGET_MAP.get(StaticViewProperties.selectedObject.getID());
            if(ambulanceTargetMap!=null) {
                ambulanceTarget = ambulanceTargetMap.get(human.getID());
            }
        }

        if(ambulanceTarget!=null){
            String strValue = String.valueOf(ambulanceTarget.getValue());
            g.setColor(Color.orange);
            drawInfo(g, t, strValue, location, -13, 15);
            drawInfo(g, t, String.valueOf(ambulanceTarget.getEstimatedHP()), location, -13, 35);
            drawInfo(g, t, String.valueOf(ambulanceTarget.getEstimatedDamage()), location, -13, 55);
            if(human.getHP() < human.getStamina() && human.getBuriedness()>0) {
                if(printTime!= TIME) {
                    printTime = TIME;
                    System.out.println(human.getID() + "\t" + TIME + "\t" + human.getHP() + "\t" + human.getDamage() + "\t" + ambulanceTarget.getEstimatedDamage());
                }
            }
        }

        if (human instanceof Civilian) {
            String strID = human.getID().toString();
            String strHP = String.valueOf(human.getHP());
            String strDMG = String.valueOf(human.getDamage());
            String strBRD = String.valueOf(human.getBuriedness());
            g.setColor(Color.GREEN);
//            drawInfo(g, t, strID, location, -13, 15);
            drawInfo(g, t, strHP, location, -16, -7);
            drawInfo(g, t, strDMG, location, 6, 5);
            drawInfo(g, t, strBRD, location, -25, 5);
        }
    }

    private void renderVisibleAllInfoFalse() {
        atInfo = false;
        fbInfo = false;
        pfInfo = false;
        civInfo = false;
    }

    private void renderVisibleAllInfoTrue() {
        atInfo = true;
        fbInfo = true;
        pfInfo = true;
        civInfo = true;
    }

    private void renderLineOfSight(Human h, Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        if (h instanceof Civilian) {
            return;
        }
        int x = location.first();
        int y = location.second();
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x), t.yToScreen(y + 30000));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x), t.yToScreen(y - 30000));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x + 30000), t.yToScreen(y));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x - 30000), t.yToScreen(y));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x + 21277), t.yToScreen(y + 21277));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x + 21277), t.yToScreen(y - 21277));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x - 21277), t.yToScreen(y + 21277));
        g.drawLine(t.xToScreen(x), t.yToScreen(y), t.xToScreen(x - 21277), t.yToScreen(y - 21277));
    }

    private void renderSayRange(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        int x = location.first();
        int y = location.second();
        int d = t.xToScreen(x + SAY_RANGE) - t.xToScreen(x);
        Circle2D circle2D = new Circle2D(t.xToScreen(x), t.yToScreen(y), d, true);
        circle2D.draw(g);
    }

    private void renderViewRange(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        int x = location.first();
        int y = location.second();
        int d = t.xToScreen(x + VIEW_RANGE) - t.xToScreen(x);
        Circle2D circle2D = new Circle2D(t.xToScreen(x), t.yToScreen(y), d, true);
        circle2D.draw(g);
    }

    private void renderClearRange(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        int x = location.first();
        int y = location.second();
        int d = t.xToScreen(x + CLEAR_RANGE) - t.xToScreen(x);
        Circle2D circle2D = new Circle2D(t.xToScreen(x), t.yToScreen(y), d, true);
        circle2D.draw(g);
    }

    private void renderExtinguishRange(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        g.setColor(Color.blue);

        int x = location.first();
        int y = location.second();
        int d = t.xToScreen(x + EXTINGUISH_RANGE) - t.xToScreen(x);
        Circle2D circle2D = new Circle2D(t.xToScreen(x), t.yToScreen(y), d, true);
        circle2D.draw(g);

        g.setColor(Color.MAGENTA);
    }

    private void renderRescueRegionRange(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        g.setColor(Color.blue);

        int x = location.first();
        int y = location.second();
        int d = t.xToScreen(x + RESCUE_RANGE) - t.xToScreen(x);
        Circle2D circle2D = new Circle2D(t.xToScreen(x), t.yToScreen(y), d, true);
        g.setColor(Color.ORANGE);
        g.setStroke(STROKE);
        circle2D.draw(g);
        g.setStroke(STROKE_DEFAULT);
    }

    private void renderAgentSize(Graphics2D g, ScreenTransform t, Pair<Integer, Integer> location) {
        int x = location.first();
        int y = location.second();
        int d = t.xToScreen(x + AGENT_SIZE) - t.xToScreen(x);
        Circle2D circle2D = new Circle2D(t.xToScreen(x), t.yToScreen(y), d, true);
        circle2D.draw(g);
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
}

