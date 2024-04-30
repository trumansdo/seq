package com.github.wolray.zero.flow.triple;

import com.github.wolray.zero.flow.BaseZeroFlow;
import com.github.wolray.zero.flow.ZeroFlow;
import java.util.function.Function;

/**
 * 三元流，暂时无对应的
 *
 * @author wolray
 */
public interface TripleZeroFlow<A, B, C> extends BaseZeroFlow<TripleConsumer<A, B, C>> {

  @SuppressWarnings("unchecked")
  static <A, B, C> TripleZeroFlow<A, B, C> empty() {

    return (TripleZeroFlow<A, B, C>) Empty.emptySeq;
  }

  @SuppressWarnings("unchecked")
  static <A, B, C> TripleConsumer<A, B, C> nothing() {

    return (TripleConsumer<A, B, C>) Empty.nothing;
  }

  default TripleZeroFlow<A, B, C> filter(TriPredicate<A, B, C> predicate) {

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
      ZeroFlow.stop();
    });
    return t;
  }

  default ZeroFlow<A> keepFirst() {

    return cs -> consume((a, b, c) -> cs.accept(a));
  }

  default ZeroFlow<B> keepSecond() {

    return cs -> consume((a, b, c) -> cs.accept(b));
  }

  default ZeroFlow<C> keepThird() {

    return cs -> consume((a, b, c) -> cs.accept(c));
  }

  default <T> TripleZeroFlow<T, B, C> mapFirst(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(function.apply(a, b, c), b, c));
  }

  default <T> TripleZeroFlow<T, B, C> mapFirst(Function<A, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(function.apply(a), b, c));
  }

  default <T> TripleZeroFlow<A, T, C> mapSecond(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, function.apply(a, b, c), c));
  }

  default <T> TripleZeroFlow<A, T, C> mapSecond(Function<B, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, function.apply(b), c));
  }

  default <T> TripleZeroFlow<A, B, T> mapThird(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, b, function.apply(a, b, c)));
  }

  default <T> TripleZeroFlow<A, B, T> mapThird(Function<C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(a, b, function.apply(c)));
  }

  default ZeroFlow<Triple<A, B, C>> tripled() {

    return map(Triple::new);
  }

  default <T> ZeroFlow<T> map(TripleFunction<A, B, C, T> function) {

    return cs -> consume((a, b, c) -> cs.accept(function.apply(a, b, c)));
  }

  interface TriPredicate<A, B, D> {

    boolean test(A a, B b, D d);

  }

  class Empty {

    static final TripleZeroFlow<Object, Object, Object> emptySeq = cs -> {
    };

    static final TripleConsumer<Object, Object, Object> nothing = (a, b, c) -> {
    };

  }

}
