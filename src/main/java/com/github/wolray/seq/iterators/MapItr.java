package com.github.wolray.seq.iterators;

import java.util.Iterator;

/**
 * 可在获取每个数据时对数据进行map/reduce操作里面的map转换操作的迭代器
 *
 * @author wolray
 */
public abstract class MapItr<T, E> implements Iterator<E> {

  private final Iterator<T> iterator;

  public MapItr(Iterator<T> iterator) {

    this.iterator = iterator;
  }

  @Override
  public boolean hasNext() {

    return iterator.hasNext();
  }

  @Override
  public E next() {

    return apply(iterator.next());
  }

  /**
   * 在迭代过程中对每个数据进行转换操作
   *
   * @return {@link E }
   */
  public abstract E apply(T t);

}
