package com.github.wolray.zero.flow;

/**
 * @author wolray
 */
public interface FourFunction<A, B, C, D, T> {

  T apply(A a, B b, C c, D d);

}
