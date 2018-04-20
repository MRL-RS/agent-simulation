package mrl.communication2013.entities;

/**
 * User: MRL
 * Date: 5/17/13
 * Time: 2:10 PM
 * Author: Mostafa Movahedi
 */
public enum PositionTypes {
    Building,
    Road,;

    public static PositionTypes indexToEnum(int index) {
        for (PositionTypes p : PositionTypes.values()) {
            if (p.ordinal() == index)
                return p;
        }
        return null;
    }

    public static int getPositionTypeBitNum() {
        int res = 1;
        int size = PositionTypes.values().length - 1;
        while ((size >>= 1) != 0)
            res++;
        return res;
    }
}
