package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * 对一个二维数组按行处理的流
 *
 * @author wolray
 */
public class BatchedZeroFlow<T> implements SizedZeroFlow<T> {

  private transient final LinkedList<ArrayList<T>> list = new LinkedList<>();

  /**
   * 控制行的数量，最大是300
   */
  private transient int batchSize = 10;

  private transient int size;

  private transient ArrayList<T> cur;

  @Override
  public void consume(Consumer<T> consumer) {

    list.forEach(ls -> ls.forEach(consumer));
  }

  @Override
  public Iterator<T> iterator() {

    return new Iterator<T>() {

      final Iterator<ArrayList<T>> iterator = list.iterator();

      Iterator<T> cur = Collections.emptyIterator();

      @Override
      public boolean hasNext() {

        if (!cur.hasNext()) {
          if (!iterator.hasNext()) {
            return false;
          }
          cur = iterator.next().iterator();
        }
        return true;
      }

      @Override
      public T next() {

        return cur.next();
      }
    };
  }

  @Override
  public boolean isEmpty() {

    return size == 0;
  }

  @Override
  public int size() {

    return size;
  }

  /**
   * 对二维数据进行逐行添加
   */
  public void add(T t) {

    if (cur == null) {
      cur = new ArrayList<>(batchSize);
      list.add(cur);
    }
    cur.add(t);
    size++;
    if (cur.size() == batchSize) {
      cur = null;
      //控制行的数量，最大是300。每行容量翻倍扩容
      batchSize = Math.min(300, Math.max(batchSize, size >> 1));
    }
  }

  @Override
  public String toString() {

    return toList().toString();
  }

}
