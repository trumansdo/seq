package com.github.wolray.seq;

/**
 * @author wolray
 */
public interface FiveFunction<A, B, C, D, E, T> {

  T apply(A a, B b, C c, D d, E e);

}
