package com.github.wolray.seq.triple;

/**
 * @author wolray
 */
public interface TripleFunction<A, B, C, T> {
    T apply(A a, B b, C c);
}
