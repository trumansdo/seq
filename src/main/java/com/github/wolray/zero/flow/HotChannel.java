package com.github.wolray.zero.flow;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author wolray
 */
public class HotChannel<T> extends ConcurrentLinkedQueue<T> implements ZeroFlow<T>, Async.EasyLock {

  public boolean stop;

  @Override
  public void consume(Consumer<T> consumer) {

    while (true) {
      while (!isEmpty()) {
        consumer.accept(poll());
      }
      easyWait();
    }
  }

}
