package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/16/13
 * Time: 8:01 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class FierynessProperty extends AbstractProperty {

    public FierynessProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(4);
    }
}
