package com.github.wolray.seq.pair;

/**
 * 以{@link Integer}为left，{@link T}为right的Pair
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
