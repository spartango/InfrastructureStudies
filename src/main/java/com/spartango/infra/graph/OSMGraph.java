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
        index.getRelations().values()
             .stream()
             .map(route -> route.getNodes(index))
             .forEach(stops -> {
                 // Pull up the graph nodes for the route
                 final List<NeoNode> neoStops = stops.stream()
                                                     .map(stop -> NeoNode.getNeoNode(stop.getId(), graphDb))
                                                     .filter(Optional::isPresent)
                                                     .map(Optional::get)
                                                     .collect(Collectors.toList());
                 // Completely connect them.
                 neoStops.stream().forEach(stop -> neoStops.stream()
                                                           .forEach(otherStop -> stop.createRelationshipTo(otherStop,
                                                                                                       RelTypes.RAIL_LINK,
                                                                                                       graphDb)));
             });
    }
}
