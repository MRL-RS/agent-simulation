package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Mostafa Movahedi
 */
public class BuriedAgent extends MessageEntity {
    EntityID buildingID;
    int hp;
    int buriedness;
    int damage;

    public EntityID getBuildingID() {
        return buildingID;
    }

    public BuriedAgent() {
        super();
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.BuriedAgent);
    }

    public BuriedAgent(EntityID buildingID, int hp, int buriedness, int damage, int sendTime) {
        super(sendTime);
        this.buildingID = buildingID;
        this.hp = hp;
        this.buriedness = buriedness;
        this.damage = damage;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getBuriedness() {
        return buriedness;
    }

    public void setBuriedness(int buriedness) {
        this.buriedness = buriedness;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }
}
