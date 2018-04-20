package mrl.ambulance.structures;

/**
 * User: pooyad
 * Date: 6/20/11
 * Time: 9:59 PM
 */

import rescuecore2.worldmodel.EntityID;

/**
 * It is about a good victim to consider in a refuge less scenario
 */
public class ValuableVictim {
    private EntityID victimID;
    private double caop;

    public ValuableVictim(EntityID victimID, double caop) {
        this.victimID = victimID;
        this.caop = caop;
    }

    public EntityID getVictimID() {
        return victimID;
    }

    public double getCaop() {
        return caop;
    }

}
