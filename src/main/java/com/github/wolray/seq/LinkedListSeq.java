package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedList;

/**
 * 既是流，也是{@link LinkedList}
 *
 * @author wolray
 */
public class LinkedListSeq<T> extends LinkedList<T> implements ListSeq<T>, QueueSeq<T> {

  public LinkedListSeq() {

  }

  public LinkedListSeq(Collection<? extends T> c) {

    super(c);
  }

}
