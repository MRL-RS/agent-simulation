package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 2:12 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class FullBuilding extends MessageEntity {
    EntityID buildingID;
    int buildingPriority;

    public FullBuilding(EntityID buildingID, int buildingPriority, int sendTime) {
        super(sendTime);
        this.buildingID = buildingID;
        this.buildingPriority = buildingPriority;
    }

    public EntityID getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    public int getBuildingPriority() {
        return buildingPriority;
    }

    public void setBuildingPriority(int buildingPriority) {
        this.buildingPriority = buildingPriority;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.FullBuilding);
    }
}
