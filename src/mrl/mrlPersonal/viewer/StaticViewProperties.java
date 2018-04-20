package mrl.mrlPersonal.viewer;

import rescuecore2.standard.entities.StandardEntity;

import java.util.ArrayList;

/**
 * User: roohi
 * Date: May 7, 2010
 * Time: 9:47:19 AM
 */
public class StaticViewProperties {
    public static StandardEntity selectedObject;
    public static ArrayList<StandardEntityToPaint> objectToPaint = new ArrayList<StandardEntityToPaint>();

    public static StandardEntityToPaint getPaintObject(StandardEntity entity) {
        for (StandardEntityToPaint e : objectToPaint) {
            if (e.getEntity() == entity) {
                return e;
            }
        }
        return null;
    }
}
