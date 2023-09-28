package com.github.wolray.seq;

import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wolray
 */
public class SeqClassesTest {
    public static Graph graph(Map<Class<?>, ArraySeq<Class<?>>> map) {
        Map<Class<?>, Pair<Class<?>, Node>> nodeMap = new HashMap<>();
        map.forEach((cls, parents) ->
            nodeMap.computeIfAbsent(cls, k -> {
                Node nd = Factory.node(k.getSimpleName());
                if (!cls.isInterface()) {
                    nd = nd.with(Shape.BOX);
                }
                return new Pair<>(k, nd);
            }));
        Seq<LinkSource> linkSources = c -> nodeMap.forEach((name, pair) -> {
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
        SeqExpand<Class<?>> expand = cls -> Seq.of(cls.getInterfaces()).append(cls.getSuperclass());
        Map<Class<?>, ArraySeq<Class<?>>> map = expand
            .filter(c -> Seq0.class != c && Object.class != c
                && SeqCollection.class != c && SeqProxy.ProxyCollection.class != c)
            .terminate(cls -> cls.getName().startsWith("java"))
            .toDAG(Seq.of(ArraySeq.class, LinkedSeq.class, ConcurrentSeq.class, LinkedSeqSet.class,
                SeqProxy.ProxyList.class, SeqProxy.ProxySet.class, SeqProxy.ProxyQueue.class));
        Graph graph = graph(map);
        try {
            Graphviz.fromGraph(graph).render(Format.SVG).toFile(new File(String.format("src/test/resources/%s.svg", "seq-classes")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
