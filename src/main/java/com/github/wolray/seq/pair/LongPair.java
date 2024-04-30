package com.github.wolray.seq.pair;

/**
 * ��{@link Long}Ϊleft��{@link T}Ϊright��Pair
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
