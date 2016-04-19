package com.spartango.infra.targeting.network;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Transaction;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/13/16
 * Time: 10:00.
 */
public class RailFlow {
    private static final Double DEFAULT_FLOW = 1.0;

    protected final Collection<NeoNode> sources;
    protected final Collection<NeoNode> sinks;
    protected final Set<NodeStub>       damagedNodes;

    // Order matters
    protected final Map<List<NeoNode>, Double> flowRates;

    protected final RailNetwork                       railNetwork;
    protected       Map<NodeStub, List<WeightedPath>> paths;
    protected       double                            totalCost;

    public RailFlow(RailNetwork railNetwork,
                    Collection<NeoNode> sources,
                    Collection<NeoNode> sinks) {
        this(railNetwork, sources, sinks, Collections.EMPTY_SET);
    }

    public RailFlow(RailNetwork railNetwork,
                    Collection<NeoNode> sources,
                    Collection<NeoNode> sinks,
                    Set<NodeStub> damagedNodes) {
        this.railNetwork = railNetwork;
        this.sinks = sinks;
        this.sources = sources;
        this.damagedNodes = damagedNodes;

        // Initialize all flows
        flowRates = new HashMap<>();
        sinks.forEach(sink -> sources.forEach(source -> flowRates.put(Arrays.asList(source, sink), DEFAULT_FLOW)));

        this.paths = null;
        this.totalCost = -1;
    }

    public void setFlowRate(NeoNode source, NeoNode sink, double flow) {
        flowRates.put(Arrays.asList(source, sink), flow);
    }

    protected Map<NodeStub, List<WeightedPath>> calculatePaths() {
        return sources.stream()
                      .peek(o -> System.out.println("Source: " + o.getOsmNode()))
                      .collect(Collectors.toMap(NeoNode::getOsmNode,
                                                this::calculatePathsFrom));
    }

    protected List<WeightedPath> calculatePathsFrom(NeoNode station) {
        return sinks.parallelStream()
                    .filter(destination -> !destination.getOsmNode()
                                                       .equals(station.getOsmNode()))
                    .map(neoDestination -> railNetwork.calculatePath(station,
                                                                     neoDestination,
                                                                     damagedNodes))
                    .filter(path -> path != null && path.length() != 0)
                    .collect(Collectors.toList());
    }

    protected double calculateCost() {
        return getPaths().values()
                         .stream()
                         .flatMap(List::stream)
                         .mapToDouble(WeightedPath::weight)
                         .sum();
    }

    public double calculateCost(Collection<NeoNode> targetSources) {
        return targetSources.stream()
                            .map(NeoNode::getOsmNode)
                            .filter(getPaths()::containsKey)
                            .map(getPaths()::get)
                            .flatMap(List::stream)
                            .mapToDouble(WeightedPath::weight)
                            .sum();
    }

    public Map<Set<NodeStub>, Set<NodeStub>> histogramPaths() {
        return histogramPaths((x) -> true);
    }

    public Map<Set<NodeStub>, Set<NodeStub>> histogramPaths(Collection<WayStub> allowedWays) {
        // Build up a structure that makes it easy to check for the ways
        final Set<Long> allowedNodeIds = allowedWays.stream()
                                                    .flatMap(bridge -> bridge.getNodeIds().stream())
                                                    .collect(Collectors.toSet());

        return histogramPaths(pair -> pair.stream() // Check if both ends of the way are allowed
                                          .map(NodeStub::getId)
                                          .allMatch(allowedNodeIds::contains));
    }

    public Map<Set<NodeStub>, Set<NodeStub>> histogramPaths(Predicate<Set<NodeStub>> filter) {
        // Histogram
        Map<Set<NodeStub>, Set<NodeStub>> histogram = new HashMap<>();

        getPaths().forEach((source, relPaths) -> relPaths.forEach(path -> {
            // For each path
            if (path == null) {
                return;
            }
            path.relationships().forEach(relationship -> {
                // Pull up the nodes & data
                try (Transaction tx = railNetwork.beginGraphTx()) {
                    final NeoNode startNode = railNetwork.getGraphNode(relationship.getStartNode());
                    final NeoNode endNode = railNetwork.getGraphNode(relationship.getEndNode());
                    tx.success();

                    final Set<NodeStub> pair = new HashSet<>(Arrays.asList(startNode.getOsmNode(),
                                                                           endNode.getOsmNode()));
                    // Check if this segment passes the filter
                    if (filter.test(pair)) {
                        // Check if we've seen this segment before
                        Set<NodeStub> set = histogram.get(pair);
                        if (set == null) {
                            set = new HashSet<>();
                            histogram.put(pair, set);
                        }
                        set.add(source);
                    }
                }
            });
        }));

        return histogram;
    }

    public Set<NodeStub> getDamagedNodes() {
        return damagedNodes;
    }

    public Map<NodeStub, List<WeightedPath>> getPaths() {
        if (paths == null) {
            paths = calculatePaths();
            totalCost = calculateCost();
        }
        return paths;
    }

    public Collection<NeoNode> getSinks() {
        return sinks;
    }

    public Collection<NeoNode> getSources() {
        return sources;
    }

    public double getTotalCost() {
        if (paths == null) {
            getPaths();
        }

        return totalCost;
    }

    public RailNetwork getRailNetwork() {
        return railNetwork;
    }

    public RailFlow damageDelta(Set<NodeStub> damagedNodes, Collection<NodeStub> affected) {
        final List<NeoNode> affectedNodes = affected.stream()
                                                    .map(node -> railNetwork.getGraphNode(node.getId()))
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get)
                                                    .collect(Collectors.toList());

        return new RailFlow(railNetwork, affectedNodes, sinks, damagedNodes);
    }

    public RailFlow damage(Set<NodeStub> damagedNodes) {
        return new RailFlow(railNetwork, sources, sinks, damagedNodes);
    }
}
