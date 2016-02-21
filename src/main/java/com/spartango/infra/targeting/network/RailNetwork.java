package com.spartango.infra.targeting.network;

import com.spartango.infra.core.OSMGraph;
import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
import com.spartango.infra.utils.ShapeUtils;
import org.mapdb.DB;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/12/16
 * Time: 15:46.
 */
public class RailNetwork {

    public static final  double DISTANCE_SCALE = (1 / 185000.0); // l of fuel per meter-ton from 436mpg
    public static final  double SLOPE_SCALE    = 0.8 * DISTANCE_SCALE; // Up to 80% more for terrain
    public static final  double DAMAGE_COST    = 2400000 * DISTANCE_SCALE; // 2,400,000m @ 100km/hr = 24 hours of delay
    private static final double MAX_SLOPE      = 2.0;

    // Indices
    private final OSMGraph graph;
    private final OSMIndex index;

    // Used as a cache
    private final DB database;

    public RailNetwork(OSMGraph graph, OSMIndex index, DB database) {
        this.graph = graph;
        this.index = index;
        this.database = database;
    }

    public Optional<NodeStub> getNode(long id) {
        if (!getIndex().hasNode(id)) {
            return Optional.empty();
        }
        return Optional.of(getIndex().getNode(id));
    }

    public double lengthEstimate(Node start, Node end) {
        // Get node IDs
        final long startId = Long.parseLong(start.getProperty(NeoNode.OSM_ID).toString());
        final long endId = Long.parseLong(end.getProperty(NeoNode.OSM_ID).toString());

        // Look up the nodes in MapDB because it's faster than Neo4J and all we need is lat/long
        final NodeStub startNode = index.getNode(startId);
        final NodeStub endNode = index.getNode(endId);

        return ShapeUtils.calculateDistance(startNode, endNode);
    }

    public NeoNode getGraphNode(Node node) {
        return graph.getNode(node);
    }

    public DB getDatabase() {
        return database;
    }

    public OSMGraph getGraph() {
        return graph;
    }

    public OSMIndex getIndex() {
        return index;
    }

    public Transaction beginGraphTx() {
        return this.graph.getGraphDb().beginTx();
    }

    public Optional<NeoNode> getGraphNode(long id) {
        return graph.getNode(id);
    }

    public Optional<NeoNode> getGraphNode(NodeStub nodeStub) {
        return graph.getNode(nodeStub.getId());
    }

    public Collection<NodeStub> getStations() {
        final Set<NodeStub> stations = database.getHashSet("stations");
        if (stations.isEmpty()) {
            System.out.println("Building station cache");
            index.streamNodesForTag("railway", "station").forEach(stations::add);
            database.commit();
        }
        return stations;
    }

    public Collection<WayStub> getBridges() {
        final Set<WayStub> bridges = database.getHashSet("bridges");
        if (bridges.isEmpty()) {
            System.out.println("Building bridge cache");
            index.streamWaysForTag("bridge").forEach(bridges::add);
            database.commit();
        }
        return bridges;
    }

    public WeightedPath calculatePath(NeoNode start, NeoNode end, Set<NodeStub> damaged) {
        return graph.calculatePath(start, end, (r, d) -> totalCost(r, damaged), this::lengthEstimate);
    }

    public Optional<Double> getElevation(NeoNode node) {
        return getElevation(node.getId());
    }

    public Optional<Double> getElevation(NodeStub node) {
        return getElevation(node.getId());
    }

    public Optional<Double> getElevation(long id) {
        return getNode(id).flatMap(this::getElevationWithIndex);
    }

    protected Optional<Double> getElevationWithIndex(NodeStub node) {
        final String ele = node.getTag("ele");
        if (ele != null) {
            try {
                return Optional.of(Double.parseDouble(ele));
            } catch (NumberFormatException e) {

            }
        }
        return Optional.empty();
    }

    // Cost functions
    protected double totalCost(Relationship relationship, Set<NodeStub> damagedNodes) {
        double distance = graph.linkLength(relationship);

        // Flat land travel cost
        double distancePrice = distanceCost(distance, relationship);

        // Elevation cost
        double elevationPrice = elevationCost(distance, relationship);

        // Cost incurred by waiting for repairs on the track
        double damagePrice = damageCost(relationship, damagedNodes);

        return distancePrice + elevationPrice + damagePrice;
    }

    protected double elevationCost(double distance, Relationship relationship) {
        // Fetch the elevations at the ends
        try (Transaction tx = beginGraphTx()) {
            final NeoNode start = getGraphNode(relationship.getStartNode());
            final NeoNode end = getGraphNode(relationship.getStartNode());

            // TODO: Fix getting elevations from the index
            final Optional<Double> startEle = getElevation(start);
            final Optional<Double> endEle = getElevation(end);

            if (startEle.isPresent() && endEle.isPresent()) {
                double slope = Math.abs((endEle.get() - startEle.get()) / distance); // Ruling grade, uphill & downhill are the same
                double scaledSlope = Math.max(1.0, slope / MAX_SLOPE);
                return distance * scaledSlope * SLOPE_SCALE;
            }

            tx.success();
        }
        return 0;
    }

    protected double distanceCost(double distance, Relationship relationship) {
        return distance * DISTANCE_SCALE;
    }

    protected double damageCost(Relationship relationship, Set<NodeStub> damagedNodes) {
        final Set<String> damagedIds = damagedNodes.stream()
                                                   .map(NodeStub::getId)
                                                   .map(String::valueOf)
                                                   .collect(Collectors.toSet());

        if (!damagedNodes.isEmpty()) {
            // Just need to know the Identifiers
            final String startId = relationship.getStartNode().getProperty(NeoNode.OSM_ID).toString();
            final String endId = relationship.getEndNode().getProperty(NeoNode.OSM_ID).toString();

            // TODO: Support multiple damaged legs properly
            // Check for damage
            final boolean damaged = damagedIds.contains(startId) && damagedIds.contains(endId);
            if (damaged) {
                return DAMAGE_COST;
            }
        }
        return 0;
    }
}
