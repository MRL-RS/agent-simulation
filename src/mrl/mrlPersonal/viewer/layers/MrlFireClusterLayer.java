package mrl.mrlPersonal.viewer.layers;/*
package mrl.viewer.layers;

import javolution.util.FastMap;
import mrl.mrlPersonal.viewer.MrlViewer;
import mrl.world.object.mrlZoneEntity.MrlZone;
import mrl.mrlPersonal.viewer.StaticViewProperties;
import mrl.world.object.FireCluster;
import mrl.world.object.FireClusters;
import mrl.world.object.MrlBuilding;
import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.awt.*;
import java.util.*;
import java.util.List;

*/
/**
 * User: vahid
 * Date: 5/29/11
 * Time: 12:30 PM
 *//*

public class MrlFireClusterLayer extends MrlAreaLayer<Building> {
    public static Map<EntityID, FireClusters> FIRECLUSTER_BUILDINGS_MAP = Collections.synchronizedMap(new FastMap<EntityID, FireClusters>());

    protected Random random;


    public MrlFireClusterLayer() {
        super(Building.class);
    }


    @Override
    protected void paintShape(Building b, Polygon shape, Graphics2D g) {
        if (StaticViewProperties.selectedObject != null) {

            FireClusters fireClusters;

            try {
                fireClusters = FIRECLUSTER_BUILDINGS_MAP.get(StaticViewProperties.selectedObject.getID());


                if (fireClusters != null) {
                    List<MrlZone> zoneEntities;
                    Color c;
                    for (FireCluster fireCluster : fireClusters) {
                        zoneEntities = fireCluster.getBurningAndBurnedBuilding();
                        if (zoneEntities == null || zoneEntities.size() == 0) {
                            continue;
                        }
                        random = new Random(zoneEntities.get(0).getId() * zoneEntities.get(0).getId() * MrlViewer.randomValue);
                        c = Color.getHSBColor(random.nextFloat() * 1.3f, random.nextFloat() * 2.5f, random.nextFloat() * 4.4f);

                        for (MrlZone zoneEntity : zoneEntities) {
                            for (MrlBuilding building : zoneEntity) {
                                if (building.getSelfBuilding().equals(b)) {
                                    drawSite(shape, g, c);
                                }
                            }
                        }
                    }
                }
            } catch (ConcurrentModificationException ignore) {
            }
        } else {
        }

    }

    private void drawSite(Polygon shape, Graphics2D g, Color color) {
        g.setColor(color);
        g.fill(shape);
    }


    @Override
    public String getName() {
        return "FireCluster";
    }
}
*/
