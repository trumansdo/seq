package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * 既是流，也是{@link LinkedHashSet}
 *
 * @author wolray
 */
public class LinkedHashSetZeroFlow<T> extends LinkedHashSet<T> implements SetZeroFlow<T> {

  public LinkedHashSetZeroFlow(int initialCapacity) {

    super(initialCapacity);
  }

  public LinkedHashSetZeroFlow() {

  }

  public LinkedHashSetZeroFlow(Collection<? extends T> c) {

    super(c);
  }

}
