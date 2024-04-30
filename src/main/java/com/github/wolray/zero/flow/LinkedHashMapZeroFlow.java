package com.github.wolray.zero.flow;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 既是流，也是{@link LinkedHashMap}
 *
 * @author wolray
 */
public class LinkedHashMapZeroFlow<K, V> extends LinkedHashMap<K, V> implements MapZeroFlow<K, V> {

  public LinkedHashMapZeroFlow(int initialCapacity) {

    super(initialCapacity);
  }

  public LinkedHashMapZeroFlow() {

  }

  public LinkedHashMapZeroFlow(Map<? extends K, ? extends V> m) {

    super(m);
  }

  @Override
  public SetZeroFlow<K> keysSeq() {

    return SetZeroFlow.of(keySet());
  }

  @Override
  public CollectionZeroFlow<V> valuesSeq() {

    return CollectionZeroFlow.of(values());
  }

  @Override
  public <A, B> MapZeroFlow<A, B> newForMapping() {

    return new LinkedHashMapZeroFlow<>(size());
  }

  @Override
  public SetZeroFlow<Entry<K, V>> entrySeq() {

    return SetZeroFlow.of(entrySet());
  }

}
