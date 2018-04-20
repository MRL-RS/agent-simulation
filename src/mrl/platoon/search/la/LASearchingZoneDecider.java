package mrl.platoon.search.la;


import javolution.util.FastMap;
import mrl.MrlPersonalData;
import mrl.common.Util;
import mrl.la.LA_LRP;
import mrl.platoon.search.SearchingZoneDeciderI;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: roohola
 * Date: 5/10/11
 * Time: 2:51 PM
 */
public class LASearchingZoneDecider implements SearchingZoneDeciderI {
    LA_LRP la;
    List<ZoneState> zoneStates = new ArrayList<ZoneState>();
    Map<MrlZone, ZoneState> stateMap = new FastMap<MrlZone, ZoneState>();
    MrlWorld world;
    ZoneAction lastAction;
    protected MrlZone searchingZone = null;


    int allAgentSize;
    public static final int AGENT_IN_ZONE_THRESHOLD = 3;

    public LASearchingZoneDecider(MrlWorld world) {
        this.world = world;
        la = new LA_LRP(.1d, .1d);
        initializeStates();
        allAgentSize = world.getAgents().size();
    }

    private void initializeStates() {

        for (MrlZone zoneEntity : world.getZones()) {
            List<MrlZone> n = new ArrayList<MrlZone>(zoneEntity.getNeighbors());
            n.add(zoneEntity);
            ZoneState state = new ZoneState(n, zoneEntity);
            stateMap.put(zoneEntity, state);
            zoneStates.add(state);
        }
    }

    public MrlZone learning(MrlZone currentZone, int epoch) {
        if (lastAction == null) {
            la.setCurrentState(stateMap.get(currentZone));
        } else {
            la.setCurrentState(stateMap.get(lastAction.getZone()));
        }
        ZoneAction action = null;
        ZIOCollection zio;
        for (int i = 0; i < epoch; i++) {
            action = (ZoneAction) la.action();
            zio = action.getZone().getZio();

            if (action.equals(lastAction)) {
                la.reward(action, 0.01);
            }

            if (zio.zone.isOnFire()) {
                double unburnedRatio = (double) zio.zone.getUnBurnedBuildings().size() / (double) zio.zone.size();
                if (unburnedRatio >= 0.5) {
                    la.reward(action, unburnedRatio / 30.0);
                } else {
                    la.penalize(action, unburnedRatio / 30.0);
                }

            }
            if (zio.zone.isBurned()) {
                la.penalize(action, 0.9);
            }

            if (!zio.possibleBuildingsInZone.isEmpty()) {
                double possibleRatio = zio.possibleBuildingsInZone.size() / (zio.zone.size() * 20.0);
                la.reward(action, possibleRatio);
            }

            if (zio.unvisitedBuildingsInZone.isEmpty()) {
                la.penalize(action, 0.9);
            } else {
                double unvisitedRatio = zio.unvisitedBuildingsInZone.size() / (zio.zone.size() * 40.0);
                la.reward(action, unvisitedRatio);
            }

//            if (zio.agentsInZone.size() <= AGENT_IN_ZONE_THRESHOLD) {
//                la.reward(action, 0.002);
//            } else {
//                la.penalize(action, 0.9);
//            }


        }
        lastAction = action;

        MrlPersonalData.VIEWER_DATA.setActionData(world.getSelf().getID(), action.getZone());

        assert action != null;
        return action.getZone();

    }

    protected MrlZone getCurrentZone() {
        for (MrlZone zoneEntity : world.getZones()) {
            if (zoneEntity.contains(world.getSelfLocation().first(), world.getSelfLocation().first())) {
                return zoneEntity;
            }
        }
        int minDistance = Integer.MAX_VALUE;
        MrlZone nearestZone = null;
        for (MrlZone zoneEntity : world.getZones()) {
            int d = Util.distance(zoneEntity.getCenter().x, zoneEntity.getCenter().y, world.getSelfLocation().first(), world.getSelfLocation().second());
            if (d < minDistance) {
                nearestZone = zoneEntity;
                minDistance = d;
            }

        }
        return nearestZone;
    }

    public MrlZone decideZone() {
        MrlZone currentZone = getCurrentZone();
        if (currentZone != null) {

            if (searchingZone == null || searchingZone.getZio().unvisitedBuildingsInZone.isEmpty()) {
                searchingZone = learning(currentZone, 50);
                if (searchingZone.getZio().unvisitedBuildingsInZone.isEmpty()) {
                    world.printData(" a zone all visited selected!!!  " + searchingZone);
                }
            }

        }
        return searchingZone;
    }
}
