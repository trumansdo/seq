package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * ��һ����ά���鰴�д������
 *
 * @author wolray
 */
public class BatchedSeq<T> implements SizedSeq<T> {

  private transient final LinkedList<ArrayList<T>> list = new LinkedList<>();

  /**
   * �����е������������300
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
   * �Զ�ά���ݽ����������
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
          //�����е������������300��ÿ��������������
          batchSize = Math.min(300, Math.max(batchSize, size >> 1));
        }
    }

    @Override
    public String toString() {
        return toList().toString();
    }
}
