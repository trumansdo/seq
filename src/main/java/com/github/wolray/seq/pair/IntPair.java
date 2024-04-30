package com.github.wolray.seq.pair;

/**
 * ��{@link Integer}Ϊleft��{@link T}Ϊright��Pair
 *
 * @author wolray
 */
public class IntPair<T> extends Pair<Integer, T> {

  public IntPair(int first, T second) {

    super(first, second);
    }

    @Override
    public String toString() {

      return String.format("(%d,%s)", first, second);
    }
}
