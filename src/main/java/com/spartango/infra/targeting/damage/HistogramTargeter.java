package com.spartango.infra.targeting.damage;

import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.network.RailFlow;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Author: spartango
 * Date: 2/13/16
 * Time: 11:00.
 */
public class HistogramTargeter {
    private final RailFlow                          baseFlow;
    private final Map<Set<NodeStub>, Set<NodeStub>> histogram;

    public HistogramTargeter(RailFlow baseFlow,
                             Map<Set<NodeStub>, Set<NodeStub>> histogram) {
        this.baseFlow = baseFlow;
        this.histogram = histogram;
    }

    public HistogramTargeter(RailFlow baseFlow) {
        this(baseFlow, baseFlow.histogramPaths());
    }

    public Stream<RailFlow> targetStream() {
        return histogram.entrySet()
                        .stream()
                        .sorted(ENTRY_COMPARATOR) // Sort by criticality
                        .map(entry -> baseFlow.damage(entry.getKey()));
    }

    public Stream<RailFlow> deltaStream() {
        return histogram.entrySet()
                        .stream()
                        .sorted(ENTRY_COMPARATOR) // Sort by criticality
                        .map(entry -> baseFlow.damageDelta(entry.getKey(), entry.getValue()));
    }

    private static final Comparator<Map.Entry<Set<NodeStub>, Set<NodeStub>>> ENTRY_COMPARATOR =
            Comparator.comparingInt((Map.Entry<Set<NodeStub>, Set<NodeStub>> entry) -> entry.getValue().size())
                      .reversed();
}
