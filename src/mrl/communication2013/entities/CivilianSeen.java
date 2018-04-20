package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:47 PM
 * Author: Mostafa Movahedi
 */
public class CivilianSeen extends MessageEntity {
    EntityID CivilianID;
    int buriedness;
    int damage;
    int hp;
    EntityID buildingID;
    int timeToRefuge;

    public CivilianSeen() {
        super();
    }

    public CivilianSeen(EntityID civilianID, int buriedness, int damage, int hp, EntityID buildingID, int timeToRefuge, int sendTime) {
        super(sendTime);
        CivilianID = civilianID;
        this.buriedness = buriedness;
        this.damage = damage;
        this.hp = hp;
        this.buildingID = buildingID;
        this.timeToRefuge = timeToRefuge;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.CivilianSeen);
    }

    public EntityID getCivilianID() {
        return CivilianID;
    }

    public void setCivilianID(EntityID civilianID) {
        CivilianID = civilianID;
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

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public EntityID getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    public int getTimeToRefuge() {
        return timeToRefuge;
    }

    public void setTimeToRefuge(int timeToRefuge) {
        this.timeToRefuge = timeToRefuge;
    }
}
