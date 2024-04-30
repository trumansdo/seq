package com.github.wolray.seq;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author wolray
 */
public class ConcurrentQueueZeroFlow<T> extends ConcurrentLinkedQueue<T> implements QueueZeroFlow<T> {

  public ConcurrentQueueZeroFlow() {

  }

  public ConcurrentQueueZeroFlow(Collection<? extends T> c) {

    super(c);
  }

}
