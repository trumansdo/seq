package com.github.wolray.zero.flow;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author wolray
 */
public interface ExpandSeq<T> extends Function<T, ZeroFlow<T>> {

  static <T> ExpandSeq<T> of(Function<T, ZeroFlow<T>> function) {

    return function instanceof ExpandSeq ? (ExpandSeq<T>) function : function::apply;
  }

  default ExpandSeq<T> filter(Predicate<T> predicate) {

    return t -> apply(t).filter(predicate);
  }

  default ExpandSeq<T> filterNot(Predicate<T> predicate) {

    return t -> apply(t).filter(predicate.negate());
  }

  default void scan(BiConsumer<T, ArrayListZeroFlow<T>> c, T node) {

    ArrayListZeroFlow<T> sub = apply(node).filterNotNull().toList();
    c.accept(node, sub);
    sub.consume(n -> scan(c, n));
  }

  default void scan(Consumer<T> c, T node) {

    c.accept(node);
    apply(node).consume(n -> {
      if (n != null) {
        scan(c, n);
      }
    });
  }

  default void scan(Consumer<T> c, T node, int maxDepth, int depth) {

    c.accept(node);
    if (depth < maxDepth) {
      apply(node).consume(n -> {
        if (n != null) {
          scan(c, n, maxDepth, depth + 1);
        }
      });
    }
  }

  default ExpandSeq<T> terminate(Predicate<T> predicate) {

    return t -> predicate.test(t) ? ZeroFlow.empty() : apply(t);
  }

  default Map<T, ArrayListZeroFlow<T>> toDAG(ZeroFlow<T> nodes) {

    Map<T, ArrayListZeroFlow<T>> map    = new HashMap<>();
    ExpandSeq<T>                 expand = terminate(t -> !map.containsKey(t));
    nodes.consume(t -> expand.scan(map::put, t));
    return map;
  }

  default Map<T, ArrayListZeroFlow<T>> toDAG(T node) {

    Map<T, ArrayListZeroFlow<T>> map = new HashMap<>();
    terminate(t -> !map.containsKey(t)).scan(map::put, node);
    return map;
  }

  default ZeroFlow<T> toSeq(T node) {

    return c -> scan(c, node);
  }

  default ZeroFlow<T> toSeq(T node, int maxDepth) {

    return c -> scan(c, node, maxDepth, 0);
  }

}