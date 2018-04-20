package mrl.platoon.search.la;

import mrl.la.Action;
import mrl.world.object.mrlZoneEntity.MrlZone;

/**
 * User: roohola
 * Date: 5/10/11
 * Time: 2:51 PM
 */
public class ZoneAction implements Action {
    private MrlZone zone;

    public ZoneAction(MrlZone zone) {
        this.zone = zone;
    }

    public MrlZone getZone() {
        return zone;
    }

    public int getIndex() {
        return zone.getId();
    }


    public String getActionName() {
        return "ZoneAction[" + getIndex() + "]";
    }

    @Override
    public int compareTo(Action o) {
        if (o.getIndex() < getIndex())
            return 1;
        if (o.getIndex() == getIndex())
            return 0;
        return -1;
    }

    @Override
    public String toString() {
        return getActionName();
    }
}
