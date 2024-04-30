package com.github.wolray.seq;

/**
 * 所有流的Base接口
 *
 * @param <C>
 *     消费回调函数
 *
 * @author wolray
 */
public interface BaseZeroFlow<C> {

  /**
   * 可中断的
   */
  default void consumeTillStop(C consumer) {

    try {
      consume(consumer);
    } catch (StopException ignore) {
    }
  }

  /**
   * 消费，也是生产，也是通道
   */
  void consume(C consumer);

}
