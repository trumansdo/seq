package com.github.wolray.zero.flow;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 可获取流中数据数量的流
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public interface SizedZeroFlow<T> extends ItrZeroFlow<T> {

  @Override
  default int sizeOrDefault() {

    return size();
  }

  @Override
  default int count() {

    return size();
  }

  boolean isEmpty();

  /**
   * 当n超出容器最大下标用另一个替换处理，小于最大下标时的逻辑: {@link ZeroFlow#consume(Consumer, int, Consumer)}
   */
  @Override
  default void consume(Consumer<T> consumer, int n, Consumer<T> substitute) {

    if (n >= size()) {
      consume(substitute);
    } else {
      ItrZeroFlow.super.consume(consumer, n, substitute);
    }
  }

  int size();

  @Override
  default SizedZeroFlow<T> cache() {

    return this;
  }

  default boolean isNotEmpty() {

    return !isEmpty();
  }

  /**
   * 删除前面多少个数据
   *
   * @return {@link ItrZeroFlow }<{@link T }>
   */
  @Override
  default ItrZeroFlow<T> drop(int n) {

    return n >= size() ? Collections::emptyIterator : ItrZeroFlow.super.drop(n);
  }

  @Override
  default <E> SizedZeroFlow<E> map(Function<T, E> function) {

    return new SizedZeroFlow<E>() {

      @Override
      public Iterator<E> iterator() {

        return ItrUtil.map(SizedZeroFlow.this.iterator(), function);
      }

      @Override
      public int size() {

        return SizedZeroFlow.this.size();
      }

      @Override
      public boolean isEmpty() {

        return SizedZeroFlow.this.isEmpty();
      }

      @Override
      public SizedZeroFlow<E> cache() {

        return toList();
      }
    };
  }

  /**
   * @return {@link ItrZeroFlow }<{@link E }>
   *
   * @see ItrZeroFlow#map(Function, int, Function)
   */
  @Override
  default <E> ItrZeroFlow<E> map(Function<T, E> function, int n, Function<T, E> substitute) {

    if (n >= size()) {
      return map(substitute);
    } else {
      return ItrZeroFlow.super.map(function, n, substitute);
    }
  }


  @Override
  default ItrZeroFlow<T> take(int n) {

    return n >= size() ? this : ItrZeroFlow.super.take(n);
  }


}
