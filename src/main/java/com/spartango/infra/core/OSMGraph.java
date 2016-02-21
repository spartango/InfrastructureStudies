package com.spartango.infra.core;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.utils.ShapeUtils;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static org.neo4j.graphalgo.GraphAlgoFactory.aStar;
import static org.neo4j.graphdb.PathExpanders.allTypesAndDirections;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 17:24.
 */


public class OSMGraph {
    public enum RelTypes implements RelationshipType {
        NEARBY,
        RAIL_LINK,
        REACHABLE,
        POWER_LINK,
        WATERWAY_LINK,
        ROAD_LINK,
        AIR_LINK
    }


    private GraphDatabaseService graphDb;

    public OSMGraph(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public void build(OSMIndex index) {
        // Go through ways
        final AtomicLong count = new AtomicLong();
        final long startTime = System.currentTimeMillis();
        index.getWaysById()
             .values()
             .forEach(way -> {
                 // Extract nodes, one by one
                 final List<NodeStub> nodes = way.getNodes(index);
                 final long latestCount = count.incrementAndGet();
                 long time = System.currentTimeMillis();
                 System.out.print("Tracing way #"
                                  + way.getId()
                                  + " w/ "
                                  + nodes.size()
                                  + " | "
                                  + latestCount
                                  + " ways @ "
                                  + (1000.0 * latestCount / (time - startTime))
                                  + "/s avg \r");

                 final Iterator<NodeStub> iterator = nodes.iterator();
                 Optional<NodeStub> first = iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
                 NeoNode current = first.map(this::getOrCreate).orElse(null);
                 while (current != null && iterator.hasNext()) {
                     // Get next node if exists; if not create it
                     final NeoNode next = getOrCreate(iterator.next());
                     // Link this node to the next one
                     current.createRelationshipTo(next, way, RelTypes.RAIL_LINK, graphDb);
                     // Move on
                     current = next;
                 }
             });

        System.out.println("Tracing ways complete");
    }

    private NeoNode getOrCreate(NodeStub osmNode) {
        final Optional<NeoNode> neoNode = NeoNode.getNeoNode(osmNode.getId(), graphDb);
        if (!neoNode.isPresent()) {
            return new NeoNode(osmNode, graphDb);
        } else {
            return neoNode.get();
        }
    }

    public long getEdgeCount() {
        final long count;
        try (Transaction tx = graphDb.beginTx()) {
            count = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                    GlobalGraphOperations.at(graphDb)
                                         .getAllRelationships()
                                         .iterator(),
                    Spliterator.DISTINCT),
                                         false)
                                 .count();
            tx.success();
        }
        return count;
    }

    public long getNodeCount() {
        final long nodeCount;
        try (Transaction tx = graphDb.beginTx()) {

            nodeCount = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                    GlobalGraphOperations.at(graphDb)
                                         .getAllNodes()
                                         .iterator(),
                    Spliterator.DISTINCT),
                                             false)
                                     .count();
            tx.success();
        }
        return nodeCount;
    }

    public Optional<NeoNode> getNode(long id) {
        return NeoNode.getNeoNode(id, graphDb);
    }


    public NeoNode getNode(Node node) {
        return new NeoNode(node, graphDb);
    }

    public WeightedPath calculatePath(NeoNode station,
                                      NeoNode neoDestination,
                                      CostEvaluator costFunction,
                                      EstimateEvaluator lengthEstimate) {
        PathFinder<WeightedPath> pathFinder = aStar(allTypesAndDirections(),
                                                    costFunction,
                                                    lengthEstimate); // Estimate (distance only)
        final WeightedPath path;
        try (Transaction tx = graphDb.beginTx()) {
            path = pathFinder.findSinglePath(station.getNeoNode(),
                                             neoDestination.getNeoNode());
            tx.success();
        }
        return path;
    }


    public double linkLength(Relationship relationship) {
        double length;
        try (Transaction tx = graphDb.beginTx()) {
            if (relationship.hasProperty("distance")) {
                length = Double.parseDouble(String.valueOf(relationship.getProperty("distance")));
            } else {
                final NeoNode start = new NeoNode(relationship.getStartNode(), graphDb);
                final NeoNode end = new NeoNode(relationship.getEndNode(), graphDb);
                length = ShapeUtils.calculateDistance(start.getOsmNode(), end.getOsmNode());
                relationship.setProperty("distance", String.valueOf(length));
            }
            tx.success();
        }
        return length;
    }
    public GraphDatabaseService getGraphDb() {
        return graphDb;
    }
}
