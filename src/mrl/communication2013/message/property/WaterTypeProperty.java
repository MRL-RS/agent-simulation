package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 2:36 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class WaterTypeProperty extends AbstractProperty {
    public WaterTypeProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(1);
    }
}
