package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * User: MRL
 * Date: 5/19/13
 * Time: 2:32 PM
 * Author: Mostafa Movahedi
 */
public class Water extends MessageEntity {
    EntityID buildingID;
    WaterTypes waterType;

    public Water(EntityID buildingID, WaterTypes waterType, int sendTime) {
        super(sendTime);
        this.buildingID = buildingID;
        this.waterType = waterType;
    }

    public EntityID getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    public WaterTypes getWaterType() {
        return waterType;
    }

    public void setWater(WaterTypes waterType) {
        this.waterType = waterType;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.WaterMessage);
    }
}
