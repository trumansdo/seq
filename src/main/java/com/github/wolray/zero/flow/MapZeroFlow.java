package com.github.wolray.zero.flow;

import com.github.wolray.zero.flow.pair.PairZeroFlow;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Map集合的流
 *
 * @author wolray
 */
public interface MapZeroFlow<K, V> extends PairZeroFlow<K, V>, Map<K, V> {

  static <K, V> MapZeroFlow<K, V> hash() {

    return new LinkedHashMapZeroFlow<>();
  }

  static <K, V> MapZeroFlow<K, V> hash(int initialCapacity) {

    return new LinkedHashMapZeroFlow<>(initialCapacity);
  }

  static <K, V> MapZeroFlow<K, V> of(Map<K, V> map) {

    return map instanceof MapZeroFlow ? (MapZeroFlow<K, V>) map : new Proxy<>(map);
  }

  /**
   * 内部容器转成{@link TreeMap}
   *
   * @return {@link MapZeroFlow }<{@link K }, {@link V }>
   */
  static <K, V> MapZeroFlow<K, V> tree(Comparator<K> comparator) {

    return new Proxy<>(new TreeMap<>(comparator));
  }

  SetZeroFlow<K> keysSeq();

  CollectionZeroFlow<V> valuesSeq();

  @Override
  default void consume(BiConsumer<K, V> consumer) {

    forEach(consumer);
  }

  default <E> MapZeroFlow<E, V> mapByKey(BiFunction<K, V, E> function) {

    return toMap(newForMapping(), function, (k, v) -> v);
  }

  /**
   * 做map转换操作时应该重新创建容器
   *
   * @return {@link MapZeroFlow }<{@link A }, {@link B }>
   */
  <A, B> MapZeroFlow<A, B> newForMapping();

  default <E> MapZeroFlow<E, V> mapByKey(Function<K, E> function) {

    return toMap(newForMapping(), (k, v) -> function.apply(k), (k, v) -> v);
  }

  default <E> MapZeroFlow<K, E> mapByValue(BiFunction<K, V, E> function) {

    return toMap(newForMapping(), (k, v) -> k, function);
  }

  default <E> MapZeroFlow<K, E> mapByValue(Function<V, E> function) {

    return toMap(newForMapping(), (k, v) -> k, (k, v) -> function.apply(v));
  }

  @Override
  default MapZeroFlow<K, V> toMap() {

    return this;
  }

  default boolean isNotEmpty() {

    return !isEmpty();
  }

  @SuppressWarnings("unchecked")
  default <E> MapZeroFlow<K, E> replaceValue(BiFunction<K, V, E> function) {

    MapZeroFlow<K, Object> map = (MapZeroFlow<K, Object>) this;
    map.entrySet().forEach(e -> e.setValue(function.apply(e.getKey(), (V) e.getValue())));
    return (MapZeroFlow<K, E>) map;
  }

  @SuppressWarnings("unchecked")
  default <E> MapZeroFlow<K, E> replaceValue(Function<V, E> function) {

    MapZeroFlow<K, Object> map = (MapZeroFlow<K, Object>) this;
    map.entrySet().forEach(e -> e.setValue(function.apply((V) e.getValue())));
    return (MapZeroFlow<K, E>) map;
  }

  default <E extends Comparable<E>> ArrayListZeroFlow<Entry<K, V>> sort(BiFunction<K, V, E> function) {

    return entrySeq().sortBy(e -> function.apply(e.getKey(), e.getValue()));
  }

  SetZeroFlow<Entry<K, V>> entrySeq();

  default ArrayListZeroFlow<Entry<K, V>> sortByKey(Comparator<K> comparator) {

    return entrySeq().sortWith(Entry.comparingByKey(comparator));
  }

  default ArrayListZeroFlow<Entry<K, V>> sortByValue(Comparator<V> comparator) {

    return entrySeq().sortWith(Entry.comparingByValue(comparator));
  }

  default <E extends Comparable<E>> ArrayListZeroFlow<Entry<K, V>> sortDesc(BiFunction<K, V, E> function) {

    return entrySeq().sortByDesc(e -> function.apply(e.getKey(), e.getValue()));
  }

  default ArrayListZeroFlow<Entry<K, V>> sortDescByKey(Comparator<K> comparator) {

    return entrySeq().sortWithDesc(Entry.comparingByKey(comparator));
  }

  default ArrayListZeroFlow<Entry<K, V>> sortDescByValue(Comparator<V> comparator) {

    return entrySeq().sortWithDesc(Entry.comparingByValue(comparator));
  }

  class Proxy<K, V> implements MapZeroFlow<K, V> {

    public final Map<K, V> backer;


    Proxy(Map<K, V> backer) {

      this.backer = backer;
    }

    @Override
    public SetZeroFlow<K> keysSeq() {

      return SetZeroFlow.of(backer.keySet());
    }

    @Override
    public CollectionZeroFlow<V> valuesSeq() {

      return CollectionZeroFlow.of(backer.values());
    }

    @Override
    public SetZeroFlow<Entry<K, V>> entrySeq() {

      return SetZeroFlow.of(backer.entrySet());
    }

    @Override
    public <A, B> MapZeroFlow<A, B> newForMapping() {

      if (backer instanceof TreeMap) {
        return new Proxy<>(new TreeMap<>());
      }
      if (backer instanceof ConcurrentHashMap) {
        return new Proxy<>(new ConcurrentHashMap<>(backer.size()));
      }
      return new LinkedHashMapZeroFlow<>(backer.size());
    }

    @Override
    public void consume(BiConsumer<K, V> consumer) {

      backer.forEach(consumer);
    }

    @Override
    public int size() {

      return backer.size();
    }

    @Override
    public boolean isEmpty() {

      return backer.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {

      return backer.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {

      return backer.containsValue(value);
    }

    @Override
    public V get(Object key) {

      return backer.get(key);
    }

    @Override
    public V put(K key, V value) {

      return backer.put(key, value);
    }

    @Override
    public V remove(Object key) {

      return backer.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

      backer.putAll(m);
    }

    @Override
    public void clear() {

      backer.clear();
    }

    @Override
    public Set<K> keySet() {

      return backer.keySet();
    }

    @Override
    public Collection<V> values() {

      return backer.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {

      return backer.entrySet();
    }

    @Override
    public String toString() {

      return backer.toString();
    }

  }

}
