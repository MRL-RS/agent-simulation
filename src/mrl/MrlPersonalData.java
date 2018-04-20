package mrl;

import mrl.common.MRLConstants;
import mrl.mrlPersonal.EmptyViewerData;
import mrl.mrlPersonal.FullViewerData;
import mrl.mrlPersonal.IViewerData;


public final class MrlPersonalData {
    public final static IViewerData VIEWER_DATA;

    static {
        if (LaunchMRL.DEBUG_MODE) {
            VIEWER_DATA = new FullViewerData();
        } else {
            VIEWER_DATA = new EmptyViewerData();
        }
    }

}
