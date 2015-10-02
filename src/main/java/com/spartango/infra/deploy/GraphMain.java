package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.graph.OSMGraph;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.RelationStub;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;

import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

/**
 * Author: spartango
 * Date: 10/1/15
 * Time: 21:56.
 */
public class GraphMain {
    private static final String TARGET_PATH   = "data/china-latest.osm.pbf";
    private static final String DB_PATH       = "data/routes.db";
    private static final String GRAPH_DB_PATH = "data/" + System.currentTimeMillis() + "_graph.db";

    public static void main(String[] args) {
        // Seeker
        TieredSeeker seeker = new TieredSeeker(TARGET_PATH, DB_PATH) {
            @Override protected boolean isTarget(Entity entity) {
                return (entity instanceof Relation &&
                        (TagUtils.hasTag(entity, "route", "train")
                         || TagUtils.hasTag(entity, "route", "railway")));
            }
        };
        seeker.run();
        final Collection<RelationStub> routes = seeker.getRelations();
        System.out.println("Building graph from " + routes.size() + " routes");

        final GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(GRAPH_DB_PATH)
                                                                       .setConfig(
                                                                               GraphDatabaseSettings.node_keys_indexable,
                                                                               NeoNode.OSM_ID)
                                                                       .setConfig(GraphDatabaseSettings.node_auto_indexing,
                                                                                  "true")
                                                                       .newGraphDatabase();

        graphDb.index().getNodeAutoIndexer().startAutoIndexingProperty(NeoNode.OSM_ID);

        OSMGraph graph = new OSMGraph(graphDb);
        graph.build(seeker.getIndex());

        // Check that the graph has been built properly
        try (Transaction tx = graphDb.beginTx()) {
            final long nodeCount = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                                                GlobalGraphOperations.at(graphDb)
                                                                                     .getAllNodes()
                                                                                     .iterator(),
                                                                Spliterator.DISTINCT),
                                                        false).
                                                        peek(node -> {
                                                            final Object id = node.getProperty(NeoNode.OSM_ID);
                                                            System.out.print("Node: " + id + "\r");
                                                        })
                                                .count();
            final long edgeCount = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                                                GlobalGraphOperations.at(graphDb)
                                                                                     .getAllRelationships()
                                                                                     .iterator(),
                                                                Spliterator.DISTINCT),
                                                        false)
                                                .count();
            System.out.println("Graph built: " + nodeCount + " & " + edgeCount);
            tx.success();
        }

        graphDb.shutdown();

    }
}
