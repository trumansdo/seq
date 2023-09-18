package com.github.wolray.seq;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wolray
 */
public interface Seq<T> extends Seq0<Consumer<T>> {
    @SuppressWarnings("unchecked")
    static <T> Seq<T> empty() {
        return (Seq<T>)Empty.emptySeq;
    }

    static <T> Seq<T> flat(Seq<Optional<T>> seq) {
        return c -> seq.consume(o -> o.ifPresent(c));
    }

    @SafeVarargs
    static <T> Seq<T> flat(Seq<T>... seq) {
        return c -> {
            for (Seq<T> s : seq) {
                s.consume(c);
            }
        };
    }

    static <T> ItrSeq<T> flatIterable(Iterable<Optional<T>> iterable) {
        return () -> ItrUtil.flatOptional(iterable.iterator());
    }

    @SafeVarargs
    static <T> ItrSeq<T> flatIterable(Iterable<T>... iterables) {
        return () -> ItrUtil.flat(Arrays.asList(iterables).iterator());
    }

    static <T> ItrSeq<T> gen(Supplier<T> supplier) {
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return supplier.get();
            }
        };
    }

    static <T> Seq<T> gen(T seed, UnaryOperator<T> operator) {
        return c -> {
            T t = seed;
            c.accept(t);
            while (true) {
                c.accept(t = operator.apply(t));
            }
        };
    }

    static <T> Seq<T> gen(T seed1, T seed2, BinaryOperator<T> operator) {
        return c -> {
            T t1 = seed1, t2 = seed2;
            c.accept(t1);
            c.accept(t2);
            while (true) {
                c.accept(t2 = operator.apply(t1, t1 = t2));
            }
        };
    }

    static ItrSeq<Matcher> match(String s, Pattern pattern) {
        return () -> new Iterator<Matcher>() {
            Matcher matcher = pattern.matcher(s);

            @Override
            public boolean hasNext() {
                return matcher.find();
            }

            @Override
            public Matcher next() {
                return matcher;
            }
        };
    }

    @SuppressWarnings("unchecked")
    static <T> Consumer<T> nothing() {
        return (Consumer<T>)Empty.nothing;
    }

    static <T> Seq<T> of(Iterable<T> iterable) {
        if (iterable instanceof ItrSeq) {
            return (ItrSeq<T>)iterable;
        }
        if (iterable instanceof Collection) {
            return new BackedSeq<>((Collection<T>)iterable);
        }
        return (ItrSeq<T>)iterable::iterator;
    }

    static <T> Seq<T> of(Optional<T> optional) {
        return optional::ifPresent;
    }

    @SafeVarargs
    static <T> Seq<T> of(T... ts) {
        return new BackedSeq<>(Arrays.asList(ts));
    }

    static Seq<Object> ofJson(Object node) {
        return Seq.ofTree(node, n -> c -> {
            if (n instanceof Iterable) {
                ((Iterable<?>)n).forEach(c);
            } else if (n instanceof Map) {
                ((Map<?, ?>)n).values().forEach(c);
            }
        });
    }

    static <N> Seq<N> ofTree(int maxDepth, N node, Function<N, Seq<N>> sub) {
        return c -> SeqUtil.scanTree(c, maxDepth, 0, node, sub);
    }

    static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {
        return c -> SeqUtil.scanTree(c, node, sub);
    }

    static <T> ItrSeq<T> repeat(int n, T t) {
        return () -> new Iterator<T>() {
            int i = n;

            @Override
            public boolean hasNext() {
                return i > 0;
            }

            @Override
            public T next() {
                i--;
                return t;
            }
        };
    }

    static <T> T stop() {
        throw StopException.INSTANCE;
    }

    static <T> ItrSeq<T> tillNull(Supplier<T> supplier) {
        return () -> new PickItr<T>() {
            @Override
            public T pick() {
                T t = supplier.get();
                return t != null ? t : Seq.stop();
            }
        };
    }

    static <T> Seq<T> unit(T t) {
        return c -> c.accept(t);
    }

    default boolean all(Predicate<T> predicate) {
        return !find(predicate.negate()).isPresent();
    }

    default boolean any(Predicate<T> predicate) {
        return find(predicate).isPresent();
    }

    default boolean anyNot(Predicate<T> predicate) {
        return any(predicate.negate());
    }

    default Seq<T> append(T t) {
        return c -> {
            consume(c);
            c.accept(t);
        };
    }

    @SuppressWarnings("unchecked")
    default Seq<T> append(T... t) {
        return c -> {
            consume(c);
            for (T x : t) {
                c.accept(x);
            }
        };
    }

    default Seq<T> appendAll(Iterable<T> iterable) {
        return c -> {
            consume(c);
            iterable.forEach(c);
        };
    }

    default Seq<T> appendWith(Seq<T> seq) {
        return c -> {
            consume(c);
            seq.consume(c);
        };
    }

    default ItrSeq<T> asIterable() {
        return toList();
    }

    default double average(ToDoubleFunction<T> function) {
        return average(function, null);
    }

    default double average(ToDoubleFunction<T> function, ToDoubleFunction<T> weightFunction) {
        return reduce(Reducer.average(function, weightFunction));
    }

    default SizedSeq<T> cache() {
        return toList();
    }

    default Seq<ArraySeq<T>> chunked(int size) {
        return c -> {
            ArraySeq<T> last = fold(null, (ts, t) -> {
                if (ts == null) {
                    ts = new ArraySeq<>(size);
                } else if (ts.size() >= size) {
                    c.accept(ts);
                    ts = new ArraySeq<>(size);
                }
                ts.add(t);
                return ts;
            });
            if (last != null) {
                c.accept(last);
            }
        };
    }

    default Seq<T> circle() {
        return c -> {
            while (true) {
                consume(c);
            }
        };
    }

    default <C extends Collection<T>> C collectBy(IntFunction<C> constructor) {
        return reduce(constructor.apply(sizeOrDefault()), Collection::add);
    }

    default void consume(Consumer<T> consumer, int n, Consumer<T> substitute) {
        if (n > 0) {
            int[] a = {n - 1};
            consume(t -> {
                if (a[0] < 0) {
                    consumer.accept(t);
                } else {
                    a[0]--;
                    substitute.accept(t);
                }
            });
        } else {
            consume(consumer);
        }
    }

    default void consumeIndexed(IndexObjConsumer<T> consumer) {
        int[] a = {0};
        consume(t -> consumer.accept(a[0]++, t));
    }

    default void consumeIndexedTillStop(IndexObjConsumer<T> consumer) {
        int[] a = {0};
        consumeTillStop(t -> consumer.accept(a[0]++, t));
    }

    default int count() {
        return reduce(Reducer.count());
    }

    default int count(Predicate<T> predicate) {
        return reduce(Reducer.count(predicate));
    }

    default int countNot(Predicate<T> predicate) {
        return reduce(Reducer.countNot(predicate));
    }

    default Seq<T> distinct() {
        return c -> {
            Set<T> set = new HashSet<>();
            consume(t -> {
                if (set.add(t)) {
                    c.accept(t);
                }
            });
        };
    }

    default <E> Seq<T> distinctBy(Function<T, E> function) {
        return c -> reduce(new HashSet<>(), (set, t) -> {
            if (set.add(function.apply(t))) {
                c.accept(t);
            }
        });
    }

    default Seq<T> drop(int n) {
        return n <= 0 ? this : partial(n, nothing());
    }

    default Seq<T> dropWhile(Predicate<T> predicate) {
        return c -> foldBoolean(false, (b, t) -> {
            if (b || !predicate.test(t)) {
                c.accept(t);
                return true;
            }
            return false;
        });
    }

    default Seq<T> duplicateAll(int times) {
        return c -> {
            for (int i = 0; i < times; i++) {
                consume(c);
            }
        };
    }

    default Seq<T> duplicateEach(int times) {
        return c -> consume(t -> {
            for (int i = 0; i < times; i++) {
                c.accept(t);
            }
        });
    }

    default Seq<T> duplicateIf(int times, Predicate<T> predicate) {
        return c -> consume(t -> {
            if (predicate.test(t)) {
                for (int i = 0; i < times; i++) {
                    c.accept(t);
                }
            } else {
                c.accept(t);
            }
        });
    }

    default Seq<T> filter(int n, Predicate<T> predicate) {
        return predicate == null ? this : c -> consume(c, n, t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filter(Predicate<T> predicate) {
        return predicate == null ? this : c -> consume(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            }
        });
    }

    default Seq<T> filterIn(Collection<T> collection) {
        return collection == null ? this : filter(collection::contains);
    }

    default Seq<T> filterIn(Map<T, ?> map) {
        return map == null ? this : filter(map::containsKey);
    }

    default Seq<T> filterIndexed(IndexObjPredicate<T> predicate) {
        return predicate == null ? this : c -> consumeIndexed((i, t) -> {
            if (predicate.test(i, t)) {
                c.accept(t);
            }
        });
    }

    default <E> Seq<E> filterInstance(Class<E> cls) {
        return c -> consume(t -> {
            if (cls.isInstance(t)) {
                c.accept(cls.cast(t));
            }
        });
    }

    default Seq<T> filterNot(Predicate<T> predicate) {
        return predicate == null ? this : filter(predicate.negate());
    }

    default Seq<T> filterNotIn(Collection<T> collection) {
        return collection == null ? this : filterNot(collection::contains);
    }

    default Seq<T> filterNotIn(Map<T, ?> map) {
        return map == null ? this : filterNot(map::containsKey);
    }

    default Seq<T> filterNotNull() {
        return filter(Objects::nonNull);
    }

    default Optional<T> find(Predicate<T> predicate) {
        Mutable<T> m = new Mutable<>(null);
        consumeTillStop(t -> {
            if (predicate.test(t)) {
                m.set(t);
                stop();
            }
        });
        return m.toOptional();
    }

    default Optional<T> findDuplicate() {
        Set<T> set = new HashSet<>(sizeOrDefault());
        return find(t -> !set.add(t));
    }

    default Optional<T> findNot(Predicate<T> predicate) {
        return find(predicate.negate());
    }

    default T first() {
        Mutable<T> m = new Mutable<>(null);
        consumeTillStop(t -> {
            m.it = t;
            stop();
        });
        return m.it;
    }

    default Optional<T> firstMaybe() {
        return find(t -> true);
    }

    default <E> Seq<E> flatIterable(Function<T, Iterable<E>> function) {
        return c -> consume(t -> function.apply(t).forEach(c));
    }

    default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {
        return c -> consume(t -> function.apply(t).consume(c));
    }

    default <E> Seq<E> flatOptional(Function<T, Optional<E>> function) {
        return c -> consume(t -> function.apply(t).ifPresent(c));
    }

    default <E> E fold(E init, BiFunction<E, T, E> function) {
        Mutable<E> m = new Mutable<>(init);
        consume(t -> m.it = function.apply(m.it, t));
        return m.it;
    }

    default int foldInt(int init, IntObjToInt<T> function) {
        int[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default double foldDouble(double init, DoubleObjToDouble<T> function) {
        double[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default long foldLong(long init, LongObjToLong<T> function) {
        long[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default boolean foldBoolean(boolean init, BooleanObjToBoolean<T> function) {
        boolean[] a = {init};
        consume(t -> a[0] = function.apply(a[0], t));
        return a[0];
    }

    default <E> E foldAtomic(E init, BiFunction<E, T, E> function) {
        AtomicReference<E> m = new AtomicReference<>(init);
        consume(t -> m.updateAndGet(e -> function.apply(e, t)));
        return m.get();
    }

    default String join(String sep) {
        return join(sep, Object::toString);
    }

    default String join(String sep, Function<T, String> function) {
        return reduce(new StringJoiner(sep), (j, t) -> j.add(function.apply(t))).toString();
    }

    default T last() {
        return reduce(new Mutable<T>(null), Mutable::set).it;
    }

    default Optional<T> last(Predicate<T> predicate) {
        return filter(predicate).lastMaybe();
    }

    default Optional<T> lastMaybe() {
        Mutable<T> m = new Mutable<>(null);
        consume(m::set);
        return m.toOptional();
    }

    default Optional<T> lastNot(Predicate<T> predicate) {
        return last(predicate.negate());
    }

    default <E> Seq<E> map(Function<T, E> function) {
        return c -> consume(t -> c.accept(function.apply(t)));
    }

    default <E> Seq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {
        return n <= 0 ? map(function) : c -> {
            int[] a = {n - 1};
            consume(t -> {
                if (a[0] < 0) {
                    c.accept(function.apply(t));
                } else {
                    a[0]--;
                    c.accept(substitute.apply(t));
                }
            });
        };
    }

    default <E> Seq<E> mapIndexed(IndexObjFunction<T, E> function) {
        return c -> consumeIndexed((i, t) -> c.accept(function.apply(i, t)));
    }

    default <E> Seq<E> mapMaybe(Function<T, E> function) {
        return c -> consume(t -> {
            if (t != null) {
                c.accept(function.apply(t));
            }
        });
    }

    default <E> Seq<E> mapNotNull(Function<T, E> function) {
        return c -> consume(t -> {
            E e = function.apply(t);
            if (e != null) {
                c.accept(e);
            }
        });
    }

    default <V> Seq<V> mapSub(Predicate<T> first, Predicate<T> last, Reducer<T, V> reducer) {
        Supplier<V> supplier = reducer.supplier();
        BiConsumer<V, T> accumulator = reducer.accumulator();
        return c -> fold((V)null, (v, t) -> {
            if (v == null && first.test(t)) {
                v = supplier.get();
                accumulator.accept(v, t);
            } else if (v != null) {
                accumulator.accept(v, t);
                if (last.test(t)) {
                    c.accept(v);
                    return null;
                }
            }
            return v;
        });
    }

    default Seq<ArraySeq<T>> mapSub(Predicate<T> takeWhile) {
        return mapSub(takeWhile, Reducer.toList());
    }

    default <V> Seq<V> mapSub(Predicate<T> takeWhile, Reducer<T, V> reducer) {
        Supplier<V> supplier = reducer.supplier();
        BiConsumer<V, T> accumulator = reducer.accumulator();
        return c -> {
            V last = fold(null, (v, t) -> {
                if (takeWhile.test(t)) {
                    if (v == null) {
                        v = supplier.get();
                    }
                    accumulator.accept(v, t);
                    return v;
                } else {
                    if (v != null) {
                        c.accept(v);
                    }
                    return null;
                }
            });
            if (last != null) {
                c.accept(last);
            }
        };
    }

    default Seq<ArraySeq<T>> mapSub(T first, T last) {
        return mapSub(first, last, Reducer.toList());
    }

    default <V> Seq<V> mapSub(T first, T last, Reducer<T, V> reducer) {
        return mapSub(first::equals, last::equals, reducer);
    }

    default <V> Seq<V> mapWindow(long timeWindow, Reducer<T, V> reducer) {
        return c -> {
            Supplier<V> supplier = reducer.supplier();
            BiConsumer<V, T> accumulator = reducer.accumulator();
            Consumer<V> finisher = reducer.finisher();
            Mutable<V> acc = new Mutable<>(supplier.get());
            long[] start = {System.currentTimeMillis()};
            consume(t -> {
                long now = System.currentTimeMillis();
                if (now - start[0] > timeWindow) {
                    start[0] = now;
                    if (finisher != null) {
                        finisher.accept(acc.it);
                    }
                    c.accept(acc.it);
                    acc.it = supplier.get();
                }
                accumulator.accept(acc.it, t);
            });
        };
    }

    default <V, E> Seq<E> mapWindow(long timeWindow, Transducer<T, V, E> transducer) {
        return mapWindow(timeWindow, transducer.reducer()).map(transducer.transformer());
    }

    default T max(Comparator<T> comparator) {
        return reduce(Reducer.max(comparator));
    }

    default T min(Comparator<T> comparator) {
        return reduce(Reducer.min(comparator));
    }

    default boolean none(Predicate<T> predicate) {
        return !find(predicate).isPresent();
    }

    default Seq<T> onEach(Consumer<T> consumer) {
        return c -> consume(consumer.andThen(c));
    }

    default Seq<T> onEach(int n, Consumer<T> consumer) {
        return c -> consume(c, n, consumer.andThen(c));
    }

    default Seq<T> onEachIndexed(IndexObjConsumer<T> consumer) {
        return c -> consumeIndexed((i, t) -> {
            consumer.accept(i, t);
            c.accept(t);
        });
    }

    default Seq<T> partial(int n, Consumer<T> substitute) {
        return c -> consume(c, n, substitute);
    }

    default void printAll(String sep) {
        if ("\n".equals(sep)) {
            println();
        } else {
            System.out.println(join(sep));
        }
    }

    default void println() {
        consume(System.out::println);
    }

    default T reduce(BinaryOperator<T> binaryOperator) {
        return reduce(Reducer.fold(binaryOperator));
    }

    default <E> E reduce(E des, BiConsumer<E, T> accumulator) {
        consume(t -> accumulator.accept(des, t));
        return des;
    }

    default <E> E reduce(Reducer<T, E> reducer) {
        E des = reducer.supplier().get();
        BiConsumer<E, T> accumulator = reducer.accumulator();
        consume(t -> accumulator.accept(des, t));
        Consumer<E> finisher = reducer.finisher();
        if (finisher != null) {
            finisher.accept(des);
        }
        return des;
    }

    default <E, V> E reduce(Reducer<T, V> reducer, Function<V, E> transformer) {
        return transformer.apply(reduce(reducer));
    }

    default <E, V> E reduce(Transducer<T, V, E> transducer) {
        return transducer.transformer().apply(reduce(transducer.reducer()));
    }

    default Seq<T> replace(int n, UnaryOperator<T> operator) {
        return c -> consume(c, n, t -> c.accept(operator.apply(t)));
    }

    default ArraySeq<T> reverse() {
        return reduce(Reducer.reverse());
    }

    default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {
        return c -> fold(init, (e, t) -> {
            e = function.apply(e, t);
            c.accept(e);
            return e;
        });
    }

    default int sizeOrDefault() {
        return 10;
    }

    default <E extends Comparable<E>> ArraySeq<T> sortBy(Function<T, E> function) {
        return sortWith(Comparator.comparing(function));
    }

    default <E extends Comparable<E>> ArraySeq<T> sortByDesc(Function<T, E> function) {
        return sortWith(Comparator.comparing(function).reversed());
    }

    default ArraySeq<T> sorted() {
        return sortWith(null);
    }

    default ArraySeq<T> sortedDesc() {
        return sortWith(Collections.reverseOrder());
    }

    default ArraySeq<T> sortWith(Comparator<T> comparator) {
        ArraySeq<T> list = toList();
        list.sort(comparator);
        return list;
    }

    default ArraySeq<T> sortWithDesc(Comparator<T> comparator) {
        return sortWith(comparator.reversed());
    }

    default double sum(ToDoubleFunction<T> function) {
        return reduce(Reducer.sum(function));
    }

    default int sumInt(ToIntFunction<T> function) {
        return reduce(Reducer.sumInt(function));
    }

    default long sumLong(ToLongFunction<T> function) {
        return reduce(Reducer.sumLong(function));
    }

    default Seq<T> take(int n) {
        return n <= 0 ? empty() : c -> {
            int[] i = {n};
            consumeTillStop(t -> {
                c.accept(t);
                if (--i[0] == 0) {
                    stop();
                }
            });
        };
    }

    default Seq<T> takeWhile(BiPredicate<T, T> testPrevCurr) {
        return takeWhile(t -> t, testPrevCurr);
    }

    default <E> Seq<T> takeWhile(Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return c -> {
            Mutable<E> m = new Mutable<>(null);
            consumeTillStop(t -> {
                E curr = function.apply(t);
                if (m.it == null || testPrevCurr.test(m.it, curr)) {
                    c.accept(t);
                    m.it = curr;
                } else {
                    stop();
                }
            });
        };
    }

    default Seq<T> takeWhile(Predicate<T> predicate) {
        return c -> consumeTillStop(t -> {
            if (predicate.test(t)) {
                c.accept(t);
            } else {
                stop();
            }
        });
    }

    default Seq<T> takeWhileEquals() {
        return takeWhile(t -> t, Objects::equals);
    }

    default <E> Seq<T> takeWhileEquals(Function<T, E> function) {
        return takeWhile(function, Objects::equals);
    }

    default Seq<T> timeLimit(long millis) {
        return millis <= 0 ? this : c -> {
            long end = System.currentTimeMillis() + millis;
            consumeTillStop(t -> {
                if (System.currentTimeMillis() > end) {
                    stop();
                }
                c.accept(t);
            });
        };
    }

    default T[] toObjArray(IntFunction<T[]> initializer) {
        SizedSeq<T> ts = toList();
        T[] a = initializer.apply(ts.size());
        ts.consumeIndexed((i, t) -> a[i] = t);
        return a;
    }

    default int[] toIntArray(ToIntFunction<T> function) {
        SizedSeq<T> ts = toList();
        int[] a = new int[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsInt(t));
        return a;
    }

    default double[] toDoubleArray(ToDoubleFunction<T> function) {
        SizedSeq<T> ts = toList();
        double[] a = new double[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsDouble(t));
        return a;
    }

    default long[] toLongArray(ToLongFunction<T> function) {
        SizedSeq<T> ts = toList();
        long[] a = new long[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.applyAsLong(t));
        return a;
    }

    default boolean[] toBooleanArray(Predicate<T> function) {
        SizedSeq<T> ts = toList();
        boolean[] a = new boolean[ts.size()];
        ts.consumeIndexed((i, t) -> a[i] = function.test(t));
        return a;
    }

    default LinkedSeq<T> toLinked() {
        return reduce(new LinkedSeq<>(), LinkedSeq::add);
    }

    default ArraySeq<T> toList() {
        return reduce(new ArraySeq<>(sizeOrDefault()), ArraySeq::add);
    }

    default <E> void zip(Iterable<E> iterable, BiConsumer<T, E> consumer) {
        Iterator<E> iterator = iterable.iterator();
        consumeTillStop(t -> consumer.accept(t, ItrUtil.pop(iterator)));
    }

    interface IntObjToInt<T> {
        int apply(int acc, T t);
    }

    interface DoubleObjToDouble<T> {
        double apply(double acc, T t);
    }

    interface LongObjToLong<T> {
        long apply(long acc, T t);
    }

    interface BooleanObjToBoolean<T> {
        boolean apply(boolean acc, T t);
    }

    interface IndexObjConsumer<T> {
        void accept(int i, T t);
    }

    interface IndexObjFunction<T, E> {
        E apply(int i, T t);
    }

    interface IndexObjPredicate<T> {
        boolean test(int i, T t);
    }

    class Empty {
        static final Seq<Object> emptySeq = c -> {};
        static final Consumer<Object> nothing = t -> {};
    }
}