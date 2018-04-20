package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:23 PM
 * Author: Mostafa Movahedi
 */
public class Loader extends MessageEntity {
    EntityID HumanID;

    public Loader(EntityID humanID, int sendTime) {
        super(sendTime);
        HumanID = humanID;
    }

    public EntityID getHumanID() {
        return HumanID;
    }

    public void setHumanID(EntityID humanID) {
        HumanID = humanID;
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.Loader);
    }
}
