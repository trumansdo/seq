package com.github.wolray.seq;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * reduce操作时的转换器，对应{@link Collector#finisher()} 方法
 *
 * @author wolray
 */
public interface Transducer<T, V, E> {

    static <T, V, E> Transducer<T, V, E> of(Collector<T, V, E> collector) {

        return of(Reducer.of(collector.supplier(), collector.accumulator()), collector.finisher());
    }

    static <T, V, E> Transducer<T, V, E> of(Reducer<T, V> reducer, Function<V, E> transformer) {

      return new Transducer<T, V, E>() {

        @Override
            public Reducer<T, V> reducer() {

          return reducer;
            }

            @Override
            public Function<V, E> transformer() {

              return transformer;
            }
        };
    }

  /**
   * 不变换类型的reduce操作，比如数值的计算
   */
  static <T> Transducer<T, ?, T> of(BinaryOperator<T> binaryOperator) {

    return of(() -> new Mutable<T>(null), (m, t) -> {
      if (m.isSet) {
        m.it = binaryOperator.apply(m.it, t);
      } else {
        m.set(t);
      }
    }, Mutable::get);
  }

  static <T, V, E> Transducer<T, V, E> of(
      Supplier<V> supplier, BiConsumer<V, T> accumulator, Function<V, E> transformer
  ) {

    return of(Reducer.of(supplier, accumulator), transformer);
    }

  /**
   * map/reduce理论中的reduce收束/归约/归纳器
   */
  Reducer<T, V> reducer();

  /**
   * 对应{@link Collector#finisher()} 方法。 <br/> 将reduce的最终结果再进行一次转换
   */
  Function<V, E> transformer();

}
