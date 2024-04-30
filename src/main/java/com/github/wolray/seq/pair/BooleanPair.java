package com.github.wolray.seq.pair;

/**
 * 以{@link Boolean}为left，{@link T}为right的Pair
 *
 * @author wolray
 */
public class BooleanPair<T> extends Pair<Boolean, T> {

  public boolean first;

  public T second;

  public BooleanPair(boolean first, T second) {

    super(first, second);
  }

  @Override
  public String toString() {

    return String.format("(%b,%s)", first, second);
  }

}
