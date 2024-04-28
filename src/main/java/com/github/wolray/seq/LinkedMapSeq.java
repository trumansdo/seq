package com.github.wolray.seq;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author wolray
 */
public class LinkedMapSeq<K, V> extends LinkedHashMap<K, V> implements MapSeq<K, V> {
    public LinkedMapSeq(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkedMapSeq() {}

    public LinkedMapSeq(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    public SetSeq<K> keySetSeq() {
        return SetSeq.of(keySet());
    }

    @Override
    public CollectionSeq<V> valuesSeq() {
        return CollectionSeq.of(values());
    }

    @Override
    public SetSeq<Entry<K, V>> entrySetSeq() {
        return SetSeq.of(entrySet());
    }

    @Override
    public <A, B> MapSeq<A, B> newForMapping() {
        return new LinkedMapSeq<>(size());
    }
}
