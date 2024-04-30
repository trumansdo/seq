package com.github.wolray.seq;

import com.github.wolray.seq.pair.DoublePair;
import com.github.wolray.seq.pair.IntPair;
import com.github.wolray.seq.pair.LongPair;
import com.github.wolray.seq.pair.Pair;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

/**
 * 对标{@link Collector}接口，将{@link T} 转换成 {@link V}
 *
 * @param <T>
 *     原值，不必是数据，也可以是函数
 * @param <V>
 *     结果值，不必是数据，也可以是函数
 *
 * @author wolray
 */
public interface Reducer<T, V> {

  /**
   * 平均数
   *
   * @return {@link Transducer }<{@link T }, {@link ? }, {@link Double }>
   */
  static <T> Transducer<T, ?, Double> average(ToDoubleFunction<T> function) {

    return average(function, null);
  }

  /**
   * 加权平均
   *
   * @param weightFunction
   *     为空时默认权重为1，就是求平均数
   *
   * @return {@link Transducer }<{@link T }, {@link ? }, {@link Double }>
   */
  static <T> Transducer<T, ?, Double> average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {

    BiConsumer<double[], T> biConsumer;
    if (weightFunction != null) {
      biConsumer = (a, t) -> {
        double v = function.applyAsDouble(t);
        double w = weightFunction.applyAsDouble(t);
        a[0] += v * w; // 数据*权重
        a[1] += w; // 权重
      };
    } else {
      biConsumer = (a, t) -> {
        a[0] += function.applyAsDouble(t);
        a[1] += 1; // 权重
      };
    }
    return Transducer.of(() -> new double[2], biConsumer, a -> a[1] != 0 ? a[0] / a[1] : 0);
  }

  static <T, C extends Collection<T>> Reducer<T, C> collect(Supplier<C> des) {

    return of(des, Collection::add);
  }

  static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator) {

    return of(supplier, accumulator, null);
  }

  static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator, Consumer<V> finisher) {

    return new Reducer<T, V>() {

      @Override
      public BiConsumer<V, T> accumulator() {

        return accumulator;
      }

      @Override
      public Supplier<V> supplier() {

        return supplier;
      }

      @Override
      public Consumer<V> finisher() {

        return finisher;
      }
    };
  }

  static <T> Transducer<T, ?, Integer> count() {

    return Transducer.of(() -> new int[1], (a, t) -> a[0]++, a -> a[0]);
  }

  /**
   * 不满足条件的数量
   *
   * @return {@link Transducer }<{@link T }, {@link ? }, {@link Integer }>
   */
  static <T> Transducer<T, ?, Integer> countNot(Predicate<T> predicate) {

    return count(predicate.negate());
  }

  /**
   * 满足条件的数量
   *
   * @return {@link Transducer }<{@link T }, {@link ? }, {@link Integer }>
   */
  static <T> Transducer<T, ?, Integer> count(Predicate<T> predicate) {

    return Transducer.of(() -> new int[1], (a, t) -> {
      if (predicate.test(t)) {
        a[0]++;
      }
    }, a -> a[0]);
  }

  static <T> Reducer<T, ArrayListSeq<T>> filtering(Predicate<T> predicate) {

    return filtering(predicate, toList());
  }

  static <T, V> Reducer<T, V> filtering(Predicate<T> predicate, Reducer<T, V> reducer) {

    BiConsumer<V, T> accumulator = reducer.accumulator();
    return of(reducer.supplier(), (v, t) -> {
      if (predicate.test(t)) {
        accumulator.accept(v, t);
      }
    }, reducer.finisher());
  }

  static <T> Reducer<T, ArrayListSeq<T>> toList() {

    return of(ArrayListSeq::new, ArrayListSeq::add);
  }

  static <T, V, E> Transducer<T, V, E> filtering(Predicate<T> predicate, Transducer<T, V, E> transducer) {

    return Transducer.of(filtering(predicate, transducer.reducer()), transducer.transformer());
  }

  static <T, K, V, E> Transducer<T, ?, MapSeq<K, E>> groupBy(Function<T, K> toKey, Transducer<T, V, E> transducer) {

    return Transducer.of(groupBy(toKey, transducer.reducer()), m -> m.replaceValue(transducer.transformer()));
  }

  static <T, K, V> Reducer<T, MapSeq<K, V>> groupBy(Function<T, K> toKey, Reducer<T, V> reducer) {

    Supplier<V>      supplier    = reducer.supplier();
    BiConsumer<V, T> accumulator = reducer.accumulator();
    Consumer<V>      finisher    = reducer.finisher();
    return of(
        MapSeq::hash,
        (m, t) -> {
          // 对于将流中的原数据加入已有的map中的处理
          accumulator.accept(
              // 如果不存在这个key，就用supplier生成一个对应value
              m.computeIfAbsent(toKey.apply(t), k -> supplier.get()),
              t
          );
        },
        finisher == null ? null : m -> m.justValues().consume(finisher)
    );
  }

  static <T> Transducer<T, ?, String> join(String sep, Function<T, String> function) {

    return Transducer.of(() -> new StringJoiner(sep), (j, t) -> j.add(function.apply(t)), StringJoiner::toString);
  }

  static <T, E> Reducer<T, ArrayListSeq<E>> mapping(Function<T, E> mapper) {

    return mapping(mapper, toList());
  }

  static <T, E, V> Reducer<T, V> mapping(Function<T, E> mapper, Reducer<E, V> reducer) {

    BiConsumer<V, E> accumulator = reducer.accumulator();
    return of(reducer.supplier(), (v, t) -> {
      E e = mapper.apply(t);
      accumulator.accept(v, e);
    }, reducer.finisher());
  }

  static <T, R, V, E> Transducer<T, V, E> mapping(Function<T, R> mapper, Transducer<R, V, E> transducer) {

    return Transducer.of(mapping(mapper, transducer.reducer()), transducer.transformer());
  }

  static <T> Transducer<T, ?, T> max(Comparator<T> comparator) {

    return Transducer.of((t1, t2) -> comparator.compare(t1, t2) < 0 ? t2 : t1);
  }

  static <T, V extends Comparable<V>> Reducer<T, AtomicReference<Pair<T, V>>> maxAtomicBy(
      V initValue, Function<T, V> function
  ) {

    return of(() -> new AtomicReference<>(new Pair<>(null, initValue)), (ref, t) -> {
      V v = function.apply(t);
      ref.updateAndGet(p -> {
        if (p.second.compareTo(v) < 0) {
          p.set(t, v);
        }
        return p;
      });
    });
  }

  static <T, V extends Comparable<V>> Reducer<T, Pair<T, V>> maxBy(Function<T, V> function) {

    return of(() -> new Pair<>(null, null), (p, t) -> {
      V v = function.apply(t);
      if (p.second == null || p.second.compareTo(v) < 0) {
        p.set(t, v);
      }
    });
  }

  static <T> Reducer<T, IntPair<T>> maxByInt(ToIntFunction<T> function) {

    return of(() -> new IntPair<>(0, null), (p, t) -> {
      int v = function.applyAsInt(t);
      if (p.second == null || p.first < v) {
        p.first  = v;
        p.second = t;
      }
    });
  }

  static <T> Reducer<T, DoublePair<T>> maxByDouble(ToDoubleFunction<T> function) {

    return of(() -> new DoublePair<>(0, null), (p, t) -> {
      double v = function.applyAsDouble(t);
      if (p.second == null || p.first < v) {
        p.first  = v;
        p.second = t;
      }
    });
  }

  static <T> Reducer<T, LongPair<T>> maxByLong(ToLongFunction<T> function) {

    return of(() -> new LongPair<>(0, null), (p, t) -> {
      long v = function.applyAsLong(t);
      if (p.second == null || p.first < v) {
        p.first  = v;
        p.second = t;
      }
    });
  }

  static <T> Transducer<T, ?, T> min(Comparator<T> comparator) {

    return Transducer.of((t1, t2) -> comparator.compare(t1, t2) > 0 ? t2 : t1);
  }

  static <T, V extends Comparable<V>> Reducer<T, AtomicReference<Pair<T, V>>> minAtomicBy(
      V initValue, Function<T, V> function
  ) {

    return of(() -> new AtomicReference<>(new Pair<>(null, initValue)), (ref, t) -> {
      V v = function.apply(t);
      ref.updateAndGet(p -> {
        if (p.second.compareTo(v) > 0) {
          p.set(t, v);
        }
        return p;
      });
    });
  }

  static <T, V extends Comparable<V>> Reducer<T, Pair<T, V>> minBy(Function<T, V> function) {

    return of(() -> new Pair<>(null, null), (p, t) -> {
      V v = function.apply(t);
      if (p.second == null || p.second.compareTo(v) > 0) {
        p.set(t, v);
      }
    });
  }

  static <T> Reducer<T, IntPair<T>> minByInt(ToIntFunction<T> function) {

    return of(() -> new IntPair<>(0, null), (p, t) -> {
      int v = function.applyAsInt(t);
      if (p.second == null || p.first > v) {
        p.first  = v;
        p.second = t;
      }
    });
  }

  static <T> Reducer<T, DoublePair<T>> minByDouble(ToDoubleFunction<T> function) {

    return of(() -> new DoublePair<>(0, null), (p, t) -> {
      double v = function.applyAsDouble(t);
      if (p.second == null || p.first > v) {
        p.first  = v;
        p.second = t;
      }
    });
  }

  static <T> Reducer<T, LongPair<T>> minByLong(ToLongFunction<T> function) {

    return of(() -> new LongPair<>(0, null), (p, t) -> {
      long v = function.applyAsLong(t);
      if (p.second == null || p.first > v) {
        p.first  = v;
        p.second = t;
      }
    });
  }

  static <T> Reducer<T, Pair<BatchedSeq<T>, BatchedSeq<T>>> partition(Predicate<T> predicate) {

    return partition(predicate, toBatched());
  }

  static <T, V> Reducer<T, Pair<V, V>> partition(Predicate<T> predicate, Reducer<T, V> reducer) {

    BiConsumer<V, T> accumulator = reducer.accumulator();
    Supplier<V>      supplier    = reducer.supplier();
    Consumer<V>      finisher    = reducer.finisher();
    return of(
        () -> new Pair<>(supplier.get(), supplier.get()),
        (p, t) -> accumulator.accept(predicate.test(t) ? p.first : p.second, t),
        finisher == null ? null : p -> {
          finisher.accept(p.first);
          finisher.accept(p.second);
        }
    );
  }

  static <T> Reducer<T, BatchedSeq<T>> toBatched() {

    return of(BatchedSeq::new, BatchedSeq::add);
  }

  static <T, V, R> Transducer<T, ?, Pair<R, R>> partition(Predicate<T> predicate, Transducer<T, V, R> transducer) {

    Function<V, R> mapper = transducer.transformer();
    return Transducer.of(
        partition(predicate, transducer.reducer()),
        p -> new Pair<>(mapper.apply(p.first), mapper.apply(p.second))
    );
  }

  static <T> Reducer<T, ArrayListSeq<T>> reverse() {

    return Reducer.<T>toList().then(Collections::reverse);
  }

  static <T> Reducer<T, ArrayListSeq<T>> sort() {

    return sort((Comparator<T>) null);
  }

  static <T> Reducer<T, ArrayListSeq<T>> sort(Comparator<T> comparator) {

    return Reducer.<T>toList().then(ts -> ts.sort(comparator));
  }

  static <T, V extends Comparable<V>> Reducer<T, ArrayListSeq<T>> sort(Function<T, V> function) {

    return sort(Comparator.comparing(function));
  }

  static <T> Reducer<T, ArrayListSeq<T>> sortDesc() {

    return sort(Collections.reverseOrder());
  }

  static <T> Reducer<T, ArrayListSeq<T>> sortDesc(Comparator<T> comparator) {

    return sort(comparator.reversed());
  }

  static <T, V extends Comparable<V>> Reducer<T, ArrayListSeq<T>> sortDesc(Function<T, V> function) {

    return sort(Comparator.comparing(function).reversed());
  }

  static <T> Transducer<T, ?, Double> sum(ToDoubleFunction<T> function) {

    return Transducer.of(() -> new double[1], (a, t) -> a[0] += function.applyAsDouble(t), a -> a[0]);
  }

  static <T> Transducer<T, ?, Integer> sumInt(ToIntFunction<T> function) {

    return Transducer.of(() -> new int[1], (a, t) -> a[0] += function.applyAsInt(t), a -> a[0]);
  }

  static <T> Transducer<T, ?, Long> sumLong(ToLongFunction<T> function) {

    return Transducer.of(() -> new long[1], (a, t) -> a[0] += function.applyAsLong(t), a -> a[0]);
  }

  static <T> Reducer<T, ConcurrentQueueSeq<T>> toConcurrentQueue() {

    return of(ConcurrentQueueSeq::new, ConcurrentQueueSeq::add);
  }

  static <T> Reducer<T, LinkedListSeq<T>> toLinkedList() {

    return of(LinkedListSeq::new, LinkedListSeq::add);
  }

  static <T> Reducer<T, ArrayListSeq<T>> toList(int initialCapacity) {

    return of(() -> new ArrayListSeq<>(initialCapacity), ArrayListSeq::add);
  }

  static <T, K, V> Reducer<T, MapSeq<K, V>> toMap(Function<T, K> toKey, Function<T, V> toValue) {

    return of(MapSeq::hash, (m, t) -> m.put(toKey.apply(t), toValue.apply(t)));
  }

  static <T, K, V> Reducer<T, MapSeq<K, V>> toMap(
      Supplier<Map<K, V>> mapSupplier, Function<T, K> toKey, Function<T, V> toValue
  ) {

    return of(() -> MapSeq.of(mapSupplier.get()), (m, t) -> m.put(toKey.apply(t), toValue.apply(t)));
  }

  static <T, K> Reducer<T, MapSeq<K, T>> toMapBy(Function<T, K> toKey) {

    return toMapBy(LinkedHashMap::new, toKey);
  }

  static <T, K> Reducer<T, MapSeq<K, T>> toMapBy(Supplier<Map<K, T>> mapSupplier, Function<T, K> toKey) {

    return of(() -> MapSeq.of(mapSupplier.get()), (m, t) -> m.put(toKey.apply(t), t));
  }

  static <T, V> Reducer<T, MapSeq<T, V>> toMapWith(Function<T, V> toValue) {

    return toMapWith(LinkedHashMap::new, toValue);
  }

  static <T, V> Reducer<T, MapSeq<T, V>> toMapWith(Supplier<Map<T, V>> mapSupplier, Function<T, V> toValue) {

    return of(() -> MapSeq.of(mapSupplier.get()), (m, t) -> m.put(t, toValue.apply(t)));
  }

  static <T> Reducer<T, SetSeq<T>> toSet() {

    return of(LinkedHashSetSeq::new, Set::add);
  }

  static <T> Reducer<T, SetSeq<T>> toSet(int initialCapacity) {

    return of(() -> new LinkedHashSetSeq<>(initialCapacity), Set::add);
  }

  /**
   * 等价{@link Collector#accumulator()} ()}
   *
   * @see Collector#supplier()
   */
  BiConsumer<V, T> accumulator();

  /**
   * 等价{@link Collector#supplier()}
   *
   * @see Collector#supplier()
   */
  Supplier<V> supplier();

  /**
   * 等价{@link Collector#finisher()} ()}
   *
   * @see Collector#finisher() ()
   */
  Consumer<V> finisher();

  default Reducer<T, V> then(Consumer<V> action) {

    Consumer<V> finisher = finisher();
    return of(supplier(), accumulator(), finisher == null ? action : finisher.andThen(action));
  }

}
