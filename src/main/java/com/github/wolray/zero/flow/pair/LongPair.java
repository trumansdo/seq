package com.github.wolray.zero.flow.pair;

/**
 * 以{@link Long}为left，{@link T}为right的Pair
 *
 * @author wolray
 */
public class LongPair<T> extends Pair<Long, T> {

  public LongPair(long first, T second) {

    super(first, second);
  }

  @Override
  public String toString() {

    return String.format("(%d,%s)", first, second);
  }

}
