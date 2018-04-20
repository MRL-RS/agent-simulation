package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 7:48 PM
 * Author: Mostafa Movahedi
 */
public class LocationYProperty extends AbstractProperty {
    public LocationYProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(24);
    }
}
