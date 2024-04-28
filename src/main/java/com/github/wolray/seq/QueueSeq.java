package com.github.wolray.seq;

import java.util.Queue;

/**
 * @author wolray
 */
public interface QueueSeq<T> extends SizedSeq<T>, Queue<T> {
    static <T> QueueSeq<T> of(Queue<T> queue) {
        return queue instanceof QueueSeq ? (QueueSeq<T>)queue : new Proxy<>(queue);
    }

    class Proxy<T> extends CollectionSeq.Proxy<T, Queue<T>> implements QueueSeq<T> {
        public Proxy(Queue<T> backer) {
            super(backer);
        }

        @Override
        public boolean offer(T t) {
            return backer.offer(t);
        }

        @Override
        public T remove() {
            return backer.remove();
        }

        @Override
        public T poll() {
            return backer.poll();
        }

        @Override
        public T element() {
            return backer.element();
        }

        @Override
        public T peek() {
            return backer.peek();
        }
    }
}
