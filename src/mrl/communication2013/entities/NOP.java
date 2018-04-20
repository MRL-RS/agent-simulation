package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;

/**
 * User: MRL
 * Date: 5/17/13
 * Time: 2:00 PM
 * Author: Mostafa Movahedi
 */
public class NOP extends MessageEntity {
    public NOP() {
        super();
    }

    @Override
    protected void setMessageEntityType() {
        setMessageEntityType(MessageTypes.NOP);
    }
}
