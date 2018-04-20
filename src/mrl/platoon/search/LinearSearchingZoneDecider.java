package mrl.platoon.search;

import mrl.common.Util;
import mrl.helper.HumanHelper;
import mrl.platoon.MrlPlatoonAgent;
import mrl.platoon.State;
import mrl.world.MrlWorld;
import mrl.world.object.mrlZoneEntity.MrlZone;
import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: roohola
 * Date: 6/22/11
 * Time: 4:48 PM
 */
public class LinearSearchingZoneDecider implements SearchingZoneDeciderI {

    protected MrlWorld world;
    protected double possibleCoef = 1.8;
    protected double unvisitedCoef = 1;
    protected double fireCoef = 0.9;
    protected double neighbourFireCoef = 1.5;
    protected double distanceCoef = 2.5;
    protected MrlZone searchingZone = null;
    protected int searchingTry;
    List<MrlZone> searchZones = new ArrayList<MrlZone>();


    public LinearSearchingZoneDecider(MrlWorld world) {
        this.world = world;
    }

    public static Comparator<MrlZone> SEARCH_VALUE_COMPARATOR = new Comparator<MrlZone>() {
        public int compare(MrlZone r1, MrlZone r2) {

            if (r1.getSearchValue() < r2.getSearchValue())
                return 1;
            if (r1.getSearchValue() == r2.getSearchValue())
                return 0;

            return -1;
        }
    };

    @Override
    public MrlZone decideZone() {
        MrlZone lastZone = searchingZone;

        if (searchingZone != null && isContinueToSearchingThis(searchingZone) && searchingZone.getZio().checkCanSearch(world.getTime())) {
            searchingTry++;
            return searchingZone;
        }

        updateValue();
        searchZones.clear();

        for (MrlZone zone : world.getZones()) {
            if (zone.getSearchValue() != Double.MIN_VALUE) {
                searchZones.add(zone);
            }
        }
        if (searchZones.isEmpty()) {
            return null;
        }

        Collections.sort(searchZones, SEARCH_VALUE_COMPARATOR);
        MrlPlatoonAgent self = (MrlPlatoonAgent) world.getSelf();
        int maxSize = Math.min(world.getPlatoonAgents().size(), searchZones.size());
        int newValue = (12 / world.getTime()) + 3;
        int index = self.getRandom().nextInt(Math.min(Math.min(newValue, getNumberOfSearchingAgents()), maxSize));
//        int index = self.getRandom().nextInt(Math.min(Math.max(7, Math.min(15, getNumberOfSearchingAgents())), maxSize));
        searchingZone = searchZones.get(index);

        if (lastZone != null && lastZone.equals(searchingZone)) {
            searchingTry++;
        } else {
            searchingTry = 0;
        }
        return searchingZone;
    }

    private boolean isContinueToSearchingThis(MrlZone zone) {
        return (!zone.getZio().unvisitedBuildingsInZone.isEmpty() || (searchingTry <= 15 && zone.getSearchValue() <= 0)) && world.getTime() > 7;

    }

    protected int getNumberOfSearchingAgents() {
        HumanHelper humanHelper = world.getHelper(HumanHelper.class);
        int count = 1;// khodesh to search hast az ghabl
        for (StandardEntity standardEntity : world.getPlatoonAgents()) {
            State state = humanHelper.getAgentState(standardEntity.getID());
            if (state != null && state == State.SEARCHING) {
                count++;
            }
        }
        return (count * 3 / 2);
    }

    private void updateValue() {

        for (MrlZone zone : world.getZones()) {

            double possibleRatio = zone.getZio().possibleBuildingsInZone.size();
            if (possibleRatio != 0) {
                possibleRatio = (double) zone.getZio().possibleCivCount / possibleRatio;
            }
            double unvisitedRatio = zone.getZio().unvisitedBuildingsInZone.size();
            if (unvisitedRatio == 0) {
                zone.setSearchValue(-zone.getZio().getLastSearchTime());
                continue;
            } else {
                unvisitedRatio = (unvisitedRatio / world.getZones().getZonesBuildingsCountAverage()) * ((double) world.getTime() / 100.0);
            }
            double unburnedRatio = 0;
            if (zone.isOnFire()) {
                unburnedRatio = (double) zone.getUnBurnedBuildings().size() / (double) zone.size();
                if (unburnedRatio < 0.5) {
                    zone.setSearchValue(-zone.getZio().getLastSearchTime());
                    continue;
                }
            }
            if (zone.isBurned()) {
                zone.setSearchValue(Double.MIN_VALUE);
                continue;
            }
            double neighbourFireRatio = 0;
            int fireCount = 0;
            for (MrlZone z : zone.getNeighbors()) {
                if (z.isOnFire()) {
                    neighbourFireRatio += (double) z.getUnBurnedBuildings().size() / (double) z.size();
                    fireCount++;
                }
            }
            if (fireCount != 0) {
                neighbourFireRatio /= (double) fireCount;
            }

            double distance = Util.distance(world.getSelfLocation(), zone.getCenter());
            if (distance < 3000) {
                distance = 3000;
            }
            double distanceValue = 3000.0 / (distance);
//            world.printData(" zone "+zone.toString()+ "  distance value = "+distanceValue);

            double value = (possibleRatio * possibleCoef) + (unvisitedRatio * unvisitedCoef) + (unburnedRatio * fireCoef) + (neighbourFireRatio * neighbourFireCoef) + (distanceValue * distanceCoef);
            zone.setSearchValue(value);
        }
    }
}
