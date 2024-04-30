package com.github.wolray.seq.triple;

import com.github.wolray.seq.BaseSeq;
import com.github.wolray.seq.Seq;
import java.util.function.Function;

/**
 * 三元流，暂时无对应的
 *
 * @author wolray
 */
public interface TripleSeq<A, B, C> extends BaseSeq<TripleConsumer<A, B, C>> {

  @SuppressWarnings("unchecked")
  static <A, B, C> TripleSeq<A, B, C> empty() {

    return (TripleSeq<A, B, C>) Empty.emptySeq;
  }

  @SuppressWarnings("unchecked")
  static <A, B, C> TripleConsumer<A, B, C> nothing() {

    return (TripleConsumer<A, B, C>) Empty.nothing;
  }

  default TripleSeq<A, B, C> filter(TriPredicate<A, B, C> predicate) {

    return cs -> consume((a, b, c) -> {
      if (predicate.test(a, b, c)) {
        cs.accept(a, b, c);
      }
    });
  }

  default Triple<A, B, C> first() {

    Triple<A, B, C> t = new Triple<>(null, null, null);
    consumeTillStop((a, b, c) -> {
      t.first  = a;
      t.second = b;
      t.third  = c;
      Seq.stop();
    });
    return t;
  }

  default Seq<A> keepFirst() {

    return cs -> consume((a, b, c) -> cs.accept(a));
  }

  default Seq<B> keepSecond() {

    return cs -> consume((a, b, c) -> cs.accept(b));
  }

  default Seq<C> keepThird() {

    return cs -> consume((a, b, c) -> cs.accept(c));
  }

  default <T> TripleSeq<T, B, C> mapFirst(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(function.apply(a, b, c), b, c));
  }

  default <T> TripleSeq<T, B, C> mapFirst(Function<A, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(function.apply(a), b, c));
  }

  default <T> TripleSeq<A, T, C> mapSecond(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, function.apply(a, b, c), c));
  }

  default <T> TripleSeq<A, T, C> mapSecond(Function<B, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, function.apply(b), c));
  }

  default <T> TripleSeq<A, B, T> mapThird(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, b, function.apply(a, b, c)));
  }

  default <T> TripleSeq<A, B, T> mapThird(Function<C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, b, function.apply(c)));
  }

  default Seq<Triple<A, B, C>> tripled() {

    return map(Triple::new);
  }

  default <T> Seq<T> map(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(function.apply(a, b, c)));
  }

  interface TriPredicate<A, B, D> {

    boolean test(A a, B b, D d);

  }

  class Empty {

    static final TripleSeq<Object, Object, Object> emptySeq = cs -> {
    };

    static final TripleConsumer<Object, Object, Object> nothing = (a, b, c) -> {
    };

  }

}
