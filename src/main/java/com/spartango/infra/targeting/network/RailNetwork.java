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

    public Optional<NeoNode> getGraphNode(long id) {
        return graph.getNode(id);
    }

    public Collection<NodeStub> getStations() {
        System.out.println("Finding stations...");
        final Set<NodeStub> stations = database.getHashSet("stations");
        if (stations.isEmpty()) {
            System.out.println("Building station cache");
            index.streamNodesForTag("railway", "station").forEach(stations::add);
            database.commit();
        }
        return stations;
    }

    public Collection<WayStub> getBridges() {
        System.out.println("Finding bridges...");
        final Set<WayStub> bridges = database.getHashSet("bridges");
        if (bridges.isEmpty()) {
            System.out.println("Building bridge cache");
            index.streamWaysForTag("bridge").forEach(bridges::add);
            database.commit();
        }
        return bridges;
    }

    public WeightedPath calculatePath(NeoNode start, NeoNode end, Set<NodeStub> damaged, double damageCost) {
        return graph.calculatePath(start, end, (r, d) -> damageCost(r, damaged, damageCost), this::lengthEstimate);
    }


    private double damageCost(Relationship relationship, Set<NodeStub> damagedNodes, double damageCost) {
        boolean damaged = false;
        final Set<String> damagedIds = damagedNodes.stream()
                                                   .map(NodeStub::getId)
                                                   .map(String::valueOf)
                                                   .collect(Collectors.toSet());

        if (!damagedNodes.isEmpty()) {
            // Just need to know the Identifiers
            final String startId = relationship.getStartNode().getProperty(NeoNode.OSM_ID).toString();
            final String endId = relationship.getEndNode().getProperty(NeoNode.OSM_ID).toString();

            // Check for damage
            damaged = damagedIds.contains(startId) && damagedIds.contains(endId);
            // TODO: Support multiple damaged legs properly
        }

        double length = graph.linkLength(relationship);

        if (damaged) {
            length += damageCost;
        }
        return length;
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
}
