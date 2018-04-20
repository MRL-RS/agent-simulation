package mrl.la;

import java.io.Serializable;
import java.util.*;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 4:27:35 PM
 */
public class Probability extends HashMap<Action, ValueRange> implements Serializable {
    public static final long serialVersionUID = -1132675752735363700L;
    protected Random random;

    public Probability(long randomSeed) {
        random = new Random(randomSeed);
    }

    public void updateRange(Action action, double percent, boolean increase) {
        ValueRange valueRange = get(action);

        List<ValueRange> valueRangeList = new ArrayList<ValueRange>(values());
        Collections.sort(valueRangeList);

        if (increase) {

            double growLength = valueRange.getLength() * percent;
            double decreaseLength = growLength / ((double) values().size() - 1.0);

            for (ValueRange range : valueRangeList) {
                if (!range.equals(valueRange)) {// for
                    range.setLength(range.getLength() - decreaseLength);
                } else {
                    range.setLength(range.getLength() + growLength);
                }
            }

        } else {
            double decreaseLength = valueRange.getLength() * percent;
            double growLength = decreaseLength / ((double) values().size() - 1.0);

            for (ValueRange range : valueRangeList) {
                if (!range.equals(valueRange)) {// for
                    range.setLength(range.getLength() + growLength);
                } else {
                    range.setLength(range.getLength() - decreaseLength);
                }
            }
        }

        for (int i = 0; i < valueRangeList.size(); i++) {
            ValueRange range = valueRangeList.get(i);
            if (i == 0) {
                range.setLower(0);
                range.setHigher(range.getLength());
            } else {
                range.setLower(valueRangeList.get(i - 1).getHigher());
                range.setHigher(range.getLength() + range.getLower());
            }
            range.computeLength();
        }

    }

    public Action chooseAction() {
        int maxDiv = 100000;
        int rand = Math.abs(random.nextInt(maxDiv));
        double randDouble = (double) rand / (double) maxDiv;
        for (Action action : keySet()) {
            if (get(action).isInRage(randDouble)) {

                return action;
            }
        }
        throw new RuntimeException("Something wrong with the value range. random number:" + randDouble);
    }

    @Override
    public String toString() {
        String s = "[Prob:";
        for (ValueRange range : values()) {
            s += range;
        }
        return s + "]";
    }
}
