package com.github.wolray.seq.pair;

import static com.github.wolray.seq.Splitter.substring;

import com.github.wolray.seq.BaseZeroFlow;
import com.github.wolray.seq.ItrZeroFlow;
import com.github.wolray.seq.MapZeroFlow;
import com.github.wolray.seq.Mutable;
import com.github.wolray.seq.ZeroFlow;
import com.github.wolray.seq.triple.TripleConsumer;
import com.github.wolray.seq.triple.TripleFunction;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 二元流，对应天生的消费接口是{@link Map#forEach(BiConsumer)}
 *
 * @author wolray
 */
public interface PairZeroFlow<K, V> extends BaseZeroFlow<BiConsumer<K, V>> {

  @SuppressWarnings("unchecked")
  static <K, V> PairZeroFlow<K, V> empty() {

    return (PairZeroFlow<K, V>) Empty.emptySeq;
  }

  @SuppressWarnings("unchecked")
  static <K, V> BiConsumer<K, V> nothing() {

    return (BiConsumer<K, V>) Empty.nothing;
  }

  static <K, V> PairZeroFlow<K, V> of(Map<K, V> map) {

    return map instanceof MapZeroFlow ? (MapZeroFlow<K, V>) map : map::forEach;
  }

  /**
   * @see #parseMap(char[], char, char)
   */
  static PairZeroFlow<String, String> parseMap(String s, char entrySep, char kvSep) {

    return parseMap(s.toCharArray(), entrySep, kvSep);
  }

  /**
   * 将字符串解析为键值对的二元流。类似：key:val|key:val|key:val|key:val的字符串
   *
   * @param chars
   *     字符串
   * @param entrySep
   *     每个键值对之间的分隔符
   * @param kvSep
   *     键值的分隔符
   *
   * @return {@link PairZeroFlow }<{@link String }, {@link String }>
   */
  static PairZeroFlow<String, String> parseMap(char[] chars, char entrySep, char kvSep) {

    return c -> {
      int    len  = chars.length, last = 0;
      String prev = null;
      for (int i = 0; i < len; i++) {
        if (chars[i] == entrySep) {
          if (prev != null) {
            c.accept(prev, substring(chars, last, i));
            prev = null;
          }
          last = i + 1;
        } else if (prev == null && chars[i] == kvSep) {
          prev = substring(chars, last, i);
          last = i + 1;
        }
      }
      if (prev != null) {
        c.accept(prev, substring(chars, last, len));
      }
    };
  }

  static <K, V> PairZeroFlow<K, V> unit(K k, V v) {

    return c -> c.accept(k, v);
  }

  default PairZeroFlow<K, V> cache() {

    ZeroFlow<Pair<K, V>> pairSeq = paired().cache();
    return c -> pairSeq.consume(p -> c.accept(p.first, p.second));
  }

  /**
   * 将key和value用{@link Pair}存放
   *
   * @return {@link ZeroFlow }<{@link Pair }<{@link K }, {@link V }>>
   */
  default ZeroFlow<Pair<K, V>> paired() {

    return map(Pair::new);
  }

  default <T> ZeroFlow<T> map(BiFunction<K, V, T> function) {

    return c -> consume((k, v) -> c.accept(function.apply(k, v)));
  }

  default PairZeroFlow<K, V> filter(BiPredicate<K, V> predicate) {

    return c -> consume((k, v) -> {
      if (predicate.test(k, v)) {
        c.accept(k, v);
      }
    });
  }

  default PairZeroFlow<K, V> filterByKey(Predicate<K> predicate) {

    return c -> consume((k, v) -> {
      if (predicate.test(k)) {
        c.accept(k, v);
      }
    });
  }

  default PairZeroFlow<K, V> filterByValue(Predicate<V> predicate) {

    return c -> consume((k, v) -> {
      if (predicate.test(v)) {
        c.accept(k, v);
      }
    });
  }

  /**
   * 折叠，如{@link ItrZeroFlow#fold(Object, BiFunction)}
   *
   * @return {@link E }
   */
  default <E> E fold(E init, TripleFunction<E, K, V, E> function) {

    Mutable<E> m = new Mutable<>(init);
    consume((k, v) -> m.setIt(function.apply(m.getIt(), k, v)));
    return m.getIt();
  }

  /**
   * 返回key的流
   *
   * @return {@link ZeroFlow }<{@link K }>
   */
  default ZeroFlow<K> justKeys() {

    return c -> consume((k, v) -> c.accept(k));
  }

  /**
   * 返回value的流
   *
   * @return {@link ZeroFlow }<{@link V }>
   */
  default ZeroFlow<V> justValues() {

    return c -> consume((k, v) -> c.accept(v));
  }

  default <T> PairZeroFlow<T, V> mapKey(BiFunction<K, V, T> function) {

    return c -> consume((k, v) -> c.accept(function.apply(k, v), v));
  }

  default <T> PairZeroFlow<T, V> mapKey(Function<K, T> function) {

    return c -> consume((k, v) -> c.accept(function.apply(k), v));
  }

  default <T> PairZeroFlow<K, T> mapValue(BiFunction<K, V, T> function) {

    return c -> consume((k, v) -> c.accept(k, function.apply(k, v)));
  }

  default <T> PairZeroFlow<K, T> mapValue(Function<V, T> function) {

    return c -> consume((k, v) -> c.accept(k, function.apply(v)));
  }

  default Pair<K, V> maxByKey(Comparator<K> comparator) {

    return reduce(new Pair<>(null, null), (p, k, v) -> {
      if (p.first == null || comparator.compare(p.first, k) < 0) {
        p.set(k, v);
      }
    });
  }

  default <E> E reduce(E des, TripleConsumer<E, K, V> accumulator) {

    consume((k, v) -> accumulator.accept(des, k, v));
    return des;
  }

  default Pair<K, V> maxByValue(Comparator<V> comparator) {

    return reduce(new Pair<>(null, null), (p, k, v) -> {
      if (p.second == null || comparator.compare(p.second, v) < 0) {
        p.set(k, v);
      }
    });
  }

  default Pair<K, V> minByKey(Comparator<K> comparator) {

    return reduce(new Pair<>(null, null), (p, k, v) -> {
      if (p.first == null || comparator.compare(p.first, k) > 0) {
        p.set(k, v);
      }
    });
  }

  default Pair<K, V> minByValue(Comparator<V> comparator) {

    return reduce(new Pair<>(null, null), (p, k, v) -> {
      if (p.second == null || comparator.compare(p.second, v) > 0) {
        p.set(k, v);
      }
    });
  }

  default PairZeroFlow<K, V> onEach(BiConsumer<K, V> consumer) {

    return c -> consumeTillStop(consumer.andThen(c));
  }

  /**
   * 交换key和value
   *
   * @return {@link PairZeroFlow }<{@link V }, {@link K }>
   */
  default PairZeroFlow<V, K> swap() {

    return c -> consume((k, v) -> c.accept(v, k));
  }

  default MapZeroFlow<K, V> toMap() {

    return toMap(new LinkedHashMap<>());
  }

  default MapZeroFlow<K, V> toMap(Map<K, V> des) {

    return MapZeroFlow.of(reduce(des, Map::put));
  }

  default <A, B> MapZeroFlow<A, B> toMap(BiFunction<K, V, A> toKey, BiFunction<K, V, B> toValue) {

    return toMap(new LinkedHashMap<>(), toKey, toValue);
  }

  default <A, B> MapZeroFlow<A, B> toMap(Map<A, B> des, BiFunction<K, V, A> toKey, BiFunction<K, V, B> toValue) {

    return MapZeroFlow.of(reduce(des, (res, k, v) -> res.put(toKey.apply(k, v), toValue.apply(k, v))));
  }

  class Empty {

    static final PairZeroFlow<Object, Object> emptySeq = c -> {
    };

    static final BiConsumer<Object, Object> nothing = (k, v) -> {
    };

  }

}
