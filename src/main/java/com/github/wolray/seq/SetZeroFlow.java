package com.github.wolray.seq;

import java.util.Set;

/**
 * 快速将{@link Set}转成流处理
 *
 * @author wolray
 */
public interface SetZeroFlow<T> extends SizedZeroFlow<T>, Set<T> {

  static <T> SetZeroFlow<T> of(Set<T> set) {

    return set instanceof SetZeroFlow ? (SetZeroFlow<T>) set : new Proxy<>(set);
  }

  class Proxy<T> extends CollectionZeroFlow.Proxy<T, Set<T>> implements SetZeroFlow<T> {

    public Proxy(Set<T> backer) {

      super(backer);
    }

  }

}
