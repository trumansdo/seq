package com.github.wolray.seq;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * @author wolray
 */
public class LinkedSetSeq<T> extends LinkedHashSet<T> implements SetSeq<T> {
    public LinkedSetSeq(int initialCapacity) {
        super(initialCapacity);
    }

    public LinkedSetSeq() {}

    public LinkedSetSeq(Collection<? extends T> c) {
        super(c);
    }
}
