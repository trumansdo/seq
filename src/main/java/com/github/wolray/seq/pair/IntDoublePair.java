package com.github.wolray.seq.pair;

/**
 * ��{@link Integer}Ϊleft��{@link Double}Ϊright��Pair
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
