package mrl.communication2013.message.property;

/**
 * @author Pooya Deldar Gohardani
 *         Date: 3/30/14
 *         Time: 10:28 PM
 */
public class AftershockProperty extends AbstractProperty {


    public AftershockProperty(int value) {
        super(value);
    }

    /**
     * define how many bits need for this property.
     * exp: setPropertyBitSize(needed_bits);
     */
    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(3);
    }
}
