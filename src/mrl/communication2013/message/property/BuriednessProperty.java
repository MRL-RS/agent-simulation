package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/17/13
 * Time: 5:07 PM
 * Author: Mostafa Movahedi
 * To change this template use File | Settings | File Templates.
 */
public class BuriednessProperty extends AbstractProperty {
    public BuriednessProperty(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(7);
    }
}
