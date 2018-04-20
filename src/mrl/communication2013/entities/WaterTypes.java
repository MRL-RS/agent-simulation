package mrl.communication2013.entities;

/**
 * User: MRL
 * Date: 5/19/13
 * Time: 3:02 PM
 * Author: Mostafa Movahedi
 */
public enum WaterTypes {
    MaxPower,
    Partial,;

    public static WaterTypes indexToEnum(int index) {
        for (WaterTypes water : WaterTypes.values()) {
            if (water.ordinal() == index)
                return water;
        }
        return null;
    }

    public static int getWaterTypeBitNum() {
        int res = 1;
        int size = WaterTypes.values().length - 1;
        while ((size >>= 1) != 0)
            res++;
        return res;
    }
}
