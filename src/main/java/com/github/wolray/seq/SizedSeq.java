package com.github.wolray.seq;

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
public interface SizedSeq<T> extends ItrSeq<T> {

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
   * 当n超出容器最大下标用另一个替换处理，小于最大下标时的逻辑: {@link Seq#consume(Consumer, int, Consumer)}
   */
  @Override
  default void consume(Consumer<T> consumer, int n, Consumer<T> substitute) {

    if (n >= size()) {
      consume(substitute);
    } else {
      ItrSeq.super.consume(consumer, n, substitute);
    }
  }

  int size();

  @Override
  default SizedSeq<T> cache() {

    return this;
  }

  default boolean isNotEmpty() {

    return !isEmpty();
  }

  /**
   * 删除前面多少个数据
   *
   * @return {@link ItrSeq }<{@link T }>
   */
  @Override
  default ItrSeq<T> drop(int n) {

    return n >= size() ? Collections::emptyIterator : ItrSeq.super.drop(n);
  }

  @Override
  default <E> SizedSeq<E> map(Function<T, E> function) {

    return new SizedSeq<E>() {

      @Override
      public Iterator<E> iterator() {

        return ItrUtil.map(SizedSeq.this.iterator(), function);
      }

      @Override
      public int size() {

        return SizedSeq.this.size();
      }

      @Override
      public boolean isEmpty() {

        return SizedSeq.this.isEmpty();
      }

      @Override
      public SizedSeq<E> cache() {

        return toList();
      }
    };
  }

  /**
   * @return {@link ItrSeq }<{@link E }>
   *
   * @see ItrSeq#map(Function, int, Function)
   */
  @Override
  default <E> ItrSeq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {

    if (n >= size()) {
      return map(substitute);
    } else {
      return ItrSeq.super.map(function, n, substitute);
    }
  }


  @Override
  default ItrSeq<T> take(int n) {

    return n >= size() ? this : ItrSeq.super.take(n);
  }


}
