package mrl.firebrigade.tools;

import javolution.util.FastMap;

import java.io.Serializable;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: MRL
 * Date: 2/12/13
 * Time: 4:52 PM
 */

/**
 * A structure for save and restore LineOfSight to/from file
 */
public class FileLineOfSight extends FastMap<Integer, List<MrlRay>> implements Serializable {
    static final long serialVersionUID = -28456987457456789L;
}
