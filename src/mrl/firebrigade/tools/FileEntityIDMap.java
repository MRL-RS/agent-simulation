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
 * A structure for save and restore ObservableAreas and VisibleFrom to/from file
 */
public class FileEntityIDMap extends FastMap<Integer, List<Integer>> implements Serializable {
    static final long serialVersionUID = -28728787457456789L;
}
