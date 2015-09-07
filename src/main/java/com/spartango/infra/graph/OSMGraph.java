package com.spartango.infra.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 17:24.
 */


public class OSMGraph {
    private enum RelTypes implements RelationshipType {
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
}
