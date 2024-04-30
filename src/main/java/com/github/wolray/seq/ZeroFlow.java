package com.github.wolray.seq;

import com.github.wolray.seq.iterators.PickItr;
import com.github.wolray.seq.pair.BooleanPair;
import com.github.wolray.seq.pair.DoublePair;
import com.github.wolray.seq.pair.IntPair;
import com.github.wolray.seq.pair.LongPair;
import com.github.wolray.seq.pair.Pair;
import com.github.wolray.seq.pair.PairZeroFlow;
import com.github.wolray.seq.triple.TripleConsumer;
import com.github.wolray.seq.triple.TripleZeroFlow;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public interface ZeroFlow<T> extends BaseZeroFlow<Consumer<T>> {

  //---------------------------核心方法----------------------------

  static <T> ZeroFlow<T> unit(T t) {

    return c -> c.accept(t);
  }

  static <T> ItrZeroFlow<T> gen(Supplier<T> supplier) {

    return () -> new Iterator<T>() {

      @Override
      public boolean hasNext() {

        return true;
      }

      @Override
      public T next() {

        return supplier.get();
      }
    };
  }

  static <T> ZeroFlow<T> gen(T seed, UnaryOperator<T> operator) {

    return c -> {
      T t = seed;
      c.accept(t);
      while (true) {
        c.accept(t = operator.apply(t));
      }
    };
  }

  static <T> ZeroFlow<T> gen(T seed1, T seed2, BinaryOperator<T> operator) {

    return c -> {
      T t1 = seed1, t2 = seed2;
      c.accept(t1);
      c.accept(t2);
      while (true) {
        c.accept(t2 = operator.apply(t1, t1 = t2));
      }
    };
  }

  static <K, V> MapZeroFlow<K, V> of(Map<K, V> map) {

    return MapZeroFlow.of(map);
  }

  static <T> ZeroFlow<T> of(Optional<T> optional) {

    return optional::ifPresent;
  }

  @SafeVarargs
  static <T> ZeroFlow<T> of(T... ts) {

    return of(Arrays.asList(ts));
  }

  static <T> ZeroFlow<T> of(Iterable<T> iterable) {

    return iterable instanceof ItrZeroFlow ? (ItrZeroFlow<T>) iterable : (ItrZeroFlow<T>) iterable::iterator;
  }

  static ZeroFlow<Object> ofJson(Object node) {

    return ZeroFlow.ofTree(node, n -> c -> {
      if (n instanceof Iterable) {
        ((Iterable<?>) n).forEach(c);
      } else if (n instanceof Map) {
        ((Map<?, ?>) n).values().forEach(c);
      }
    });
  }

  static <N> ZeroFlow<N> ofTree(N node, Function<N, ZeroFlow<N>> sub) {

    return ExpandSeq.of(sub).toSeq(node);
  }

  static <N> ZeroFlow<N> ofTree(int maxDepth, N node, Function<N, ZeroFlow<N>> sub) {

    return ExpandSeq.of(sub).toSeq(node, maxDepth);
  }

  static <T> ItrZeroFlow<T> repeat(int n, T t) {

    return () -> new Iterator<T>() {

      int i = n;

      @Override
      public boolean hasNext() {

        return i > 0;
      }

      @Override
      public T next() {

        i--;
        return t;
      }
    };
  }

  static <T> ItrZeroFlow<T> flatIterable(Iterable<Optional<T>> iterable) {

    return () -> ItrUtil.flatOptional(iterable.iterator());
  }

  @SafeVarargs
  static <T> ItrZeroFlow<T> flatIterable(Iterable<T>... iterables) {

    return () -> ItrUtil.flat(Arrays.asList(iterables).iterator());
  }

  static <T> ItrZeroFlow<T> tillNull(Supplier<T> supplier) {

    return () -> new PickItr<T>() {

      @Override
      public T pick() {

        T t = supplier.get();
        return t != null ? t : ZeroFlow.stop();
      }
    };
  }

  static <T> T stop() {

    throw StopException.INSTANCE;
  }

  static <T> ZeroFlow<T> flat(ZeroFlow<Optional<T>> seq) {

    return c -> seq.consume(o -> o.ifPresent(c));
  }

  @SafeVarargs
  static <T> ZeroFlow<T> flat(ZeroFlow<T>... seq) {

    return c -> {
      for (ZeroFlow<T> s : seq) {
        s.consume(c);
      }
    };
  }

  static ItrZeroFlow<Matcher> match(String s, Pattern pattern) {

    return () -> new Iterator<Matcher>() {

      final Matcher matcher = pattern.matcher(s);

      @Override
      public boolean hasNext() {

        return matcher.find();
      }

      @Override
      public Matcher next() {

        return matcher;
      }
    };
  }

  @SuppressWarnings("unchecked")
  static <T> Consumer<T> nothing() {

    return (Consumer<T>) Empty.nothing;
  }

  //--------------------------stream的快速构造操作-----------------------------
  @SuppressWarnings("unchecked")
  static <T> ZeroFlow<T> empty() {

    return (ZeroFlow<T>) Empty.emptySeq;
  }

  /**
   * map/reduce理论中的核心reduce方法
   */
  default <E, V> E reduce(Reducer<T, V> reducer, Function<V, E> transformer) {

    return transformer.apply(reduce(reducer));
  }

  /**
   * map/reduce理论中的核心reduce方法
   */
  default <E> E reduce(Reducer<T, E> reducer) {

    E                des         = reducer.supplier().get();
    BiConsumer<E, T> accumulator = reducer.accumulator();
    consume(t -> accumulator.accept(des, t));
    Consumer<E> finisher = reducer.finisher();
    if (finisher != null) {
      finisher.accept(des);
    }
    return des;
  }

  default boolean all(Predicate<T> predicate) {

    return !find(predicate.negate()).isPresent();
  }

  //--------------------------stream的中间处理操作-----------------------------

  default Optional<T> find(Predicate<T> predicate) {

    Mutable<T> m = new Mutable<>(null);
    consumeTillStop(t -> {
      if (predicate.test(t)) {
        m.set(t);
        stop();
      }
    });
    return m.toOptional();
  }

  default boolean anyNot(Predicate<T> predicate) {

    return any(predicate.negate());
  }

  default boolean any(Predicate<T> predicate) {

    return find(predicate).isPresent();
  }

  default ZeroFlow<T> append(T t) {

    return c -> {
      consume(c);
      c.accept(t);
    };
  }

  @SuppressWarnings("unchecked")
  default ZeroFlow<T> append(T... t) {

    return c -> {
      consume(c);
      for (T x : t) {
        c.accept(x);
      }
    };
  }

  default ZeroFlow<T> appendAll(Iterable<T> iterable) {

    return c -> {
      consume(c);
      iterable.forEach(c);
    };
  }

  default ZeroFlow<T> appendWith(ZeroFlow<T> seq) {

    return c -> {
      consume(c);
      seq.consume(c);
    };
  }

  default ItrZeroFlow<T> asIterable() {

    return toBatched();
  }

  default BatchedZeroFlow<T> toBatched() {

    return reduce(new BatchedZeroFlow<>(), BatchedZeroFlow::add);
  }

  /**
   * map/reduce理论中的核心reduce方法
   *
   * @param des
   *     原始值
   * @param accumulator
   *     对值的收束
   */
  default <E> E reduce(E des, BiConsumer<E, T> accumulator) {

    consume(t -> accumulator.accept(des, t));
    return des;
  }

  default double average(ToDoubleFunction<T> function) {

    return average(function, null);
  }

  /**
   * 加权平均
   *
   * @return double
   */
  default double average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {

    return reduce(Reducer.average(function, weightFunction));
  }

  /**
   * map/reduce理论中的核心reduce方法
   */
  default <E, V> E reduce(Transducer<T, V, E> transducer) {

    return transducer.transformer().apply(reduce(transducer.reducer()));
  }

  default ZeroFlow<ArrayListZeroFlow<T>> chunked(int size) {

    return chunked(size, Reducer.toList(size));
  }

  default <V> ZeroFlow<V> chunked(int size, Reducer<T, V> reducer) {

    if (size <= 0) {
      throw new IllegalArgumentException("non-positive size");
    }
    Supplier<V>      supplier    = reducer.supplier();
    BiConsumer<V, T> accumulator = reducer.accumulator();
    Consumer<V>      finisher    = reducer.finisher();
    return c -> {
      IntPair<V> intPair = new IntPair<>(0, supplier.get());
      reduce(intPair, (p, t) -> {
        if (p.first == size) {
          if (finisher != null) {
            finisher.accept(p.second);
          }
          c.accept(p.second);
          p.second = supplier.get();
          p.first  = 0;
        }
        accumulator.accept(p.second, t);
        p.first++;
      });
      if (intPair.second != null) {
        c.accept(intPair.second);
      }
    };
  }

  default <V, E> ZeroFlow<E> chunked(int size, Transducer<T, V, E> transducer) {

    return chunked(size, transducer.reducer()).map(transducer.transformer());
  }

  /**
   * map/reduce理论中的核心map方法
   */
  default <E> ZeroFlow<E> map(Function<T, E> function) {

    return c -> consume(t -> c.accept(function.apply(t)));
  }

  default ZeroFlow<T> circle() {

    return c -> {
      while (true) {
        consume(c);
      }
    };
  }

  default <C extends Collection<T>> C collectBy(IntFunction<C> constructor) {

    return reduce(constructor.apply(sizeOrDefault()), Collection::add);
  }

  default int sizeOrDefault() {

    return 10;
  }

  default void consumeIndexedTillStop(IndexObjConsumer<T> consumer) {

    int[] a = {0};
    consumeTillStop(t -> consumer.accept(a[0]++, t));
  }

  default int count() {

    return reduce(Reducer.count());
  }

  default int count(Predicate<T> predicate) {

    return reduce(Reducer.count(predicate));
  }

  default int countNot(Predicate<T> predicate) {

    return reduce(Reducer.countNot(predicate));
  }

  default ZeroFlow<T> distinct() {

    return c -> reduce(new HashSet<>(), (set, t) -> {
      if (set.add(t)) {
        c.accept(t);
      }
    });
  }

  default <E> ZeroFlow<T> distinctBy(Function<T, E> function) {

    return c -> reduce(new HashSet<>(), (set, t) -> {
      if (set.add(function.apply(t))) {
        c.accept(t);
      }
    });
  }

  default ZeroFlow<T> drop(int n) {

    return n <= 0 ? this : partial(n, nothing());
  }

  default ZeroFlow<T> partial(int n, Consumer<T> substitute) {

    return c -> consume(c, n, substitute);
  }

  /**
   * 按以0为分界，给定的n一直自减，直到小于等于0后替换另一个处理逻辑
   *
   * @param consumer
   *     小于等于0的处理
   * @param n
   *     n会一直自减
   * @param substitute
   *     大于0的处理
   */
  default void consume(Consumer<T> consumer, int n, Consumer<T> substitute) {

    if (n > 0) {
      int[] a = {n - 1};
      consume(t -> {
        if (a[0] < 0) {
          // 小于等于0的处理
          consumer.accept(t);
        } else {
          // 大于0的处理
          a[0]--;
          substitute.accept(t);
        }
      });
    } else {
      // 小于等于0的处理
      consume(consumer);
    }
  }

  default ZeroFlow<T> dropWhile(Predicate<T> predicate) {

    return c -> foldBoolean(false, (b, t) -> {
      if (b || !predicate.test(t)) {
        c.accept(t);
        return true;
      }
      return false;
    });
  }

  default boolean foldBoolean(boolean init, BooleanObjToBoolean<T> function) {

    boolean[] a = {init};
    consume(t -> a[0] = function.apply(a[0], t));
    return a[0];
  }

  default ZeroFlow<T> duplicateAll(int times) {

    return c -> {
      for (int i = 0; i < times; i++) {
        consume(c);
      }
    };
  }

  default ZeroFlow<T> duplicateEach(int times) {

    return c -> consume(t -> {
      for (int i = 0; i < times; i++) {
        c.accept(t);
      }
    });
  }

  default ZeroFlow<T> duplicateIf(int times, Predicate<T> predicate) {

    return c -> consume(t -> {
      if (predicate.test(t)) {
        for (int i = 0; i < times; i++) {
          c.accept(t);
        }
      } else {
        c.accept(t);
      }
    });
  }

  default ZeroFlow<T> filter(int n, Predicate<T> predicate) {

    return predicate == null ? this : c -> consume(c, n, t -> {
      if (predicate.test(t)) {
        c.accept(t);
      }
    });
  }

  default ZeroFlow<T> filterIn(Collection<T> collection) {

    return collection == null ? this : filter(collection::contains);
  }

  default ZeroFlow<T> filter(Predicate<T> predicate) {

    return predicate == null ? this : c -> consume(t -> {
      if (predicate.test(t)) {
        c.accept(t);
      }
    });
  }

  default ZeroFlow<T> filterIn(Map<T, ?> map) {

    return map == null ? this : filter(map::containsKey);
  }

  default ZeroFlow<T> filterIndexed(IndexObjPredicate<T> predicate) {

    return predicate == null ? this : c -> consumeIndexed((i, t) -> {
      if (predicate.test(i, t)) {
        c.accept(t);
      }
    });
  }

  default void consumeIndexed(IndexObjConsumer<T> consumer) {

    int[] a = {0};
    consume(t -> consumer.accept(a[0]++, t));
  }

  default <E> ZeroFlow<E> filterInstance(Class<E> cls) {

    return c -> consume(t -> {
      if (cls.isInstance(t)) {
        c.accept(cls.cast(t));
      }
    });
  }

  default ZeroFlow<T> filterNotIn(Collection<T> collection) {

    return collection == null ? this : filterNot(collection::contains);
  }

  default ZeroFlow<T> filterNot(Predicate<T> predicate) {

    return predicate == null ? this : filter(predicate.negate());
  }

  default ZeroFlow<T> filterNotIn(Map<T, ?> map) {

    return map == null ? this : filterNot(map::containsKey);
  }

  default ZeroFlow<T> filterNotNull() {

    return filter(Objects::nonNull);
  }

  default Optional<T> findDuplicate() {

    Set<T> set = new HashSet<>(sizeOrDefault());
    return find(t -> !set.add(t));
  }

  default Optional<T> findNot(Predicate<T> predicate) {

    return find(predicate.negate());
  }

  default T first() {

    Mutable<T> m = new Mutable<>(null);
    consumeTillStop(t -> {
      m.it = t;
      stop();
    });
    return m.it;
  }

  default Optional<T> firstMaybe() {

    return find(t -> true);
  }

  default <E> ZeroFlow<E> flatIterable(Function<T, Iterable<E>> function) {

    return c -> consume(t -> function.apply(t).forEach(c));
  }

  default <E> ZeroFlow<E> flatMap(Function<T, ZeroFlow<E>> function) {

    return c -> consume(t -> function.apply(t).consume(c));
  }

  default <E> ZeroFlow<E> flatOptional(Function<T, Optional<E>> function) {

    return c -> consume(t -> function.apply(t).ifPresent(c));
  }

  default double foldDouble(double init, DoubleObjToDouble<T> function) {

    double[] a = {init};
    consume(t -> a[0] = function.apply(a[0], t));
    return a[0];
  }

  default long foldLong(long init, LongObjToLong<T> function) {

    long[] a = {init};
    consume(t -> a[0] = function.apply(a[0], t));
    return a[0];
  }

  default <E> E foldAtomic(E init, BiFunction<E, T, E> function) {

    AtomicReference<E> m = new AtomicReference<>(init);
    consume(t -> m.updateAndGet(e -> function.apply(e, t)));
    return m.get();
  }

  default <K> MapZeroFlow<K, ArrayListZeroFlow<T>> groupBy(Function<T, K> toKey) {

    return groupBy(toKey, Reducer.toList());
  }

  default <K, V> MapZeroFlow<K, V> groupBy(Function<T, K> toKey, Reducer<T, V> reducer) {

    return reduce(Reducer.groupBy(toKey, reducer));
  }

  default <K> MapZeroFlow<K, T> groupBy(Function<T, K> toKey, BinaryOperator<T> operator) {

    return groupBy(toKey, Transducer.of(operator));
  }

  default <K, V, E> MapZeroFlow<K, E> groupBy(Function<T, K> toKey, Transducer<T, V, E> transducer) {

    return reduce(Reducer.groupBy(toKey, transducer));
  }

  default <K, E> MapZeroFlow<K, ArrayListZeroFlow<E>> groupBy(Function<T, K> toKey, Function<T, E> toValue) {

    return groupBy(toKey, Reducer.mapping(toValue));
  }

  default T last() {

    return reduce(new Mutable<T>(null), Mutable::set).it;
  }

  default Optional<T> lastNot(Predicate<T> predicate) {

    return last(predicate.negate());
  }

  default Optional<T> last(Predicate<T> predicate) {

    return filter(predicate).lastMaybe();
  }

  default Optional<T> lastMaybe() {

    Mutable<T> m = new Mutable<>(null);
    consume(m::set);
    return m.toOptional();
  }

  default Lazy<T> lazyLast() {

    return new Mutable<T>(null) {

      @Override
      protected void eval() {

        consume(t -> it = t);
      }
    };
  }

  default Lazy<T> lazyReduce(BinaryOperator<T> binaryOperator) {

    return Lazy.of(() -> reduce(binaryOperator));
  }

  /**
   * map/reduce理论中的核心reduce方法
   */
  default T reduce(BinaryOperator<T> binaryOperator) {

    return reduce(Transducer.of(binaryOperator));
  }

  default <E> Lazy<E> lazyReduce(Reducer<T, E> reducer) {

    return Lazy.of(() -> reduce(reducer));
  }

  default <E, V> Lazy<E> lazyReduce(Transducer<T, V, E> transducer) {

    return Lazy.of(() -> reduce(transducer));
  }

  default <E> ZeroFlow<E> map(Function<T, E> function, int n, Function<T, E> substitute) {

    return n <= 0 ? map(function) : c -> {
      int[] a = {n - 1};
      consume(t -> {
        if (a[0] < 0) {
          c.accept(function.apply(t));
        } else {
          a[0]--;
          c.accept(substitute.apply(t));
        }
      });
    };
  }

  default <E> ZeroFlow<E> mapIndexed(IndexObjFunction<T, E> function) {

    return c -> consumeIndexed((i, t) -> c.accept(function.apply(i, t)));
  }

  default <E> ZeroFlow<E> mapMaybe(Function<T, E> function) {

    return c -> consume(t -> {
      if (t != null) {
        c.accept(function.apply(t));
      }
    });
  }

  default <E> ZeroFlow<E> mapNotNull(Function<T, E> function) {

    return c -> consume(t -> {
      E e = function.apply(t);
      if (e != null) {
        c.accept(e);
      }
    });
  }

  default PairZeroFlow<T, T> mapPair(boolean overlapping) {

    return c -> reduce(new BooleanPair<>(false, (T) null), (p, t) -> {
      if (p.first) {
        c.accept(p.second, t);
      }
      p.first  = overlapping || !p.first;
      p.second = t;
    });
  }

  default ZeroFlow<ArrayListZeroFlow<T>> mapSub(Predicate<T> takeWhile) {

    return mapSub(takeWhile, Reducer.toList());
  }

  default <V> ZeroFlow<V> mapSub(Predicate<T> takeWhile, Reducer<T, V> reducer) {

    Supplier<V>      supplier    = reducer.supplier();
    BiConsumer<V, T> accumulator = reducer.accumulator();
    return c -> {
      V last = fold(null, (v, t) -> {
        if (takeWhile.test(t)) {
          if (v == null) {
            v = supplier.get();
          }
          accumulator.accept(v, t);
          return v;
        } else {
          if (v != null) {
            c.accept(v);
          }
          return null;
        }
      });
      if (last != null) {
        c.accept(last);
      }
    };
  }

  /**
   * @return {@link E }
   *
   * @see ItrZeroFlow#fold(Object, BiFunction)
   */
  default <E> E fold(E init, BiFunction<E, T, E> function) {

    Mutable<E> m = new Mutable<>(init);
    consume(t -> m.it = function.apply(m.it, t));
    return m.it;
  }

  default ZeroFlow<ArrayListZeroFlow<T>> mapSub(T first, T last) {

    return mapSub(first, last, Reducer.toList());
  }

  default <V> ZeroFlow<V> mapSub(T first, T last, Reducer<T, V> reducer) {

    return mapSub(first::equals, last::equals, reducer);
  }

  default <V> ZeroFlow<V> mapSub(Predicate<T> first, Predicate<T> last, Reducer<T, V> reducer) {

    Supplier<V>      supplier    = reducer.supplier();
    BiConsumer<V, T> accumulator = reducer.accumulator();
    return c -> fold((V) null, (v, t) -> {
      if (v == null && first.test(t)) {
        v = supplier.get();
        accumulator.accept(v, t);
      } else if (v != null) {
        accumulator.accept(v, t);
        if (last.test(t)) {
          c.accept(v);
          return null;
        }
      }
      return v;
    });
  }

  default IntZeroFlow mapToInt(ToIntFunction<T> function) {

    return c -> consume(t -> c.accept(function.applyAsInt(t)));
  }

  default T max(Comparator<T> comparator) {

    return reduce(Reducer.max(comparator));
  }

  default <V extends Comparable<V>> Pair<T, V> maxBy(Function<T, V> function) {

    return reduce(Reducer.maxBy(function));
  }

  default T min(Comparator<T> comparator) {

    return reduce(Reducer.min(comparator));
  }

  default <V extends Comparable<V>> Pair<T, V> minBy(Function<T, V> function) {

    return reduce(Reducer.minBy(function));
  }

  default boolean none(Predicate<T> predicate) {

    return !find(predicate).isPresent();
  }

  default ZeroFlow<T> onEach(Consumer<T> consumer) {

    return c -> consume(consumer.andThen(c));
  }

  default ZeroFlow<T> onEach(int n, Consumer<T> consumer) {

    return c -> consume(c, n, consumer.andThen(c));
  }

  default ZeroFlow<T> onEachIndexed(IndexObjConsumer<T> consumer) {

    return c -> consumeIndexed((i, t) -> {
      consumer.accept(i, t);
      c.accept(t);
    });
  }

  default <A, B> PairZeroFlow<A, B> pair(Function<T, A> f1, Function<T, B> f2) {

    return c -> consume(t -> c.accept(f1.apply(t), f2.apply(t)));
  }

  default <E> PairZeroFlow<E, T> pairBy(Function<T, E> function) {

    return c -> consume(t -> c.accept(function.apply(t), t));
  }

  default <E> PairZeroFlow<E, T> pairByNotNull(Function<T, E> function) {

    return c -> consume(t -> {
      E e = function.apply(t);
      if (e != null) {
        c.accept(e, t);
      }
    });
  }

  default <E> PairZeroFlow<T, E> pairWith(Function<T, E> function) {

    return c -> consume(t -> c.accept(t, function.apply(t)));
  }

  default <E> PairZeroFlow<T, E> pairWithNotNull(Function<T, E> function) {

    return c -> consume(t -> {
      E e = function.apply(t);
      if (e != null) {
        c.accept(t, e);
      }
    });
  }

  default ZeroFlow<T> parallel() {

    return parallel(Async.common());
  }

  default ZeroFlow<T> parallel(Async async) {

    return c -> async.joinAll(map(t -> () -> c.accept(t)));
  }

  default ZeroFlow<T> parallelNoJoin() {

    return parallelNoJoin(Async.common());
  }

  default ZeroFlow<T> parallelNoJoin(Async async) {

    return c -> consume(t -> async.submit(() -> c.accept(t)));
  }

  default void printAll(String sep) {

    if ("\n".equals(sep)) {
      println();
    } else {
      System.out.println(join(sep));
    }
  }

  default String join(String sep) {

    return join(sep, Object::toString);
  }

  default String join(String sep, Function<T, String> function) {

    return reduce(new StringJoiner(sep), (j, t) -> j.add(function.apply(t))).toString();
  }

  default void println() {

    consume(System.out::println);
  }

  default ZeroFlow<T> replace(int n, UnaryOperator<T> operator) {

    return c -> consume(c, n, t -> c.accept(operator.apply(t)));
  }

  default ArrayListZeroFlow<T> reverse() {

    return reduce(Reducer.reverse());
  }

  /**
   * @return {@link IntZeroFlow }
   *
   * @see ItrZeroFlow#fold(Object, BiFunction)
   */
  default <E> ZeroFlow<E> runningFold(E init, BiFunction<E, T, E> function) {

    return c -> fold(init, (e, t) -> {
      e = function.apply(e, t);
      c.accept(e);
      return e;
    });
  }

  default <E extends Comparable<E>> ZeroFlow<T> sortCached(Function<T, E> function) {

    return map(t -> new Pair<>(t, function.apply(t)))
        .sortBy(p -> p.second)
        .map(p -> p.first);
  }

  default <E extends Comparable<E>> ArrayListZeroFlow<T> sortBy(Function<T, E> function) {

    return sortWith(Comparator.comparing(function));
  }

  default ArrayListZeroFlow<T> sortWith(Comparator<T> comparator) {

    ArrayListZeroFlow<T> list = toList();
    list.sort(comparator);
    return list;
  }

  default ArrayListZeroFlow<T> toList() {

    return reduce(new ArrayListZeroFlow<>(sizeOrDefault()), ArrayListZeroFlow::add);
  }

  default <E extends Comparable<E>> ZeroFlow<T> sortCachedDesc(Function<T, E> function) {

    return map(t -> new Pair<>(t, function.apply(t)))
        .sortByDesc(p -> p.second)
        .map(p -> p.first);
  }

  default <E extends Comparable<E>> ArrayListZeroFlow<T> sortByDesc(Function<T, E> function) {

    return sortWith(Comparator.comparing(function).reversed());
  }

  default ArrayListZeroFlow<T> sorted() {

    return sortWith(null);
  }

  default ArrayListZeroFlow<T> sortedDesc() {

    return sortWith(Collections.reverseOrder());
  }

  default ArrayListZeroFlow<T> sortWithDesc(Comparator<T> comparator) {

    return sortWith(comparator.reversed());
  }

  default double sum(ToDoubleFunction<T> function) {

    return reduce(Reducer.sum(function));
  }

  default int sumInt(ToIntFunction<T> function) {

    return reduce(Reducer.sumInt(function));
  }

  default long sumLong(ToLongFunction<T> function) {

    return reduce(Reducer.sumLong(function));
  }

  default ZeroFlow<T> take(int n) {

    return n <= 0 ? empty() : c -> {
      int[] i = {n};
      consumeTillStop(t -> {
        c.accept(t);
        if (--i[0] == 0) {
          stop();
        }
      });
    };
  }

  default ZeroFlow<T> takeWhile(BiPredicate<T, T> testPrevCurr) {

    return takeWhile(t -> t, testPrevCurr);
  }

  default <E> ZeroFlow<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {

    return c -> {
      Mutable<E> m = new Mutable<>(null);
      consumeTillStop(t -> {
        E curr = function.apply(t);
        if (m.it == null || testPrevCurr.test(m.it, curr)) {
          c.accept(t);
          m.it = curr;
        } else {
          stop();
        }
      });
    };
  }

  default ZeroFlow<T> takeWhile(Predicate<T> predicate) {

    return c -> consumeTillStop(t -> {
      if (predicate.test(t)) {
        c.accept(t);
      } else {
        stop();
      }
    });
  }

  default ZeroFlow<T> takeWhileEquals() {

    return takeWhile(t -> t, Objects::equals);
  }

  default <E> ZeroFlow<T> takeWhileEquals(Function<T, E> function) {

    return takeWhile(function, Objects::equals);
  }

  default ZeroFlow<T> timeLimit(long millis) {

    return millis * 1000000 <= 0 ? this : c -> {
      long end = System.nanoTime() + millis;
      consumeTillStop(t -> {
        if (System.nanoTime() > end) {
          stop();
        }
        c.accept(t);
      });
    };
  }

  default T[] toObjArray(IntFunction<T[]> initializer) {

    SizedZeroFlow<T> ts = cache();
    T[]              a  = initializer.apply(ts.size());
    ts.consumeIndexed((i, t) -> a[i] = t);
    return a;
  }

  default SizedZeroFlow<T> cache() {

    return toBatched();
  }

  default int[] toIntArray(ToIntFunction<T> function) {

    SizedZeroFlow<T> ts = cache();
    int[]            a  = new int[ts.size()];
    ts.consumeIndexed((i, t) -> a[i] = function.applyAsInt(t));
    return a;
  }

  default double[] toDoubleArray(ToDoubleFunction<T> function) {

    SizedZeroFlow<T> ts = cache();
    double[]         a  = new double[ts.size()];
    ts.consumeIndexed((i, t) -> a[i] = function.applyAsDouble(t));
    return a;
  }

  default long[] toLongArray(ToLongFunction<T> function) {

    SizedZeroFlow<T> ts = cache();
    long[]           a  = new long[ts.size()];
    ts.consumeIndexed((i, t) -> a[i] = function.applyAsLong(t));
    return a;
  }

  default boolean[] toBooleanArray(Predicate<T> function) {

    SizedZeroFlow<T> ts = cache();
    boolean[]        a  = new boolean[ts.size()];
    ts.consumeIndexed((i, t) -> a[i] = function.test(t));
    return a;
  }

  default ConcurrentQueueZeroFlow<T> toConcurrentQueue() {

    return reduce(new ConcurrentQueueZeroFlow<>(), ConcurrentQueueZeroFlow::add);
  }

  default <E> Lazy<E> toLazy(Reducer<T, E> reducer) {

    return Lazy.of(() -> reduce(reducer));
  }

  default <V, E> Lazy<E> toLazy(Transducer<T, V, E> transducer) {

    return Lazy.of(() -> reduce(transducer));
  }

  default LinkedListZeroFlow<T> toLinkedList() {

    return reduce(new LinkedListZeroFlow<>(), LinkedListZeroFlow::add);
  }

  default <K> MapZeroFlow<K, T> toMapBy(Function<T, K> toKey) {

    return toMap(toKey, v -> v);
  }

  default <K, V> MapZeroFlow<K, V> toMap(Function<T, K> toKey, Function<T, V> toValue) {

    return reduce(Reducer.toMap(() -> new LinkedHashMap<>(sizeOrDefault()), toKey, toValue));
  }

  default <V> MapZeroFlow<T, V> toMapWith(Function<T, V> toValue) {

    return toMap(k -> k, toValue);
  }

  default SetZeroFlow<T> toSet() {

    return reduce(Reducer.toSet(sizeOrDefault()));
  }

  default <A, B, D> TripleZeroFlow<A, B, D> triple(BiConsumer<TripleConsumer<A, B, D>, T> consumer) {

    return c -> consume(t -> consumer.accept(c, t));
  }

  default <A, B, D> TripleZeroFlow<A, B, D> triple(Function<T, A> f1, Function<T, B> f2, Function<T, D> f3) {

    return c -> consume(t -> c.accept(f1.apply(t), f2.apply(t), f3.apply(t)));
  }

  default ZeroFlow<ArrayListZeroFlow<T>> windowed(int size, int step, boolean allowPartial) {

    return windowed(size, step, allowPartial, Reducer.toList());
  }

  /**
   * 通过滑动窗口生产数据，
   *
   * @param size
   *     每个窗口的大小，既是原数据流中多少个数据生产一个窗口包括的数据
   * @param step
   *     上一个窗口的第一个数据滑动多少个数据才形成下一个窗口的第一个数据
   * @param allowPartial
   *     剩余不足窗口数据量的窗口是否生产出来
   * @param reducer
   *     窗口的类型
   * @param <V>
   *     窗口的类型，既是容纳窗口中数据的容器类型
   *
   * @return {@link ZeroFlow }<{@link V }>
   */
  default <V> ZeroFlow<V> windowed(int size, int step, boolean allowPartial, Reducer<T, V> reducer) {

    if (size <= 0 || step <= 0) {
      throw new IllegalArgumentException("non-positive size or step");
    }
    return c -> {
      Supplier<V>       supplier    = reducer.supplier();
      BiConsumer<V, T>  accumulator = reducer.accumulator();
      Consumer<V>       finisher    = reducer.finisher();
      Queue<IntPair<V>> queue       = new LinkedList<>();
      foldInt(0, (left, t) -> {
        // t是已有序列的数据
        if (left == 0) {
          // 当等于0的时候就创建一个窗口，然后将间隔step赋值，
          // 控制每次折叠数据的时候自减到下一个窗口的起始数据时再创建下一个窗口
          left = step;
          queue.offer(new IntPair<>(0, supplier.get()));
        }
        queue.forEach(sub -> {
          //将当前数据添加到所有此刻打开的窗口中
          accumulator.accept(sub.second, t);
          sub.first++;
        });
        IntPair<V> first = queue.peek();
        if (first != null && first.first == size) {
          // 当第一个窗口满的时候移除
          queue.poll();
          if (finisher != null) {
            finisher.accept(first.second);
          }
          // 并将窗口生产
          c.accept(first.second);
        }
        return left - 1;
      });
      if (allowPartial) {
        //剩余不足数量的窗口生产
        queue.forEach(p -> c.accept(p.second));
      }
      queue.clear();
    };
  }

  /**
   * 将流中数据折叠成一个结果
   *
   * @return int
   */
  default int foldInt(int init, IntObjToInt<T> function) {

    int[] a = {init};
    consume(t -> a[0] = function.apply(a[0], t));
    return a[0];
  }

  default <V, E> ZeroFlow<E> windowed(int size, int step, boolean allowPartial, Transducer<T, V, E> transducer) {

    return windowed(size, step, allowPartial, transducer.reducer()).map(transducer.transformer());
  }

  default ZeroFlow<ArrayListZeroFlow<T>> windowedByTime(long timeMillis) {

    return windowedByTime(timeMillis, Reducer.toList());
  }

  /**
   * 按时间的滑动窗口实现
   *
   * @return {@link ZeroFlow }<{@link V }>
   */
  default <V> ZeroFlow<V> windowedByTime(long timeMillis, Reducer<T, V> reducer) {

    if (timeMillis <= 0) {
      throw new IllegalArgumentException("non-positive time");
    }
    return c -> {
      Supplier<V>      supplier    = reducer.supplier();
      BiConsumer<V, T> accumulator = reducer.accumulator();
      Consumer<V>      finisher    = reducer.finisher();
      reduce(new LongPair<>(System.currentTimeMillis(), supplier.get()), (p, t) -> {
        long now = System.currentTimeMillis();
        if (now - p.first > timeMillis) {
          // 超过给定的时间时隔就重置窗口开始时间
          p.first = now;
          if (finisher != null) {
            finisher.accept(p.second);
          }
          // 将上一个窗口生产
          c.accept(p.second);
          //重新创建一个窗口
          p.second = supplier.get();
        }
        accumulator.accept(p.second, t);
      });
    };
  }

  default ZeroFlow<ArrayListZeroFlow<T>> windowedByTime(long timeMillis, long stepMillis) {

    return windowedByTime(timeMillis, stepMillis, Reducer.toList());
  }

  /**
   * 类似{@link  #windowed(int, int, boolean, Reducer)}，只是把控制数量变成控制时间
   *
   * @param timeMillis
   *     一个窗口产生多少时间数据
   * @param stepMillis
   *     每个窗口间隔多少时间才创建
   * @param reducer
   *     生产窗口的类型
   *
   * @return {@link ZeroFlow }<{@link V }>
   */
  default <V> ZeroFlow<V> windowedByTime(long timeMillis, long stepMillis, Reducer<T, V> reducer) {

    if (timeMillis <= 0 || stepMillis <= 0) {
      throw new IllegalArgumentException("non-positive time or step");
    }
    return c -> {
      Supplier<V>        supplier    = reducer.supplier();
      BiConsumer<V, T>   accumulator = reducer.accumulator();
      Consumer<V>        finisher    = reducer.finisher();
      Queue<LongPair<V>> queue       = new LinkedList<>();
      long[]             last        = {System.currentTimeMillis(), 0};
      reduce(last, (a, t) -> {
        if (a[1] <= 0) {
          a[1] = stepMillis;
          queue.offer(new LongPair<>(System.currentTimeMillis(), supplier.get()));
        }
        queue.forEach(sub -> accumulator.accept(sub.second, t));
        LongPair<V> first = queue.peek();
        if (first != null && System.currentTimeMillis() - first.first > timeMillis) {
          queue.poll();
          if (finisher != null) {
            finisher.accept(first.second);
          }
          c.accept(first.second);
        }
        long now = System.currentTimeMillis();
        a[1] -= now - a[0];
        a[0] = now;
      });
      queue.clear();
    };
  }

  default <V, E> ZeroFlow<E> windowedByTime(long timeMillis, long stepMillis, Transducer<T, V, E> transducer) {

    return windowedByTime(timeMillis, stepMillis, transducer.reducer()).map(transducer.transformer());
  }

  default <V, E> ZeroFlow<E> windowedByTime(long timeMillis, Transducer<T, V, E> transducer) {

    return windowedByTime(timeMillis, transducer.reducer()).map(transducer.transformer());
  }

  default ZeroFlow<IntPair<T>> withInt(ToIntFunction<T> function) {

    return map(t -> new IntPair<>(function.applyAsInt(t), t));
  }

  default ZeroFlow<DoublePair<T>> withDouble(ToDoubleFunction<T> function) {

    return map(t -> new DoublePair<>(function.applyAsDouble(t), t));
  }

  default ZeroFlow<LongPair<T>> withLong(ToLongFunction<T> function) {

    return map(t -> new LongPair<>(function.applyAsLong(t), t));
  }

  default ZeroFlow<BooleanPair<T>> withBool(Predicate<T> function) {

    return map(t -> new BooleanPair<>(function.test(t), t));
  }

  default ZeroFlow<IntPair<T>> withIndex() {

    return c -> consumeIndexed((i, t) -> c.accept(new IntPair<>(i, t)));
  }

  default <B, C> TripleZeroFlow<T, B, C> zip(Iterable<B> bs, Iterable<C> cs) {

    return c -> zip(bs, cs, c);
  }

  default <B, C> void zip(Iterable<B> bs, Iterable<C> cs, TripleConsumer<T, B, C> consumer) {

    Iterator<B> bi = bs.iterator();
    Iterator<C> ci = cs.iterator();
    consumeTillStop(t -> consumer.accept(t, ItrUtil.pop(bi), ItrUtil.pop(ci)));
  }

  default <E> PairZeroFlow<T, E> zip(Iterable<E> iterable) {

    return c -> zip(iterable, c);
  }

  default <E> void zip(Iterable<E> iterable, BiConsumer<T, E> consumer) {

    Iterator<E> iterator = iterable.iterator();
    consumeTillStop(t -> consumer.accept(t, ItrUtil.pop(iterator)));
  }

  interface IntObjToInt<T> {

    int apply(int acc, T t);

  }

  interface DoubleObjToDouble<T> {

    double apply(double acc, T t);

  }

  interface LongObjToLong<T> {

    long apply(long acc, T t);

  }

  interface BooleanObjToBoolean<T> {

    boolean apply(boolean acc, T t);

  }

  interface IndexObjConsumer<T> {

    void accept(int i, T t);

  }

  interface IndexObjFunction<T, E> {

    E apply(int i, T t);

  }

  interface IndexObjPredicate<T> {

    boolean test(int i, T t);

  }

  class Empty {

    static final ZeroFlow<Object> emptySeq = c -> {
    };

    static final Consumer<Object> nothing = t -> {
    };

  }

}
