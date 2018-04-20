package mrl.la;

/**
 * User: roohi
 * Date: Sep 30, 2010
 * Time: 4:29:57 PM
 */
public class ValueRange implements Comparable<ValueRange> {
    int index;
    double lower;
    double higher;
    double length;

    public ValueRange(int index, double length) {
        this.index = index;
        this.length = length;
    }

    public ValueRange(int index, double lower, double higher) {
        if (index <= 0) {
            throw new RuntimeException("index must be more than Zero");
        }
        if (higher < lower) {
            throw new RuntimeException("Lower is more than Higher");
        }
        this.lower = lower;
        this.higher = higher;
        this.index = index;
        computeLength();
    }

    public void computeLength() {
        this.length = higher - lower;
    }

    public double getLengthBounded() {
        return higher - lower;
    }

    public boolean isInRage(double value) {
        return value >= lower && value <= higher;
    }

    public double getLower() {
        return lower;
    }

    public double getHigher() {
        return higher;
    }

    public void setLower(double lower) {
        this.lower = filterDouble(lower);
    }

    public void setHigher(double higher) {
        this.higher = filterDouble(higher);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = filterDouble(length);
    }

    public void update(ValueRange valueRange) {
        this.index = valueRange.index;
        this.lower = valueRange.lower;
        this.higher = valueRange.higher;
        this.length = valueRange.length;
    }

    @Override
    public int compareTo(ValueRange valueRange) {
        if (valueRange.index < index)
            return 1;
        if (valueRange.index == index)
            return 0;
        return -1;
    }

    @Override
    public String toString() {
        return "[Range:" + lower + "," + higher + "]";
    }

    private double filterDouble(double d) {
        if (d > 1)
            return 1;
        if (d < 0)
            return 0;
        return d;

    }
}
