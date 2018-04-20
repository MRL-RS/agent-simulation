package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * User: MRL
 * Date: 5/19/13
 * Time: 1:20 PM
 * Author: Mostafa Movahedi
 */
public class ExtinguishedBuilding extends MessageEntity {
    EntityID buildingID;
    int fieryness;

    public ExtinguishedBuilding(EntityID buildingID, int fieryness, int sendTime) {
        super(sendTime);
        this.buildingID = buildingID;
        this.fieryness = fieryness;

    }

    public EntityID getBuildingID() {

        return buildingID;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    public int getFieryness() {
        return fieryness;
    }

    public void setFieryness(int fieryness) {
        this.fieryness = fieryness;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.ExtinguishedBuilding);
    }
}
