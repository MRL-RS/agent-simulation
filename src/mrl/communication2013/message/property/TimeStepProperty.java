package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/20/13
 * Time: 5:34 PM
 * Author: Mostafa Movahedi
 */
public class TimeStepProperty extends AbstractProperty {
    public TimeStepProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(10);
    }
}
