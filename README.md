# seq
本项目提供一个强大而完备的流式编程API，并独创性的为Java添加类似生成器的编程机制。

网页端参考：[阿里云社区](https://developer.aliyun.com/article/1191351?spm=5176.28261954.J_7341193060.5.44812fdeTRXvK5&scm=20140722.S_community@@%E6%96%87%E7%AB%A0@@1191351._.ID_1191351-RL_%E4%B8%80%E7%A7%8D%E6%96%B0%E7%9A%84%E6%B5%81%E4%B8%BA%20java%20%E5%8A%A0%E5%85%A5%E7%94%9F%E6%88%90%E5%99%A8generator%E7%89%B9%E6%80%A7-LOC_m~UND~search~UND~community~UND~i-OR_ser-V_3-P0_1)

手机端参考：[阿里开发者微信公众号](https://mp.weixin.qq.com/s/v-HMKBWxtz1iakxFL09PDw)

这一机制的设计思想与核心代码在上述公开文章中都已阐述完整，本项目是在此基础上的完全重写，并得到了原作者的鼓励与支持，未来发展也将主要面向开源社区需求。

#### 引用方式
项目已发布到Central Maven，直接引用即可
```xml
<dependency>
    <groupId>io.github.wolray</groupId>
    <artifactId>seq</artifactId>
    <version>1.0.1</version>
</dependency>
```

## 流的定义与种类
### 普通流（一元流）
```Java
public interface Seq<T> {
    void consume(Consumer<T> consumer);
}
```
### 可迭代流ItrSeq
它既是`Iterable`，也是`Seq`，并提供一个额外的默认`zip`方法
```Java
public interface ItrSeq<T> extends Iterable<T>, Seq<T> {
    // (1, 2, 3, 4) -> zip(0) -> (1, 0, 2, 0, 3, 0, 4, 0)
    default ItrSeq<T> zip(T t) {...}
}
```
### 可数流SizedSeq
它直接继承自`ItrSeq`，同时提供`size`和`isEmpty`方法，以及默认的`isNotEmpty`方法
```Java
public interface SizedSeq<T> extends ItrSeq<T> {
    // (1, 2, 3, 4) -> size() -> 4
    int size();
    // (1, 2, 3, 4) -> isEmpty() -> false
    boolean isEmpty();
    // (1, 2, 3, 4) -> isNotEmpty() -> true
    default boolean isNotEmpty() {...}
}
```
### ArrayList流ArraySeq
它直接继承自`ArrayList`，自带它的全部方法和功能，同时也是个`SizedSeq`，该Class是对`ArrayList`的强大扩展，还可以在此基础上追加一些额外方法，例如`swap`
```Java
public class ArraySeq<T> extends ArrayList<T> implements SizedSeq<T> {
    // (1, 2, 3, 4) -> swap(1, 2) -> (1, 3, 2, 4)
    public void swap(int i, int j) {...}
}
```
### LinkedList流LinkedSeq
它直接继承自`LinkedList`，自带它的全部方法和功能，同时也是个`SizedSeq`
```Java
public class LinkedSeq<T> extends LinkedList<T> implements SizedSeq<T> {...}
```
## 流的创建
### 基于生成器表达式
```Java
// {code block} -> (0, 1, 2, 3, 4, 5, ...)
Seq<Integer> seq = c -> {
    c.accept(0);
    int i = 1;
    for (; i < 4; i++) {
        c.accept(i);
    }
    // 这是一个无限流
    while (true) {
        c.accept(i++);
    }
};
```

### 单位流
```Java
// unit(1) -> (1)
static <T> Seq<T> unit(T t) {...}
```

### 基于Iterable
```Java
// of([1, 2, 3, 4]) -> (1, 2, 3, 4)
static <T> Seq<T> of(Iterable<T> iterable) {...}
```

### 基于可变参数
```Java
// of(1, 2, 3, 4) -> (1, 2, 3, 4)
static <T> Seq<T> of(T... ts) {...}
```

### 基于Supplier
```Java
// gen(() -> 1) -> (1, 1, 1, ...)
static <T> ItrSeq<T> gen(Supplier<T> supplier) {...}
// gen(() -> 1 or null) -> (1, 1, 1)
static <T> ItrSeq<T> tillNull(Supplier<T> supplier) {...}
```

### 基于Optional
```Java
// of(Optional.of(1)) -> (1)
static <T> Seq<T> of(Optional<T> optional) {...}
```

#### 基于正则匹配
```Java
// match("abc123foo456bar789", Pattern.compile("[0-9]+")) -> ("123", "456", "789")
// match("abc123foo456bar789", Pattern.compile("[a-z]+")) -> ("abc", "foo", "bar")
static ItrSeq<Matcher> match(String s, Pattern pattern) {...}
```
也可以指定正则匹配的分组
```Java
// match("abc123foo456bar789", Pattern.compile("([0-9]+)[0-9]"), 1) -> ("12", "45", "78")
static ItrSeq<Matcher> match(String s, Pattern pattern, int group) {...}
```

### 基于树
按照深度优先的顺序依次遍历节点
```Java
static <N> Seq<N> ofTree(N node, Function<N, Seq<N>> sub) {...}
// 对分支深度做限制，可用于树搜索算法
static <N> Seq<N> ofTree(int maxDepth, N node, Function<N, Seq<N>> sub) {...}
```

### 基于JSONObject
按照深度优先的顺序依次遍历内部成员以及成员的成员
```Java
static Seq<Object> ofJson(Object node) {...}
```

### 基于元素重复
```Java
// repeat(4, "x") -> ("x", "x", "x", "x")
static <T> ItrSeq<T> repeat(int n, T t) {...}
```

## 流的链式调用
链式调用是流的核心功能，它由一个流触发，返回一个新的流，中间可对数据进行映射、过滤、排序等等各种操作

### map 流的映射
```Java
// (1, 2, 3, 4) -> map(i -> i + 10) -> (11, 12, 13, 14)
default <E> Seq<E> map(Function<T, E> function) {...}

// 带下标映射
// (1, 2, 3, 4) -> mapIndexed((index, i) -> index % 2 == 0 ? i : i + 10) -> (11, 2, 13, 4)
default <E> Seq<E> mapIndexed(IndexObjFunction<T, E> function) {...}
```
其他映射方式
```Java
// 分段映射，前n项使用substitute，其他使用function
// (1, 2, 3, 4) -> map(i -> i + 10, 2, i -> i + 100) -> (101, 102, 13, 14)
default <E> Seq<E> map(Function<T, E> function, int n, Function<T, E> substitute) {...}

// 仅对非空元素映射
// (1, 2, null, 4) -> mapMaybe(i -> i + 10) -> (10, 20, 40)
default <E> Seq<E> mapMaybe(Function<T, E> function) {...}

// 映射后过滤非空元素
// (1, 2, 3, 4) -> mapNotNull(i -> i % 2 == 0 ? null : i + 10) -> (10, 30)
// 等价于 map(function).filterNotNull()
default <E> Seq<E> mapNotNull(Function<T, E> function) {...}
```

### 元素过滤 filter
```Java
// (1, 2, 3, 4) -> filter(i -> i % 2 == 0) -> (2, 4)
default Seq<T> filter(Predicate<T> predicate) {...}

// 以及只在前n项里过滤
// (1, 2, 3, 4) -> filter(2, i -> i % 2 == 0) -> (2, 3, 4)
default Seq<T> filter(int n, Predicate<T> predicate) {...}

// 带下标过滤
// (1, 2, 3, 4) -> filterIndexed((index, i) -> index % 2 == 0) -> (1, 3)
default Seq<T> filterIndexed(IndexObjPredicate<T> predicate) {...}
```

其他过滤方法
```Java
// 取collection交集
// (1, 2, 3, 4) -> filterIn([3, 4, 5]) -> (3, 4)
default Seq<T> filterIn(Collection<T> collection) {...}

// 取map交集
// (1, 2, 3, 4) -> filterIn({3: "x", 4: "y", 5: "z"}) -> (3, 4)
default Seq<T> filterIn(Map<T, ?> map) {...}

// 按类型过滤
// (dog, cat, rock, water) -> filterInstance(Animal.class) -> (dog, cat)
default <E> Seq<E> filterInstance(Class<E> cls) {...}

// 按条件否过滤
default Seq<T> filterNot(Predicate<T> predicate) {...}
default Seq<T> filterNotIn(Collection<T> collection) {...}
default Seq<T> filterNotIn(Map<T, ?> map) {...}

// 非空过滤
// (1, 2, null, 4) -> filterNotNull() -> (1, 2, 4)
default Seq<T> filterNotNull() {...}
```

### 展平流 flatMap
可以认为`Seq`也是一种`Monad`，`flatMap`即是它的`bind`方法
```Java
// (1, 2, 3, 4) -> flatMap(i -> repeat(2, i)) -> (1, 1, 2, 2, 3, 3, 4, 4)
default <E> Seq<E> flatMap(Function<T, Seq<E>> function) {...}
```

### 按Optional展平 flatOptional
```Java
// (1, 2, 3, 4) -> flatOptional(i -> Optional.of(i + 10)) -> (11, 12, 13, 14)
default <E> Seq<E> flatOptional(Function<T, Optional<E>> function) {...}
```

### 处理元素 onEach
处理但不消费
```Java
// (1, 2, 3, 4) -> onEach(i -> print(i)) -> (1, 2, 3, 4) while print each
default Seq<T> onEach(Consumer<T> consumer) {...}

// 只处理前n项
// (1, 2, 3, 4) -> onEach(2, i -> print(i)) -> (1, 2, 3, 4) while print 1, 2
default Seq<T> onEach(int n, Consumer<T> consumer) {...}

// 带下标peek
// (1, 2, 3, 4) -> onEachIndexed((index, i) -> if (index % 2 == 0) print(i)) -> (1, 2, 3, 4) while print 2, 4
default Seq<T> onEachIndexed(IndexObjConsumer<T> consumer) {...}
```

### 提取前n项元素 take
```Java
default Seq<T> take(int n) {...}
```

### 流的部分消费 partial
只按照指定方式消费前n项，后面元素保留
```Java
// (1, 2, 3, 4) -> partial(2, i -> print(i)) -> (3, 4) while print 1, 2
default Seq<T> partial(int n, Consumer<T> substitute) {...}
```

### 翻转流 reverse
```Java
// (1, 2, 3, 4) -> reverse() -> (4, 3, 2, 1)
default ArraySeq<T> reverse() {...}
```

### 累加流 runningFold
```Java
// (1, 2, 3, 4) -> runningFold(0, (i, j) -> i + j) -> (1, 3, 6, 10)
default <E> Seq<E> runningFold(E init, BiFunction<E, T, E> function) {...}
```

### 去重 distinct
```Java
// (1, 1, 2, 2, 3) -> distinct() -> (1, 2, 3)
default Seq<T> distinct() {...}
// 也可以指定值函数去重
// (1, 1, 2, 2, 3) -> distinctBy(i -> i / 2) -> (1, 3)
default <E> Seq<T> distinctBy(Function<T, E> function) {...}
```

## 流的窗口函数
所谓窗口函数就是对流的元素按照某种规则进行局部聚合，每一个小组聚合为整体后，构成一个新的流。
聚合的逻辑通常有三种，按次数，按时间，按头尾元素特征。

### 每n个元素分为一组 chunked
```Java
// (1, 2, 3, 4, 5, 6, 7, 8) -> chunked(3) -> ([1, 2, 3], [4, 5, 6], [7, 8])
default Seq<ArraySeq<T>> chunked(int size) {...}
```

### 按条件局部分组 mapSub
```Java
// 连续满足条件，分为一组，不满足即中断
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> mapSub(isEven) -> ([2, 2, 2], [4, 4])
default Seq<ArraySeq<T>> mapSub(Predicate<T> takeWhile) {...}

// 指定分组的聚合方式为toSet（默认为toList）
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> mapSub(isEven, toSet) -> ({2}, {4})
default <V> Seq<V> mapSub(Predicate<T> takeWhile, Reducer<T, V> reducer) {...}

// 指定分组的开始条件与结束条件，如下示例为奇数开启，偶数结束
// (1, 1, 2, 2, 2, 3, 4, 4, 5) -> mapSub(isOdd, isEven, toList) -> ([1, 1, 2], [3, 4])
default <V> Seq<V> mapSub(Predicate<T> first, Predicate<T> last, Reducer<T, V> reducer) {...}
```

### 按数量滑动开窗 windowed
```Java
// (1, 2, 3, 4) -> windowed(3, 1, true)  -> ([1, 2, 3], [2, 3, 4], [3, 4], [4])
// (1, 2, 3, 4) -> windowed(3, 1, false) -> ([1, 2, 3], [2, 3, 4])
// (1, 2, 3, 4) -> windowed(3, 2, true)  -> ([1, 2, 3], [3, 4])
// (1, 2, 3, 4) -> windowed(3, 2, false) -> ([1, 2, 3])
default Seq<ArraySeq<T>> windowed(int size, int step, boolean allowPartial) {...}
```

### 按时间开窗 windowedByTime
需要热流和异步流发布后才能完全发挥价值，效果与`windowed`类似，只不过将数量窗替换为了时间窗

## 流的聚合之一 Reducer
`Reducer`是用于流聚合的专用接口，它有三个方法，`supplier`提供用于聚合的容器或者累加器，`accumulator`用于对聚合容器/累加器同每一个元素依次添加/累加，`finisher`用于对完成操作后的聚合容器/累加器进行后处理

```Java
public interface Reducer<T, V> {
    Supplier<V> supplier();
    BiConsumer<V, T> accumulator();
    Consumer<V> finisher();
}
```

`Reducer`这个接口类似于`java.util.stream.Collector`的设计，但是更为简洁，更容易让人类理解。举个例子，将流聚合为一个`List`，可以先用`supplier`初始化一个`ArrayList`，而后用`accumulator`提供的`List.add`方法依次添加元素，`finisher`允许为空。以此即可实现流的`toList`方法。

有了`Reducer`之后，所有的聚合操作都有了标准的、可递归的统一接口，所谓递归，是指由于`Reducer`本身的纯函数特性，它是完全支持多重嵌套的聚合操作，后文会有具体介绍。

```Java
default <E> E reduce(E des, BiConsumer<E, T> accumulator) {...}
default <E> E reduce(Reducer<T, E> reducer) {...}
```

### 构造Reducer
```Java
static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator) {...}
// finisher可有可无
static <T, V> Reducer<T, V> of(Supplier<V> supplier, BiConsumer<V, T> accumulator, Consumer<V> finisher) {...}
```

### 内置常见Reducer

#### 聚合为List: toList
```Java
// Reducer
static <T> Reducer<T, ArraySeq<T>> toList() {...}
// Seq
default ArraySeq<T> toList() {...}
```

#### 聚合为Set: toSet
```Java
// Reducer
static <T> Reducer<T, SeqSet<T>> toSet() {...}
// Seq
default SeqSet<T> toSet() {...}
```
这里的`SeqSet`与`ArraySeq`类似，同时继承了`Seq`与`Set`两个接口

#### 聚合为ConcurrentLinkedQueue: toConcurrent
```Java
// Reducer
static <T> Reducer<T, ConcurrentSeq<T>> toConcurrent() {...}
// Seq
default ConcurrentSeq<T> toConcurrent() {...
```

#### 聚合为LinkedList: toLinked
```Java
// Reducer
static <T> Reducer<T, LinkedSeq<T>> toLinked() {...}
// Seq
default LinkedSeq<T> toLinked() {...}
```

#### 聚合为其他Collection: collect
```Java
// Reducer
static <T, C extends Collection<T>> Reducer<T, C> collect(Supplier<C> des) {...}
// Seq
default <C extends Collection<T>> C collectBy(IntFunction<C> constructor) {...}
```

## 流的聚合之二 Transducer
`Reducer`在流的聚合场景是不完备的，很多时候，聚合完成后还需要进行一次转化。以`join`为例，要将(1, 2, 3, 4)这样一个流以加号(+)为分隔符，聚合为`String`，使用`reduce`的思路，可以这么实现
```Java
Seq<Integer> seq = Seq.of(1, 2, 3, 4);
StringJoiner joiner = seq.reduce(new StringJoiner("+"), (j, i) -> j.add(i.toString()));
String result = joiner.toString();
```
可以看到，`reduce`之后的结果是一个`StringJoiner`，并不是我们最终需要的`String`。那怎么把这个过程抽象为一个标准函数式接口呢？答案是：引入`Transducer`，即`Reducer`+`Transformer`
```Java
public interface Transducer<T, V, E> {
    Reducer<T, V> reducer();
    Function<V, E> transformer();
}
```
它只比`Reducer`多一个接口，可以将`reduce`之后的结果`V`，进一步映射为一个`E`。其构造方式如下：
```Java
static <T, V, E> Transducer<T, V, E> of(Supplier<V> supplier, BiConsumer<V, T> accumulator, Function<V, E> transformer) {...}
static <T, V, E> Transducer<T, V, E> of(Reducer<T, V> reducer, Function<V, E> transformer) {...}
```
值得一提的是，熟悉`Collector`的朋友可能会注意到它和、`Transducer`的相似，事实上`Collector`就完全可以看作是`Transducer`的原型参考，它的缺陷是强制三个泛型参数，在不需要事后转换的情况下，也无法退化为只有两个泛型参数的接口，所以认知成本很高。
当然，`Collector`也可以无缝转化为`Transducer`：
```Java
static <T, V, E> Transducer<T, V, E> of(Collector<T, V, E> collector) {
    return of(Reducer.of(collector.supplier(), collector.accumulator()), collector.finisher());
}
```
有了`Transducer`，就可以对`join`操作进行标准化实现
```Java
// (1, 2, 3, 4) -> join("+", i -> i.toString()) -> "1+2+3+4"
static <T> Transducer<T, StringJoiner, String> join(String sep, Function<T, String> function) {
    return Transducer.of(() -> new StringJoiner(sep), (j, t) -> j.add(function.apply(t)), StringJoiner::toString);
}
```

### 内置聚合函数
#### 计数
```Java
// Reducer
// (1, 2, 3, 4) -> count() -> 4
static <T> Transducer<T, int[], Integer> count() {...}
// 按照条件计数
// (1, 2, 3, 4) -> count(isEvent) -> 2
static <T> Transducer<T, int[], Integer> count(Predicate<T> predicate) {...}
// Seq
default int count() {...}
```

#### 求和
```Java
// Reducer
// 求和为Double
// (1, 2, 3, 4) -> sum(i -> i) -> 10.0
static <T> Transducer<T, double[], Double> sum(ToDoubleFunction<T> function) {...}
// 求和为Int
// (1, 2, 3, 4) -> sum(i -> i) -> 10
static <T> Transducer<T, int[], Integer> sumInt(ToIntFunction<T> function) {...}
// 求和为Long
// (1, 2, 3, 4) -> sum(i -> i) -> 10L
static <T> Transducer<T, long[], Long> sumLong(ToLongFunction<T> function) {...}
// Seq 提供对应默认方法
```

### 求平均与加权平均
```Java
// (1, 2, 3, 4) -> average(i -> i) -> 2.5
static <T> Transducer<T, double[], Double> average(ToDoubleFunction<T> function) {...}
```

## 流的分组
todo

## 其他特性

### 惰性有向图与并发递归计算
见文章：[面向上下文(Context)编程：一种带缓存、可并发、惰性递归的编程范式，源码可画图](https://mp.weixin.qq.com/s/lxpoXcH7fGF1_n8iI96d4A)

本项目提供了上文的一种实现。

### 流的缓存
当需要对流进行缓存或者collect时，传统做法是使用`ArrayList`或者`LinkedList`。然而前者需要不时拷贝内部数组，后者每个元素内存成本高，更好的方式是将二者结合，使用扩容后的新数组存放新元素，但不进行数组拷贝，而是直接将新旧数组链接起来。从而既缓存了元素，也实现了内存和性能的双优化。

该实现称之为`BatchedSeq`，已发布，并将应用于各种需要对流进行缓存的场景，包括暂未发布的并发流。


## todo特性
由于内容过多，仍有许多功能尚未实现，暂列于后文。

#### 更好的groupby

#### 二元流/多元流
基于callback机制的二元流，是其独特的衍生特性。二元流最大的优势是不产生任何tuple或pair之类的中间数据结构，即用即走，十分优雅。
Java里的`List`/`Set`/`Iterable`都可以在一元流的基础上实现升级改造，二元流则自然对应`Map`，可以衍生许多有趣玩法。

#### Splitter
Java的`String`提供了默认的`split`实现，但它会将substring收集为一个数组，在关心性能或后续应用的场景下殊为不妥。Google的guava库提供了产出`Iterable`的`Splitter`，十分优秀。

而基于callback机制，我们能实现出更好更快的`Splitter`，相对guava不仅能有20%-40%的性能提升，还可支持任意后续的流式操作。

#### 统一InputStream与数据源
`InputStream`是Java里进行IO交互时一个非常底层且重要的接口，而对于许多常用文件格式，例如CSV/properties，天然一行代表一个数据块。所以`InputStream`和Seq of String是直接对应的。

在另一方面，`InputStream`是一次性的，本身不可重用，从函数式编程的角度来讲，它不算是一种好的数据。所以本项目会对其进行封装，抽象出一个中间数据结构`ISSouce`(意为InputStream Source)。它将对`File`, `Path`, local file, resouce, `URL`以及literal string等常见数据源进行统一收口，实现可重用且IO隔离的安全流式数据源，并提供与`Seq`的互转。

#### 异步流/并发流/异步通道

#### 热流与订阅

## 反馈
可使用中文直接提issue，也可添加微信radiumlei2010进群，原作者也在群里，方便他与大家沟通，收集反馈。

## 发布记录
#### 1.0.1 (20230922)
新增`IntSeq`, `Lazy`, `BatchedSeq`, `SeqSet`, `windowed`, `timeWindowed`
