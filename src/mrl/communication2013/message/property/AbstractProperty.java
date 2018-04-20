package mrl.communication2013.message.property;

import mrl.communication2013.message.Message;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/16/13
 * Time: 7:56 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
abstract public class AbstractProperty {
    protected int value;
    protected int propertyBitSize;

    public AbstractProperty(int value) {
        this.value = value;
        setPropertyBitSize();
    }

    protected AbstractProperty() {
        setPropertyBitSize();
    }

    public boolean write(Message msg) {
        if (!msg.write(getValue(), propertyBitSize)) {
            System.out.println(getClass().getName() + " write error : " + getValue() + " bits:" + propertyBitSize);
            return false;
        }
        return true;
    }

    public int read(Message msg) {
        return msg.read(propertyBitSize, false);
    }

    /**
     * define how many bits need for this property.
     * exp: setPropertyBitSize(needed_bits);
     */
    abstract protected void setPropertyBitSize();

    public void setPropertyBitSize(int propertyBitSize) {
        this.propertyBitSize = propertyBitSize;
    }

    public int getPropertyBitSize() {
        return propertyBitSize;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
