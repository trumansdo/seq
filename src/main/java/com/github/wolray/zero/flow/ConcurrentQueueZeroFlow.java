package com.github.wolray.zero.flow;

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
