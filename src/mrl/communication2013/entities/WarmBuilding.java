package mrl.communication2013.entities;

import rescuecore2.worldmodel.EntityID;

/**
 * User: MRL
 * Date: 5/23/13
 * Time: 4:14 PM
 *
 * @Author: Mostafa Movahedi
 */
public class WarmBuilding extends MessageEntity {
    EntityID buildingID;

    public WarmBuilding() {
        super();
    }

    public WarmBuilding(EntityID buildingID, int sendTime) {
        super(sendTime);
        this.buildingID = buildingID;
    }

    public EntityID getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(EntityID buildingID) {
        this.buildingID = buildingID;
    }

    @Override
    protected void setMessageEntityType() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
