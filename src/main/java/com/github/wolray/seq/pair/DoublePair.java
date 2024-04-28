package com.github.wolray.seq.pair;

/**
 * @author wolray
 */
public class DoublePair<T> {
    public double doubleVal;
    public T it;

    public DoublePair(double doubleVal, T it) {
        this.doubleVal = doubleVal;
        this.it = it;
    }

    @Override
    public String toString() {
        return String.format("(%f,%s)", doubleVal, it);
    }
}
