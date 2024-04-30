package com.github.wolray.zero.flow;

import com.github.wolray.zero.flow.pair.Pair;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import java.io.File;
import java.util.Map;
import org.junit.Test;

/**
 * @author wolray
 */
public class SeqClassesTest {

  public static final ExpandSeq<Class<?>> CLASS_EXPAND = cls -> ZeroFlow.of(cls.getInterfaces()).append(cls.getSuperclass());

  public static Graph graph(Map<Class<?>, ArrayListZeroFlow<Class<?>>> map) {

    Map<Class<?>, Pair<Class<?>, Node>> nodeMap = MapZeroFlow.of(map).mapByValue((cls, parents) -> {
      Node nd = Factory.node(cls.getSimpleName());
      if (!cls.isInterface()) {
        nd = nd.with(Shape.BOX);
      }
      return new Pair<>(cls, nd);
    });
    ZeroFlow<LinkSource> linkSources = c -> nodeMap.forEach((name, pair) -> {
      Node curr = pair.second;
      for (Class<?> parent : map.get(pair.first)) {
        c.accept(nodeMap.get(parent).second.link(curr));
      }
    });
    return Factory.graph("Classes").directed()
        .graphAttr().with(Rank.dir(Rank.RankDir.LEFT_TO_RIGHT))
        .nodeAttr().with(Font.name("Consolas"))
        .linkAttr().with("class", "link-class")
        .with(linkSources.toObjArray(LinkSource[]::new));
  }

  @Test
  public void testClasses() {

    ZeroFlow<Class<?>> ignore = ZeroFlow.of(BaseZeroFlow.class, Object.class);
    Map<Class<?>, ArrayListZeroFlow<Class<?>>> map = CLASS_EXPAND
        .filterNot(ignore.toSet()::contains)
        .terminate(cls -> cls.getName().startsWith("java"))
        .toDAG(ZeroFlow.of(
            ArrayListZeroFlow.class, LinkedListZeroFlow.class, ConcurrentQueueZeroFlow.class, LinkedHashSetZeroFlow.class,
            BatchedZeroFlow.class
        ));
    Graph graph = graph(map);
    IOChain.apply(
        String.format("src/test/resources/%s.svg", "seq-classes"),
        s -> Graphviz.fromGraph(graph).render(Format.SVG).toFile(new File(s))
    );
  }

  @Test
  public void testSeqMap() {

    ZeroFlow<Class<?>> ignore = ZeroFlow.of(BaseZeroFlow.class);
    Map<Class<?>, ArrayListZeroFlow<Class<?>>> map = CLASS_EXPAND
        .filterNot(ignore.toSet()::contains)
        .terminate(cls -> cls.getName().startsWith("java"))
        .toDAG(ZeroFlow.of(LinkedHashMapZeroFlow.class));
    Graph graph = graph(map);
    IOChain.apply(
        String.format("src/test/resources/%s.svg", "seq-map"),
        s -> Graphviz.fromGraph(graph).render(Format.SVG).toFile(new File(s))
    );
  }

}
