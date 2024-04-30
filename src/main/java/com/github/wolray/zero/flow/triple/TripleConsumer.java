package com.github.wolray.zero.flow.triple;

/**
 * @author wolray
 */
public interface TripleConsumer<A, B, C> {

  void accept(A a, B b, C c);

}
