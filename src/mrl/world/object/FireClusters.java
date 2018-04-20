package mrl.world.object;

import javolution.util.FastMap;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

/**
 * User: vahid
 * Date: 5/29/11
 * Time: 2:53 PM
 */
public class FireClusters extends ArrayList<FireCluster> {
    private MrlWorld world;

    public FireClusters(MrlWorld world) {
        this.world = world;
    }

    public void update() {
        for (MrlZone zone : world.getZones().getBurningZones()) {
            if (!containsZone(zone)) {
                add(new FireCluster(zone, world));
            }
        }
        mergeFireClusters();

        for (FireCluster fireCluster : this) {
            fireCluster.update();
        }
//        if (MRLConstants.FILL_VIEWER_DATA) {
//            MrlFireClusterLayer.FIRECLUSTER_BUILDINGS_MAP.put(world.getSelf().getID(), this);
//        }
    }

    private boolean containsZone(MrlZone zone) {
        for (FireCluster fireCluster : this) {
            if (fireCluster.contains(zone)) {
                return true;
            }
        }
        return false;

    }

    private void mergeFireClusters() {
        HashSet<FireCluster> separatedSites = new HashSet<FireCluster>();
        Map<FireCluster, FireCluster> map = new FastMap<FireCluster, FireCluster>();

        boolean isSeparated;
        for (FireCluster c1 : this) {
            isSeparated = true;
            for (FireCluster c2 : this) {
                if (map.get(c2) == c1) {
                    isSeparated = false;
                }
                if (c1.canBeMerged(c2) && c1 != c2 && map.get(c2) != c1) {
                    isSeparated = false;
                    map.put(c2, c1);
                    map.put(c1, c2);
                    c1.addAll(c2);

                    separatedSites.add(c1);
//                     if (!separatedSites.contains(c1) && !separatedSites.contains(c2)) {
//                         separatedSites.add(c1);
//                     }
                }
            }
            if (isSeparated)
                separatedSites.add(c1);
        }

        this.clear();
        this.addAll(separatedSites);
    }

}
