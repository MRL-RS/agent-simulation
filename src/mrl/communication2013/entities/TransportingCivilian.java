package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * User: MRL
 * Date: 5/20/13
 * Time: 3:43 PM
 * Author: Mostafa Movahedi
 */
public class TransportingCivilian extends MessageEntity {
    EntityID humanID;

    public TransportingCivilian(EntityID humanID, int sendTime) {
        super(sendTime);
        this.humanID = humanID;
    }

    public EntityID getHumanID() {
        return humanID;
    }

    public void setHumanID(EntityID humanID) {
        this.humanID = humanID;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.TransportingCivilian);
    }
}
