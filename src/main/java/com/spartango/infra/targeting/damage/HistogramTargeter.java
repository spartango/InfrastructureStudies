package com.spartango.infra.targeting.damage;

import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.network.RailFlow;
import com.spartango.infra.utils.ShapeUtils;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
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
    private final long                              limit;

    public HistogramTargeter(RailFlow baseFlow,
                             Map<Set<NodeStub>, Set<NodeStub>> histogram,
                             long limit) {
        this.baseFlow = baseFlow;
        this.histogram = histogram;
        this.limit = limit;
    }

    public HistogramTargeter(RailFlow baseFlow, long limit) {
        this(baseFlow, baseFlow.histogramPaths(), limit);
    }

    public Stream<RailFlow> targetStream() {
        return sortedStream().map(entry -> baseFlow.damage(entry.getKey()));
    }

    public Stream<RailFlow> deltaStream() {
        return sortedStream() // Sorted bridge : affected sinks
                .map(entry -> baseFlow.damageDelta(entry.getKey(), entry.getValue())); // Damage this bridge, affecting sinks only
    }

    private Stream<Map.Entry<Set<NodeStub>, Set<NodeStub>>> sortedStream() {
        Comparator<Map.Entry<Set<NodeStub>, Set<NodeStub>>> ELEVATION_COMPARATOR =
                Comparator.comparingDouble((Map.Entry<Set<NodeStub>, Set<NodeStub>> entry)
                                                   -> entry.getKey() // The bridge pair
                                                           .stream()
                                                           .map(baseFlow.getRailNetwork()::getElevation)
                                                           .filter(Optional::isPresent)
                                                           .mapToDouble(Optional::get)
                                                           .average()
                                                           .orElse(0.0))
                          .reversed(); // Highest first

        return histogram.entrySet()
                        .stream()
                        .sorted(HISTOGRAM_COMPARATOR.thenComparing(ELEVATION_COMPARATOR)
                                                    .thenComparing(LENGTH_COMPARATOR))
                        .limit(limit);
    }

    protected static final Comparator<Map.Entry<Set<NodeStub>, Set<NodeStub>>> LENGTH_COMPARATOR =
            Comparator.comparingDouble(
                    (Map.Entry<Set<NodeStub>, Set<NodeStub>> entry) -> ShapeUtils.calculateLength(entry.getKey()))
                      .reversed(); // Longest first

    protected static final Comparator<Map.Entry<Set<NodeStub>, Set<NodeStub>>> HISTOGRAM_COMPARATOR =
            Comparator.comparingInt(
                    (Map.Entry<Set<NodeStub>, Set<NodeStub>> entry) -> entry.getValue().size())
                      .reversed(); // Most shared first
}
