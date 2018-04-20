package mrl.communication2013.message.property;

import mrl.communication2013.message.IDConverter;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/16/13
 * Time: 8:01 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class BuildingIndexProperty extends AbstractProperty {

    public BuildingIndexProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(IDConverter.getBuildingsBitSize());
    }
}
