package com.spartango.infra.targeting.network;

import com.spartango.infra.core.OSMGraph;
import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
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

    public static final  double DISTANCE_SCALE = 1.0d;//(1 / 185000.0); // l of fuel per meter-ton from 436mpg
    public static final  double SLOPE_SCALE    = 0.5 * DISTANCE_SCALE; // Up to 80% more for terrain
    public static final  double DAMAGE_COST    = 2400000 * DISTANCE_SCALE; // 2,400,000m @ 100km/hr = 24 hours of delay
    private static final double MAX_SLOPE      = 0.02;

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
//        final long startId = Long.parseLong(start.getProperty(NeoNode.OSM_ID).toString());
//        final long endId = Long.parseLong(end.getProperty(NeoNode.OSM_ID).toString());
//
//        // Look up the nodes in MapDB because it's faster than Neo4J and all we need is lat/long
//        final NodeStub startNode = index.getNode(startId);
//        final NodeStub endNode = index.getNode(endId);
//
//        final double distance = ShapeUtils.calculateDistance(startNode, endNode);
//        double distanceCost = distanceCost(distance);

//        final Optional<Double> startElevation = getElevationWithIndex(startNode);
//        final Optional<Double> endElevation = getElevationWithIndex(endNode);
//
//        final double elevationChange = startElevation.isPresent() && endElevation.isPresent() ?
//                                       endElevation.get() - startElevation.get() :
//                                       0.0;
//        double elevationCost = elevationCost(distance, elevationChange);
        return 1;
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
        final Set<String> damagedIds = damaged.stream()
                                              .map(NodeStub::getId)
                                              .map(String::valueOf)
                                              .collect(Collectors.toSet());
        return graph.calculatePath(start, end, (r, d) -> totalCost(r, damagedIds), this::lengthEstimate);
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
    protected double totalCost(Relationship relationship, Set<String> damagedIds) {
        double distance = graph.linkLength(relationship);

        // Flat land travel cost
        double distancePrice = distanceCost(distance);

        // Elevation cost
        double elevationChange = graph.linkElevationChange(relationship, this);
        double elevationPrice = elevationCost(distance, elevationChange);

        // Cost incurred by waiting for repairs on the track
        double damagePrice = damageCost(relationship, damagedIds);

        return distancePrice + elevationPrice + damagePrice;
    }

    protected double elevationCost(double distance, double elevationChange) {
        // Treat uphill and downhill the same
        double slope = Math.abs(elevationChange / distance);
        double scaledSlope = Math.max(1.0, slope / MAX_SLOPE);

        // Penalty paid across the entire length of track
        return distance * scaledSlope * SLOPE_SCALE;
    }

    protected double distanceCost(double distance) {
        return distance * DISTANCE_SCALE;
    }

    protected double damageCost(Relationship relationship, Set<String> damagedIds) {
        if (!damagedIds.isEmpty()) {
            // TODO: Support multiple damaged legs properly
            // Just need to know the Identifiers to figure the damage
            final boolean damaged = damagedIds.contains(relationship.getStartNode()
                                                                    .getProperty(NeoNode.OSM_ID)
                                                                    .toString())
                                    && damagedIds.contains(relationship.getEndNode()
                                                                       .getProperty(NeoNode.OSM_ID)
                                                                       .toString());
            if (damaged) {
                return DAMAGE_COST;
            }
        }
        return 0;
    }
}
