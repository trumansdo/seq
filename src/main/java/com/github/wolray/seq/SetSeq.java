package com.github.wolray.seq;

import java.util.Set;

/**
 * @author wolray
 */
public interface SetSeq<T> extends SizedSeq<T>, Set<T> {
    static <T> SetSeq<T> of(Set<T> set) {
        return set instanceof SetSeq ? (SetSeq<T>)set : new Proxy<>(set);
    }

    class Proxy<T> extends CollectionSeq.Proxy<T, Set<T>> implements SetSeq<T> {
        public Proxy(Set<T> backer) {
            super(backer);
        }
    }
}
