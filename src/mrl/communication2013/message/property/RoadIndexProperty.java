package mrl.communication2013.message.property;

import mrl.communication2013.message.IDConverter;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 12:54 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class RoadIndexProperty extends AbstractProperty {
    public RoadIndexProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(IDConverter.getRoadsBitSize());
    }
}
