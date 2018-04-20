package mrl.communication2013.message.property;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 5/19/13
 * Time: 2:19 PM
 * Author: Mostafa Movahedi
 */
public class BuildingPriority extends AbstractProperty {

    public BuildingPriority(int value) {
        super(value);
    }

    @Override
    protected void setPropertyBitSize() {
        setPropertyBitSize(3);
    }
}
