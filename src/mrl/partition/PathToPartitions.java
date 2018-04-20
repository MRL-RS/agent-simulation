package mrl.partition;

import javolution.util.FastMap;

import java.io.Serializable;
import java.util.List;

/**
 * User: roohola
 * Date: 3/25/11
 * Time: 3:46 PM
 */
public class PathToPartitions extends FastMap<PairSerialized<Integer, Integer>, PairSerialized<List<Integer>, Integer>> implements Serializable {
    static final long serialVersionUID = -8989817658719826510L;

}
