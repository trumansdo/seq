package com.github.wolray.seq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * 既是流，也是{@link ArrayList}
 *
 * @author wolray
 */
public class ArrayListSeq<T> extends ArrayList<T> implements ListSeq<T> {

  public ArrayListSeq(int initialCapacity) {

    super(initialCapacity);
  }

  public ArrayListSeq() {

  }

  public ArrayListSeq(Collection<? extends T> c) {

    super(c);
  }

  public void swap(int i, int j) {

    T t = get(i);
    set(i, get(j));
    set(j, t);
  }

  public Seq<ArrayListSeq<T>> permute(boolean inplace) {

    return c -> permute(c, inplace, 0);
  }

  private void permute(Consumer<ArrayListSeq<T>> c, boolean inplace, int i) {
    // 感觉是给快速排序用的
    int n = size();
    if (i == n) {
      c.accept(inplace ? this : new ArrayListSeq<>(this));
      return;
    }
    for (int j = i; j < n; j++) {
      swap(i, j);
      permute(c, inplace, i + 1);
      swap(i, j);
    }
  }

}

