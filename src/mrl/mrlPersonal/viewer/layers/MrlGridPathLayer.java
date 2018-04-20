package mrl.mrlPersonal.viewer.layers;

import rescuecore2.misc.Pair;
import rescuecore2.misc.gui.ScreenTransform;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.view.StandardEntityViewLayer;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Random;

/**
 * Created by Javoon
 * User: mrl
 * Date: Sep 20, 2010
 * Time: 5:33:52 PM
 */
public class MrlGridPathLayer extends StandardEntityViewLayer<Area> {

    static EntityID firstAreaId;
    private static final Stroke STROKE = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
    protected Random random;
    public static HashSet<ArrayList<Pair<Integer, Integer>>> GRID_PATH_TEMP = new HashSet<ArrayList<Pair<Integer, Integer>>>();


    public MrlGridPathLayer() {
        super(Area.class);
    }

    @Override
    public String getName() {
        return "Grids Path";
    }

    @Override
    public Shape render(Area area, Graphics2D g, ScreenTransform t) {

        Color COLOUR;

        g.setStroke(STROKE);
        if (firstAreaId == null) {
            firstAreaId = area.getID();
        } else if (firstAreaId == area.getID()) {

            try {
                for (ArrayList<Pair<Integer, Integer>> gridPath : GRID_PATH_TEMP) {
                    if (gridPath != null) {
                        random = new Random();
                        COLOUR = Color.getHSBColor(random.nextFloat() * gridPath.get(0).first() * 4, random.nextFloat() * gridPath.get(0).first() * 4, random.nextFloat() * gridPath.get(0).first() * 4);
                        g.setColor(COLOUR);

                        for (int i = 0; i < gridPath.size() - 1; i++) {
                            g.drawLine(t.xToScreen(gridPath.get(i).first()), t.yToScreen(gridPath.get(i).second()), t.xToScreen(gridPath.get(i + 1).first()), t.yToScreen(gridPath.get(i + 1).second()));
                        }
                    }
                }
            } catch (ConcurrentModificationException ignored) {
            }
        }

        return null;
    }
}