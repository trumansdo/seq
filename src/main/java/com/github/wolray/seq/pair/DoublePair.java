package com.github.wolray.seq.pair;

/**
 * ��{@link Double}Ϊleft��{@link T}Ϊright��Pair
 *
 * @author wolray
 */
public class DoublePair<T> extends Pair<Double, T> {

  public DoublePair(double first, T second) {

    super(first, second);
    }

    @Override
    public String toString() {

      return String.format("(%f,%s)", first, second);
    }
}
