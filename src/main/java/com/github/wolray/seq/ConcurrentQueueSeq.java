package com.github.wolray.seq;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author wolray
 */
public class ConcurrentQueueSeq<T> extends ConcurrentLinkedQueue<T> implements QueueSeq<T> {

  public ConcurrentQueueSeq() {

  }

  public ConcurrentQueueSeq(Collection<? extends T> c) {

    super(c);
  }

}
