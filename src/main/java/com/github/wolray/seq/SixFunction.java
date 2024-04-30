package com.github.wolray.seq;

/**
 * @author wolray
 */
public interface SixFunction<A, B, C, D, E, F, T> {

  T apply(A a, B b, C c, D d, E e, F f);

}
