package com.github.wolray.seq;

/**
 * 所有流的Base接口
 *
 * @author wolray
 */
public interface BaseSeq<C> {

  void consume(C consumer);

  default void consumeTillStop(C consumer) {

    try {
      consume(consumer);
    } catch (StopException ignore) {
    }
  }

}
