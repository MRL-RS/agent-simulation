package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 7:42 PM
 * Author: Mostafa Movahedi
 */
public class HumanIDProperty extends AbstractProperty {
    public HumanIDProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(31);
    }
}
