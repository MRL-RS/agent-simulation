package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * @author Pooya Deldar Gohardani
 */
public class BuriedAgentSay extends BuriedAgent {
    EntityID buriedAgentID;

    public BuriedAgentSay() {
        super();
    }

    public BuriedAgentSay(EntityID buriedAgentID, EntityID buildingID, int hp, int buriedness, int damage, int sendTime) {
        super(buildingID, hp, buriedness, damage, sendTime);
        this.buriedAgentID = buriedAgentID;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.BuriedAgentSay);
    }


    public EntityID getBuriedAgentID() {
        return buriedAgentID;
    }

    public void setBuriedAgentID(EntityID buriedAgentID) {
        this.buriedAgentID = buriedAgentID;
    }

}
