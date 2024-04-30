package com.github.wolray.seq.pair;

/**
 * 以{@link Integer}为left，{@link Double}为right的Pair
 *
 * @author wolray
 */
public class IntDoublePair extends Pair<Integer, Double> {

  public int first;

  public double second;

  public IntDoublePair(int first, double second) {

    super(first, second);
  }

  @Override
  public String toString() {

    return String.format("(%d,%f)", first, second);
  }

}
