package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * User: MRL
 * Date: 5/18/13
 * Time: 7:31 PM
 * Author: Mostafa Movahedi
 */
public class BurningBuilding extends MessageEntity {
    private EntityID ID;
    private int fieryness;
    private int temperature;

    public BurningBuilding(EntityID ID, int fieryness, int temperature, int sendTime) {
        super(sendTime);
        this.ID = ID;
        this.fieryness = fieryness;
        this.temperature = temperature;
    }

    public BurningBuilding() {
        super();
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.BurningBuilding);
    }

    public void setID(EntityID ID) {
        this.ID = ID;
    }

    public void setFieryness(int fieryness) {
        this.fieryness = fieryness;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public EntityID getID() {
        return ID;
    }

    public int getFieryness() {
        return fieryness;
    }

    public int getTemperature() {
        return temperature;
    }
}
