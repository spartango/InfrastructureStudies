package com.spartango.infra.deploy;

import com.spartango.infra.core.OSMGraph;
import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.targeting.Simulation;
import com.spartango.infra.targeting.load.NodeIdLoader;
import com.spartango.infra.targeting.network.RailNetwork;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.util.Arrays;

/**
 * Author: spartango
 * Date: 10/6/15
 * Time: 16:07.
 */
public class TraverseMain {
    private static final String PATH          = "data/";
    //    private static final String TARGET_PATH   = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH       = PATH + "rail.db";
    private static final String GRAPH_DB_PATH = PATH + "graph.db";

    private static GraphDatabaseService graphDb;

    public static void main(String[] args) {
        // Read the rail network files
        System.out.println("Loading existing data...");
        final RailNetwork railNet = loadNetwork();

        // Load up the sources and sinks
        final NodeIdLoader sources = new NodeIdLoader(railNet)
                .addIds(Arrays.asList(3195094191L,
                                      2874005142L,
                                      1681825138L,
                                      2971189978L,
                                      269838555L,
                                      2051979297L,
                                      2699872473L,
                                      3048742262L,
                                      3476981835L,
                                      843052502L,
                                      270146375L,
                                      1658989377L,
                                      339089288L,
                                      582373939L,
                                      3499304147L,
                                      2987122176L));

        final NodeIdLoader sinks = new NodeIdLoader(railNet)
                .addIds(Arrays.asList(1582348731L,
                                      677180563L,
                                      2023044210L,
                                      2333085945L,
                                      525377519L,
                                      843121428L,
                                      1577895082L,
                                      2483530943L,
                                      2651768079L,
                                      661025343L,
                                      3662634093L,
                                      1642904934L,
                                      3019467507L,
                                      2279127731L,
                                      2999345286L,
                                      1584384382L,
                                      2451329911L,
                                      2623140053L));

        Simulation simulation = new Simulation(0, railNet, sources, sinks);
        simulation.run();

        // Shutdown
        cleanUp();
    }

    private static void cleanUp() {
        System.out.println("Shutting down");
        graphDb.shutdown();
    }

    private static RailNetwork loadNetwork() {
        // Neo4J
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(GRAPH_DB_PATH)
                                            .setConfig(GraphDatabaseSettings.node_keys_indexable, NeoNode.OSM_ID)
                                            .setConfig(GraphDatabaseSettings.node_auto_indexing, "true")
                                            .newGraphDatabase();

        graphDb.index().getNodeAutoIndexer().startAutoIndexingProperty(NeoNode.OSM_ID);

        // MapDB
        final DB database = DBMaker.newFileDB(new File(DB_PATH))
                                   .mmapFileEnable()
                                   .closeOnJvmShutdown()
                                   .make();


        // Load up the pre-built indices
        final OSMIndex index = new OSMIndex(database);
        final OSMGraph graph = new OSMGraph(graphDb);

        return new RailNetwork(graph, index, database);
    }

}
