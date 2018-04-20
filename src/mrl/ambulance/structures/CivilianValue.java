package mrl.ambulance.structures;

import rescuecore2.worldmodel.EntityID;

/**
 * User: Pooyad
 * Date: Feb 6, 2011
 * Time: 12:15:22 PM
 */
public class CivilianValue implements Comparable {

    private EntityID id;
    private int value;


    public CivilianValue(EntityID id, int tta) {
        this.id = id;
        this.value = tta;
    }

    public EntityID getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int compareTo(Object o) {
        CivilianValue civilianValue = (CivilianValue) o;

        int value = civilianValue.getValue();

        if (this.value > value) //increase
            return 1;
        if (this.value == value)
            return 0;

        return -1;

    }
}
