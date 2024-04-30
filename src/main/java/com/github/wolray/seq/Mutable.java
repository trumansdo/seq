package com.github.wolray.seq;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

/**
 * 可变的数据对象存放容器
 *
 * @author wolray
 */
public class Mutable<T> implements Lazy<T> {

  protected boolean isSet = false;

  protected T it;

  public Mutable(T it) {

    this.it = it;
  }

  @Override
  public final T get() {

    if (isSet) {
      return it;
    }
    eval();
    isSet = true;
    return it;
  }

  /**
   * 计算字段{@link #it}的值
   */
  protected void eval() {

  }

  @Override
  public boolean isSet() {

    return isSet;
  }

  @Override
  public synchronized final T forkJoin(ForkJoinPool pool) {

    if (isSet) {
      return it;
    }
    eval(pool);
    isSet = true;
    return it;
  }

  protected void eval(ForkJoinPool pool) {

    eval();
  }

  @Override
  public T set(T value) {

    isSet = true;
    return this.it = value;
  }

  public Optional<T> toOptional() {

    return isSet ? Optional.ofNullable(it) : Optional.empty();
  }

  public T getIt() {

    return it;
  }

  public void setIt(T it) {

    this.it = it;
  }

}
