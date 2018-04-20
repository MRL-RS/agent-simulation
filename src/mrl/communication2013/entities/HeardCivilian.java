package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 7:13 PM
 * Author: Mostafa Movahedi
 */
public class HeardCivilian extends MessageEntity {
    EntityID civilianID;
    int locationX;
    int locationY;

    public HeardCivilian(EntityID civilianID, int locationX, int locationY, int sendTime) {
        super(sendTime);
        this.civilianID = civilianID;
        this.locationX = locationX;
        this.locationY = locationY;
    }

    public EntityID getCivilianID() {
        return civilianID;
    }

    public void setCivilianID(EntityID civilianID) {
        this.civilianID = civilianID;
    }

    public int getLocationX() {
        return locationX;
    }

    public void setLocationX(int locationX) {
        this.locationX = locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocationY(int locationY) {
        this.locationY = locationY;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.HeardCivilian);
    }
}
