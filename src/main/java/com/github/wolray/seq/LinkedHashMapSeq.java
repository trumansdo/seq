package com.github.wolray.seq;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 既是流，也是{@link LinkedHashMap}
 *
 * @author wolray
 */
public class LinkedHashMapSeq<K, V> extends LinkedHashMap<K, V> implements MapSeq<K, V> {

  public LinkedHashMapSeq(int initialCapacity) {

    super(initialCapacity);
  }

  public LinkedHashMapSeq() {

  }

  public LinkedHashMapSeq(Map<? extends K, ? extends V> m) {

    super(m);
  }

  @Override
  public SetSeq<K> keysSeq() {

    return SetSeq.of(keySet());
  }

  @Override
  public CollectionSeq<V> valuesSeq() {

    return CollectionSeq.of(values());
  }

  @Override
  public <A, B> MapSeq<A, B> newForMapping() {

    return new LinkedHashMapSeq<>(size());
  }

  @Override
  public SetSeq<Entry<K, V>> entrySeq() {

    return SetSeq.of(entrySet());
  }

}
