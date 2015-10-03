package com.spartango.infra.graph;

import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.OSMIndex;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        // Pull out the stops as nodes, and uniquely enter them into the db
        index.getRelations()
             .values()
             .stream()
             .flatMap(route -> route.getNodes(index).stream())
             .distinct()
             .forEach(relationStub -> new NeoNode(relationStub, graphDb));

        // Attach the appropriate links
        index.getRelations()
             .values()
             .forEach(route -> {
                 // Pull up the graph nodes for the route
                 final List<NeoNode> neoStops = route.getNodes(index)
                                                     .stream()
                                                     .map(stop -> NeoNode.getNeoNode(stop.getId(), graphDb))
                                                     .filter(Optional::isPresent)
                                                     .map(Optional::get)
                                                     .collect(Collectors.toList());

                 // Reachability
                 neoStops.forEach(stop -> neoStops.stream()
                                                  .filter(otherStop -> !stop.equals(otherStop))
                                                  .forEach(otherStop -> stop.createRelationshipTo(otherStop,
                                                                                                  RelTypes.REACHABLE,
                                                                                                  graphDb)));

                 // Find the containing ways & adjacents
//                 neoStops.forEach(stop -> route.getWays(index)
//                                               .stream()
//                                               .filter(way -> way.contains(stop.getOsmNode()))
//                                               .forEach(way -> neoStops.stream() // Extract the stops contained in each way
//                                                                .filter(otherStop -> !otherStop.equals(stop)
//                                                                                     && way.contains(otherStop.getOsmNode()))
//                                                                .forEach(otherStop ->
//                                                                                 stop.createRelationshipTo(otherStop,
//                                                                                                           RelTypes.RAIL_LINK,
//                                                                                                           graphDb))
//                                               ));
             });
    }
}
