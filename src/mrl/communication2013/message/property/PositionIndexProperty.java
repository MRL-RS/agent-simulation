package mrl.communication2013.message.property;

import mrl.communication2013.message.IDConverter;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 2:20 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class PositionIndexProperty extends AbstractProperty {
    public PositionIndexProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(Math.max(IDConverter.getBuildingsBitSize(), IDConverter.getRoadsBitSize()));
    }
}
