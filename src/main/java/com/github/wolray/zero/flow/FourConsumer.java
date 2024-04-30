package com.github.wolray.zero.flow;

/**
 * @author wolray
 */
public interface FourConsumer<A, B, C, D> {

  void accept(A a, B b, C c, D d);

}
