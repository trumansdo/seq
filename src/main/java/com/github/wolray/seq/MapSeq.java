package com.github.wolray.seq;

import com.github.wolray.seq.pair.PairSeq;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author wolray
 */
public interface MapSeq<K, V> extends PairSeq<K, V>, Map<K, V> {

    SetSeq<K> keySetSeq();

    CollectionSeq<V> valuesSeq();

    SetSeq<Entry<K, V>> entrySetSeq();

    <A, B> MapSeq<A, B> newForMapping();


    @Override
    default void consume(BiConsumer<K, V> consumer) {
        forEach(consumer);
    }

    default <E> MapSeq<E, V> mapByKey(BiFunction<K, V, E> function) {
        return toMap(newForMapping(), function, (k, v) -> v);
    }

    default <E> MapSeq<E, V> mapByKey(Function<K, E> function) {
        return toMap(newForMapping(), (k, v) -> function.apply(k), (k, v) -> v);
    }

    default <E> MapSeq<K, E> mapByValue(BiFunction<K, V, E> function) {
        return toMap(newForMapping(), (k, v) -> k, function);
    }

    default <E> MapSeq<K, E> mapByValue(Function<V, E> function) {
        return toMap(newForMapping(), (k, v) -> k, (k, v) -> function.apply(v));
    }

    @Override
    default MapSeq<K, V> toMap() {
        return this;
    }

    static <K, V> MapSeq<K, V> hash() {
        return new LinkedMapSeq<>();
    }

    static <K, V> MapSeq<K, V> hash(int initialCapacity) {
        return new LinkedMapSeq<>(initialCapacity);
    }

    static <K, V> MapSeq<K, V> of(Map<K, V> map) {
        return map instanceof MapSeq ? (MapSeq<K, V>)map : new Proxy<>(map);
    }

    static <K, V> MapSeq<K, V> tree(Comparator<K> comparator) {
        return new Proxy<>(new TreeMap<>(comparator));
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    @SuppressWarnings("unchecked")
    default <E> MapSeq<K, E> replaceValue(BiFunction<K, V, E> function) {
        MapSeq<K, Object> map = (MapSeq<K, Object>)this;
        map.entrySet().forEach(e -> e.setValue(function.apply(e.getKey(), (V)e.getValue())));
        return (MapSeq<K, E>)map;
    }

    @SuppressWarnings("unchecked")
    default <E> MapSeq<K, E> replaceValue(Function<V, E> function) {
        MapSeq<K, Object> map = (MapSeq<K, Object>)this;
        map.entrySet().forEach(e -> e.setValue(function.apply((V)e.getValue())));
        return (MapSeq<K, E>)map;
    }

    default <E extends Comparable<E>> ArraySeq<Entry<K, V>> sort(BiFunction<K, V, E> function) {
        return entrySetSeq().sortBy(e -> function.apply(e.getKey(), e.getValue()));
    }

    default ArraySeq<Entry<K, V>> sortByKey(Comparator<K> comparator) {
        return entrySetSeq().sortWith(Entry.comparingByKey(comparator));
    }

    default ArraySeq<Entry<K, V>> sortByValue(Comparator<V> comparator) {
        return entrySetSeq().sortWith(Entry.comparingByValue(comparator));
    }

    default <E extends Comparable<E>> ArraySeq<Entry<K, V>> sortDesc(BiFunction<K, V, E> function) {
        return entrySetSeq().sortByDesc(e -> function.apply(e.getKey(), e.getValue()));
    }

    default ArraySeq<Entry<K, V>> sortDescByKey(Comparator<K> comparator) {
        return entrySetSeq().sortWithDesc(Entry.comparingByKey(comparator));
    }

    default ArraySeq<Entry<K, V>> sortDescByValue(Comparator<V> comparator) {
        return entrySetSeq().sortWithDesc(Entry.comparingByValue(comparator));
    }

    class Proxy<K, V> implements MapSeq<K, V> {
        public final Map<K, V> backer;

        Proxy(Map<K, V> backer) {
            this.backer = backer;
        }

        @Override
        public void consume(BiConsumer<K, V> consumer) {
            backer.forEach(consumer);
        }

        @Override
        public Set<K> keySet() {
            return backer.keySet();
        }

        @Override
        public SetSeq<K> keySetSeq() {
            return SetSeq.of(backer.keySet());
        }

        @Override
        public Collection<V> values() {
            return backer.values();
        }

        @Override
        public CollectionSeq<V> valuesSeq() {
            return CollectionSeq.of(backer.values());
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return backer.entrySet();
        }

        @Override
        public SetSeq<Entry<K, V>> entrySetSeq() {
            return SetSeq.of(backer.entrySet());
        }

        @Override
        public <A, B> MapSeq<A, B> newForMapping() {
            if (backer instanceof TreeMap) {
                return new Proxy<>(new TreeMap<>());
            }
            if (backer instanceof ConcurrentHashMap) {
                return new Proxy<>(new ConcurrentHashMap<>(backer.size()));
            }
            return new LinkedMapSeq<>(backer.size());
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
        public String toString() {
            return backer.toString();
        }
    }
}
