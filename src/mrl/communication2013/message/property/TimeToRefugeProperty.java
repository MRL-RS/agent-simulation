package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 3:51 PM
 * Author: Mostafa Movahedi
 */
public class TimeToRefugeProperty extends AbstractProperty {
    public TimeToRefugeProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(9);

    }
}
