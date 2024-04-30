package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedList;

/**
 * 既是流，也是{@link LinkedList}
 *
 * @author wolray
 */
public class LinkedListZeroFlow<T> extends LinkedList<T> implements ListZeroFlow<T>, QueueZeroFlow<T> {

  public LinkedListZeroFlow() {

  }

  public LinkedListZeroFlow(Collection<? extends T> c) {

    super(c);
  }

}
