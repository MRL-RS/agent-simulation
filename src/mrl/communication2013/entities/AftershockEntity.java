package mrl.communication2013.entities;

import mrl.communication2013.message.type.MessageTypes;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 3/30/14
 *         Time: 10:07 PM
 */
public class AftershockEntity extends MessageEntity {

    private int aftershockCount;


    public AftershockEntity(int sendTime, int aftershockCount) {
        super(sendTime);
        this.aftershockCount = aftershockCount;
    }


    public int getAftershockCount() {
        return aftershockCount;
    }

    public void setAftershockCount(int aftershockCount) {
        this.aftershockCount = aftershockCount;
    }

    @Override
    public String toString() {
        return "AftershockEntity{" +
                "aftershockCount=" + aftershockCount +
                '}';
    }

    @Override
    protected void setMessageEntityType() {

        setMessageEntityType(MessageTypes.AftershockEntity);
    }
}
