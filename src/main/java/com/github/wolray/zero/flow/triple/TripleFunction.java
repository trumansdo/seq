package com.github.wolray.zero.flow.triple;

/**
 * @author wolray
 */
public interface TripleFunction<A, B, C, T> {

  T apply(A a, B b, C c);

}
