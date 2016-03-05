package com.spartango.infra.interactive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spartango.infra.core.OSMGraph;
import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.targeting.Simulation;
import com.spartango.infra.targeting.load.NodeIdLoader;
import com.spartango.infra.targeting.network.RailNetwork;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Author: spartango
 * Date: 3/5/16
 * Time: 11:30.
 */
public class SimulationServer {
    private static final String PATH          = "data/";
    private static final String DB_PATH       = PATH + "rail.db";
    private static final String GRAPH_DB_PATH = PATH + "graph.db";
    private static final int    DEFAULT_PORT  = 8080;

    private static GraphDatabaseService graphDb;
    private static RailNetwork railNetwork = loadNetwork();

    private Undertow        server;
    private ExecutorService executor;
    private AtomicLong      idCount;

    public SimulationServer() {
        this(DEFAULT_PORT);
    }

    public SimulationServer(int port) {
        // Serve up the usual files by default
        final ResourceHandler resource = Handlers.resource(new FileResourceManager(new File("viz/src"),
                                                                                   1024,
                                                                                   true,
                                                                                   true));
        final RoutingHandler router = Handlers.routing()
                                              .post("/simulate", this::handleSimulationRequest)
                                              .setFallbackHandler(resource);
        // Handle simulation requests
        server = Undertow.builder()
                         .setHandler(router)
                         .addHttpListener(port, "localhost").build();

        idCount = new AtomicLong();
        executor = Executors.newSingleThreadExecutor(); // One at a time, as each simulation will take a while
    }

    private void handleSimulationRequest(HttpServerExchange httpServerExchange) {
        // Boot this to a worker thread
        if (httpServerExchange.isInIoThread()) {
            httpServerExchange.dispatch(this::handleSimulationRequest);
            return;
        }

        System.out.println("Handling simulation request");

        // Pull up the json body
        ObjectMapper mapper = new ObjectMapper();
        httpServerExchange.startBlocking();
        final NodeIdLoader sinks = new NodeIdLoader(railNetwork);
        try {
            final InputStream inputStream = httpServerExchange.getInputStream();
            final JsonNode json = mapper.readTree(inputStream);

            // Add to Sink loader
            json.get("sinks").forEach(node -> sinks.addId(node.longValue()));
        } catch (IOException e) {
            e.printStackTrace();
            httpServerExchange.setStatusCode(400);
            httpServerExchange.endExchange();
            return;
        }

        // Use the prefab sourceloader
        final NodeIdLoader sources = new NodeIdLoader(railNetwork)
                .addIds(Arrays.asList(3195094191L,
                                      2874005142L));

        // Generate an ID
        long id = idCount.incrementAndGet();
        Simulation sim = new Simulation(id, railNetwork, sources, sinks);

        System.out.println("Starting simulation " + id);
        // Spin off the sim in its own thread(s)
        executor.execute(sim);

        // Send back the ID for the client to track
        httpServerExchange.setStatusCode(200);
        httpServerExchange.getResponseSender().send("{ \"id\": " + id + "}");
        httpServerExchange.endExchange();

    }

    public void start() {
        server.start();
    }

    public static void cleanUp() {
        System.out.println("Shutting down");
        graphDb.shutdown();
    }

    private static RailNetwork loadNetwork() {
        System.out.println("Loading existing data...");

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

    public void stop() {
        server.stop();
    }
}
