package mrl.common.comparator;

import rescuecore2.standard.entities.Building;
import rescuecore2.worldmodel.EntityID;

import java.util.Comparator;
import java.util.Map;

/**
 * User: mirsharifi
 * Date: Mar 10, 2011
 * Time: 11:55:44 PM
 */
public class BuildingValueComparator implements Comparator<Building> {
    Map<EntityID, Double> valueMap;

    public BuildingValueComparator(Map<EntityID, Double> valueMap) {
        this.valueMap = valueMap;
    }

    public int compare(Building b1, Building b2) {
        double v1 = valueMap.get(b1.getID()), v2 = valueMap.get(b2.getID());
        if (v1 > v2)
            return 1;
        if (v1 == v2)
            return 0;
        return -1;
    }
}
