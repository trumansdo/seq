package com.github.wolray.zero.flow;

import com.github.wolray.zero.flow.iterators.MapItr;
import com.github.wolray.zero.flow.iterators.PickItr;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 可自我迭代的流
 *
 * @author wolray
 */
public interface ItrZeroFlow<T> extends Iterable<T>, ZeroFlow<T> {

  /**
   * 消费就是循环
   */
  @Override
  default void consume(Consumer<T> consumer) {

    forEach(consumer);
  }

  @Override
  default Optional<T> find(Predicate<T> predicate) {

    for (T t : this) {
      if (predicate.test(t)) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }

  /**
   * 将自己作为迭代对象
   */
  @Override
  default ItrZeroFlow<T> asIterable() {

    return this;
  }

  @Override
  default <E> ItrZeroFlow<E> map(Function<T, E> function) {

    return () -> ItrUtil.map(iterator(), function);
  }

  /**
   * 删除前面n个
   */
  @Override
  default ItrZeroFlow<T> drop(int n) {

    return () -> ItrUtil.drop(iterator(), n);
  }

  /**
   * 按条件删除
   */
  @Override
  default ItrZeroFlow<T> dropWhile(Predicate<T> predicate) {

    return () -> ItrUtil.dropWhile(iterator(), predicate);
  }

  /**
   * 过滤
   */
  @Override
  default ItrZeroFlow<T> filter(Predicate<T> predicate) {

    return predicate == null ? this : () -> ItrUtil.filter(iterator(), predicate);
  }

  /**
   * 按数据的类型过滤
   *
   * @return {@link ItrZeroFlow }<{@link E }>
   */
  @Override
  default <E> ItrZeroFlow<E> filterInstance(Class<E> cls) {

    return () -> new PickItr<E>() {

      final Iterator<T> iterator = iterator();

      @Override
      public E pick() {

        while (iterator.hasNext()) {
          T t = iterator.next();
          if (cls.isInstance(t)) {
            return cls.cast(t);
          }
        }
        return ZeroFlow.stop();
      }
    };
  }

  /**
   * 获取第一个
   *
   * @return {@link T }
   */
  @Override
  default T first() {

    for (T t : this) {
      return t;
    }
    return null;
  }

  /**
   * 展开二维可迭代对象
   *
   * @return {@link ItrZeroFlow }<{@link E }>
   */
  @Override
  default <E> ItrZeroFlow<E> flatIterable(Function<T, Iterable<E>> function) {

    return () -> ItrUtil.flat(iterator(), function);
  }

  @Override
  default <E> ItrZeroFlow<E> flatOptional(Function<T, Optional<E>> function) {

    return () -> ItrUtil.flatOptional(ItrUtil.map(iterator(), function));
  }

  @Override
  default T last() {

    T res = null;
    for (T t : this) {
      res = t;
    }
    return res;
  }

  /**
   * @see ItrUtil#map(Iterator, Function, int, Function)
   */
  @Override
  default <E> ItrZeroFlow<E> map(Function<T, E> function, int n, Function<T, E> substitute) {

    return n <= 0 ? map(function) : () -> ItrUtil.map(iterator(), function, n, substitute);
  }

  /**
   * @see ItrUtil#mapIndexed(Iterator, IndexObjFunction)
   */
  @Override
  default <E> ItrZeroFlow<E> mapIndexed(IndexObjFunction<T, E> function) {

    return () -> ItrUtil.mapIndexed(iterator(), function);
  }

  /**
   * null的不处理
   *
   * @return {@link ItrZeroFlow }<{@link E }>
   */
  @Override
  default <E> ItrZeroFlow<E> mapMaybe(Function<T, E> function) {

    return () -> new PickItr<E>() {

      final Iterator<T> iterator = iterator();

      @Override
      public E pick() {

        while (iterator.hasNext()) {
          T t = iterator.next();
          if (t != null) {
            return function.apply(t);
          }
        }
        return ZeroFlow.stop();
      }
    };
  }

  /**
   * 转换后的数据为空则直接跳过
   *
   * @return {@link ItrZeroFlow }<{@link E }>
   */
  @Override
  default <E> ItrZeroFlow<E> mapNotNull(Function<T, E> function) {

    return () -> new PickItr<E>() {

      final Iterator<T> iterator = iterator();

      @Override
      public E pick() {

        while (iterator.hasNext()) {
          E e = function.apply(iterator.next());
          if (e != null) {
            return e;
          }
        }
        return ZeroFlow.stop();
      }
    };
  }

  /**
   * 折叠数据，就像是海浪一样，每个浪波是之前浪波的延续
   *
   * @return {@link E }
   */
  @Override
  default <E> E fold(E init, BiFunction<E, T, E> function) {

    E acc = init;
    for (T t : this) {
      acc = function.apply(acc, t);
    }
    return acc;
  }

  /**
   * 类似Peek只消费不处理也不终止
   *
   * @return {@link ItrZeroFlow }<{@link T }>
   */
  @Override
  default ItrZeroFlow<T> onEach(Consumer<T> consumer) {

    return map(t -> {
      consumer.accept(t);
      return t;
    });
  }

  @Override
  default ItrZeroFlow<T> onEach(int n, Consumer<T> consumer) {

    return map(t -> t, n, t -> {
      consumer.accept(t);
      return t;
    });
  }

  /**
   * 在处理中折叠，就像是海浪一样，每个浪波是之前浪波的延续。 <br/> 但不同于方法{@link #fold(Object, BiFunction)}是关注最后一个浪波的结果，而它关注每个浪波的形成过程
   *
   * @return {@link ItrZeroFlow }<{@link E }>
   */
  @Override
  default <E> ItrZeroFlow<E> runningFold(E init, BiFunction<E, T, E> function) {

    return () -> new MapItr<T, E>(iterator()) {

      E acc = init;

      @Override
      public E apply(T t) {

        return acc = function.apply(acc, t);
      }
    };
  }

  /**
   * @see ItrUtil#take(Iterator, int)
   */
  @Override
  default ItrZeroFlow<T> take(int n) {

    return () -> ItrUtil.take(iterator(), n);
  }

  @Override
  default <E> ItrZeroFlow<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {

    return () -> ItrUtil.takeWhile(iterator(), function, testPrevCurr);
  }

  /**
   * @see ItrUtil#takeWhile(Iterator, Predicate)
   */
  @Override
  default ItrZeroFlow<T> takeWhile(Predicate<T> predicate) {

    return () -> ItrUtil.takeWhile(iterator(), predicate);
  }

  /**
   * 获取可以将指定数据插值压缩进流中的迭代器
   *
   * @param t
   *     每个数据之间的插入数据
   *
   * @return {@link ItrZeroFlow }<{@link T }>
   */
  default ItrZeroFlow<T> zip(T t) {

    return () -> ItrUtil.zip(iterator(), t);
  }

}
