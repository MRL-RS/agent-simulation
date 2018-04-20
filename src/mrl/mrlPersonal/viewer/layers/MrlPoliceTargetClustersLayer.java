package mrl.mrlPersonal.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.partitioning.Partition;
import rescuecore2.config.Config;
import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.view.StandardViewLayer;
import rescuecore2.view.Icons;
import rescuecore2.view.RenderedObject;
import rescuecore2.worldmodel.EntityID;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 12/1/12
 *         Time: 5:32 PM
 */
public class MrlPoliceTargetClustersLayer extends StandardViewLayer {

    private static final Stroke STROKE = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke STROKE_All_SEGMENTS = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    private static final Stroke STROKE_MY_SEGMENT = new BasicStroke(4, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT);
    private boolean visible;
    protected Random random;

    public static Map<EntityID, List<Partition>> PARTITIONS_MAP = Collections.synchronizedMap(new FastMap<EntityID, List<Partition>>());
    public static Map<EntityID, Map<Human, Set<Partition>>> HUMAN_PARTITIONS_MAP = Collections.synchronizedMap(new FastMap<EntityID, Map<Human, Set<Partition>>>());

    private boolean policeTargetsInfo;
    private RenderPoliceTargetsAction policeTargetsAction;


    private boolean assignmentInfo;
    private RenderAssignmentInfoAction assignmentInfoAction;


    /**
     * Construct an area view layer.
     */
    public MrlPoliceTargetClustersLayer() {

    }

    @Override
    public void initialise(Config config) {
        policeTargetsInfo = true;
        policeTargetsAction = new RenderPoliceTargetsAction();

        assignmentInfo = false;
        assignmentInfoAction = new RenderAssignmentInfoAction();

    }

    @Override
    public List<JMenuItem> getPopupMenuItems() {
        List<JMenuItem> result = new ArrayList<JMenuItem>();

        result.add(new JMenuItem(policeTargetsAction));
        result.add(new JMenuItem(assignmentInfoAction));

        return result;
    }


    @Override
    public Collection<RenderedObject> render(Graphics2D g, ScreenTransform t, int width, int height) {

        List<Partition> partitions = null;

        Map<Human, Set<Partition>> humanPartitionsMap = null;

        try {
            if (StaticViewProperties.selectedObject != null) {
                if (PARTITIONS_MAP.get(StaticViewProperties.selectedObject.getID()) != null) {
                    partitions = Collections.synchronizedList(PARTITIONS_MAP.get(StaticViewProperties.selectedObject.getID()));

                    humanPartitionsMap = HUMAN_PARTITIONS_MAP.get(StaticViewProperties.selectedObject.getID());
                }
            }
        } catch (NullPointerException ignored) {
            ignored.printStackTrace();
        }


        g.setStroke(STROKE);
        Collection<RenderedObject> list = new ArrayList<RenderedObject>();
        if (partitions == null) {
            return list;
        }

        if (policeTargetsInfo) {
            renderPoliceTargets(g, t, partitions);
        }

        if (assignmentInfo) {
            renderAssignment(g, t, humanPartitionsMap);
        }

        return list;
    }

    private void renderPoliceTargets(Graphics2D g, ScreenTransform t, List<Partition> partitions) {
        Color COLOUR;
        if (partitions != null) {
            for (Partition partition : partitions) {
                int[] x = new int[partition.getPolygon().npoints];
                int[] y = new int[partition.getPolygon().npoints];
                for (int i = 0; i < partition.getPolygon().npoints; i++) {
                    x[i] = t.xToScreen(partition.getPolygon().xpoints[i]);
                    y[i] = t.yToScreen(partition.getPolygon().ypoints[i]);
                }

                Polygon shape = new Polygon(x, y, partition.getPolygon().npoints);
                random = new Random(7 * partition.getId().getValue() * MrlViewer.randomValue);
                COLOUR = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);
                g.setColor(COLOUR);


                //ID
                g.drawString("ID: " + String.valueOf(partition.getId()), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY() - 10);

                //Value
                g.drawString("Value: " + String.valueOf(partition.getValue()), (int) shape.getBounds().getCenterX(), (int) shape.getBounds().getCenterY() + 10);

                g.draw(shape);
//
            }

        }
    }


    private void renderAssignment(Graphics2D g, ScreenTransform t, Map<Human, Set<Partition>> humanPartitionsMap) {
        Color COLOUR;
        int sumX;
        int sumY;
        if (humanPartitionsMap != null) {

            random = new Random();
            COLOUR = Color.CYAN;
            g.setColor(COLOUR);
            int x1, y1, x2, y2;
            Pair<Integer, Integer> location;


            Set<Partition> partitions = null;
            for (Human human : humanPartitionsMap.keySet()) {

                partitions = humanPartitionsMap.get(human);
                if (partitions != null && !partitions.isEmpty()) {
                    for (Partition partition : partitions) {

                        x1 = partition.getCenter().first();
                        y1 = partition.getCenter().second();
                        location = world.getEntity(human.getID()).getLocation(world);
                        x2 = location.first();
                        y2 = location.second();

                        g.setColor(Color.red);
                        g.drawLine(t.xToScreen(x1), t.yToScreen(y1), t.xToScreen(x2), t.yToScreen(y2));

                    }
                }
            }
        }


    }

    private final class RenderPoliceTargetsAction extends AbstractAction {
        public RenderPoliceTargetsAction() {
            super("Police Targets");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setPoliceTargetInfo(!policeTargetsInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(policeTargetsInfo));
            putValue(Action.SMALL_ICON, policeTargetsInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setPoliceTargetInfo(boolean render) {
        policeTargetsInfo = render;
        policeTargetsAction.update();
    }


    private final class RenderAssignmentInfoAction extends AbstractAction {
        public RenderAssignmentInfoAction() {
            super("Assignment");
            update();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setTargetsRenderInfo(!assignmentInfo);
            component.repaint();
        }


        void update() {
            putValue(Action.SELECTED_KEY, Boolean.valueOf(assignmentInfo));
            putValue(Action.SMALL_ICON, assignmentInfo ? Icons.TICK : Icons.CROSS);
        }
    }

    public void setTargetsRenderInfo(boolean render) {
        assignmentInfo = render;
        assignmentInfoAction.update();
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
        return "PoliceTargetClusters";
    }

}
