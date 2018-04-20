package mrl.common;

import mrl.ambulance.MrlAmbulanceCentre;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.Refuge;

/**
 * User: mrl
 * Date: May 5, 2010
 * Time: 5:03:25 PM
 */
public interface ConstantConditions {
    static final Condition REFUGE_C = new Condition() {
        public boolean eval(Object obj) {
            return (obj instanceof Refuge);
        }
    };

    static final Condition CENTER_C = new Condition() {
        public boolean eval(Object obj) {
            return (obj instanceof PoliceOffice || obj instanceof MrlAmbulanceCentre ||
                    obj instanceof FireStation);
        }
    };

    static final Condition SPECIAL_BUILDING_C = REFUGE_C.or(CENTER_C);


//    public static final Condition SHOULD_CHECK_PATH_CND = new Condition() {
//        public boolean eval(Object obj) {
//            return ((Path) obj).shouldCheck();
//        }
//    };

//    public static final Condition SHOULD_CHECK_ROAD_CND = new Condition() {
//        public boolean eval(Object obj) {
//            return ((Road) obj).getRoadIsOpen();
//        }
//    };

}
