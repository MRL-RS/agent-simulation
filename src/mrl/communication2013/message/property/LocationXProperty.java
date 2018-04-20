package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 7:48 PM
 * Author: Mostafa Movahedi
 */
public class LocationXProperty extends AbstractProperty {
    public LocationXProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(24);
    }
}
