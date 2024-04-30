package com.github.wolray.zero.flow.iterators;

import com.github.wolray.zero.flow.ZeroFlow;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 定制获取下一个数据方式的迭代器 <br/> 实现{@link #pick()} 方法可做到丰富的从可迭代容器里获取数据的方式，比如跳过n个数据再获取、每隔n个获取、一定条件获取
 *
 * @author wolray
 */
public abstract class PickItr<T> implements Iterator<T> {

  private T next;

  private State state = State.Unset;

  @Override
  public boolean hasNext() {

    if (state == State.Unset) {
      try {
        next  = pick();
        state = State.Cached;
      } catch (NoSuchElementException e) {
        state = State.Done;
      }
    }
    return state == State.Cached;
  }

  /**
   * 获取下一个数据的方法，想法可以丰富些
   */
  public abstract T pick();

  @Override
  public T next() {

    if (state == State.Cached) {
      T res = next;
      next  = null;
      state = State.Unset;
      return res;
    } else {
      return ZeroFlow.stop();
    }
  }

  private enum State {
    Unset,
    Cached,
    Done
  }

}
