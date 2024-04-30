package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * 既是流，也是{@link LinkedHashSet}
 *
 * @author wolray
 */
public class LinkedHashSetSeq<T> extends LinkedHashSet<T> implements SetSeq<T> {

  public LinkedHashSetSeq(int initialCapacity) {

    super(initialCapacity);
  }

  public LinkedHashSetSeq() {

  }

  public LinkedHashSetSeq(Collection<? extends T> c) {

    super(c);
  }

}
