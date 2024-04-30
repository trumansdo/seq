package com.github.wolray.seq;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * ���ٽ�{@link List}ת��������
 *
 * @author wolray
 */
public interface ListSeq<T> extends SizedSeq<T>, List<T> {
    static <T> ListSeq<T> of(List<T> list) {

      return list instanceof ListSeq ? (ListSeq<T>) list : new Proxy<>(list);
    }

    class Proxy<T> extends CollectionSeq.Proxy<T, List<T>> implements ListSeq<T> {
        public Proxy(List<T> backer) {
            super(backer);
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            return backer.addAll(c);
        }

        @Override
        public T get(int index) {
            return backer.get(index);
        }

        @Override
        public T set(int index, T element) {
            return backer.set(index, element);
        }

        @Override
        public void add(int index, T element) {
            backer.add(index, element);
        }

        @Override
        public T remove(int index) {
            return backer.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return backer.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return backer.lastIndexOf(o);
        }

        @Override
        public ListIterator<T> listIterator() {
            return backer.listIterator();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            return backer.listIterator(index);
        }

        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            return backer.subList(fromIndex, toIndex);
        }
    }
}
