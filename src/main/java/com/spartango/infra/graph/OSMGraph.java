package com.spartango.infra.graph;

import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.OSMIndex;
import com.spartango.infra.osm.type.NodeStub;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
        index.getWays()
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

}
