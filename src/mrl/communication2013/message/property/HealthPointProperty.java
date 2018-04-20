package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 5:04 PM
 * Author: Mostafa Movahedi
 */
public class HealthPointProperty extends AbstractProperty {
    public HealthPointProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(14);
    }
}
