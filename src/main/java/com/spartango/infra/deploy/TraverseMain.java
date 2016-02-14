package com.spartango.infra.deploy;

import com.spartango.infra.core.OSMGraph;
import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.damage.HistogramTargeter;
import com.spartango.infra.targeting.load.NodeIdLoader;
import com.spartango.infra.targeting.network.RailFlow;
import com.spartango.infra.targeting.network.RailNetwork;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.spartango.infra.io.Writer.*;

/**
 * Author: spartango
 * Date: 10/6/15
 * Time: 16:07.
 */
public class TraverseMain {
    private static final String PATH          = "data/";
    private static final String TARGET_PATH   = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH       = PATH + "rail.db";
    private static final String GRAPH_DB_PATH = PATH + "graph.db";

    private static final String OUTPUT_PATH      = "testing/";
    private static final String SOURCES_GEOJSON  = PATH + OUTPUT_PATH + "sources.geojson";
    private static final String SINKS_GEOJSON    = PATH + OUTPUT_PATH + "sinks.geojson";
    private static final String SEGMENTS_GEOJSON = PATH + OUTPUT_PATH + "bridges.geojson";
    private static final String BASELINE_GEOJSON = PATH + OUTPUT_PATH + "baseline.geojson";
    private static final String DAMAGE_GEOJSON   = PATH + OUTPUT_PATH + "damage.geojson";

    // Damage equivalent to a track extension
    private static final long BRIDGE_LIMIT = 10000;

    private static GraphDatabaseService graphDb;
    private static DB                   database;

    private static OSMIndex index;
    private static OSMGraph graph;

    public static void main(String[] args) {
        // Read the rail network files
        System.out.println("Loading existing data...");
        final RailNetwork railNet = loadNetwork();

        long startTime = System.currentTimeMillis();

        // Load up the sources and sinks
        final List<NeoNode> sources = new NodeIdLoader(railNet)
                .addIds(Arrays.asList(3195094191l,
                                      2874005142l,
                                      1681825138l,
                                      2971189978l,
                                      269838555l,
                                      2051979297l,
                                      2699872473l,
                                      3048742262l,
                                      3476981835l,
                                      843052502l,
                                      270146375l,
                                      1658989377l,
                                      339089288l,
                                      582373939l,
                                      3499304147l,
                                      2987122176l)).load();

        final List<NeoNode> sinks = new NodeIdLoader(railNet)
                .addIds(Arrays.asList(1582348731l,
                                      677180563l,
                                      2023044210l,
                                      2333085945l,
                                      525377519l,
                                      843121428l,
                                      1577895082l,
                                      2483530943l,
                                      2651768079l,
                                      661025343l,
                                      3662634093l,
                                      1642904934l,
                                      3019467507l,
                                      2279127731l,
                                      2999345286l,
                                      1584384382l,
                                      2451329911l)).load();

        System.out.println("Loaded "
                           + sources.size()
                           + " sources and "
                           + sinks.size()
                           + " sinks in "
                           + (System.currentTimeMillis() - startTime)
                           + "ms");

        // Write sources
        writeStations(sources, SOURCES_GEOJSON);

        // Write sinks
        writeStations(sinks, SINKS_GEOJSON);

        startTime = System.currentTimeMillis();
        System.out.println("Calculating baseline...");

        // Calculate the baseline flow from these sources and sinks
        RailFlow baselineFlow = new RailFlow(railNet, sources, sinks);

        System.out.println("Calculated baseline flow in "
                           + (System.currentTimeMillis() - startTime)
                           + "ms");

        // Write the baseline flow
        writeFlow(baselineFlow, BASELINE_GEOJSON);

        // Histogram the segments, only including bridges
        final Map<Set<NodeStub>, Set<NodeStub>> histogram = baselineFlow.histogramPaths(railNet.getBridges());

        // Write the histogram of bridges (criticality)
        writeHistogram(histogram, SEGMENTS_GEOJSON);

        // Rank targets with the histogram
        HistogramTargeter targeter = new HistogramTargeter(baselineFlow, histogram, BRIDGE_LIMIT);

        final long damageStartTime = System.currentTimeMillis();

        final Map<Set<NodeStub>, Double> resilienceScores = new ConcurrentHashMap<>();
        // Simulate Damage, get the changes
        targeter.deltaStream()
                .peek(railFlow -> writeFlow(railFlow, // Write the altered paths
                                            PATH
                                            + OUTPUT_PATH
                                            + railFlow.getDamagedNodes().hashCode()
                                            + "_damage.geojson"))
                .peek((x) -> System.out.println("Calculated damaged flow after "
                                                + (System.currentTimeMillis() - damageStartTime)
                                                + "ms"))
                .forEach(deltaFlow -> {
                    double baseCost = baselineFlow.calculateCost(deltaFlow.getSources());
                    double deltaCost = deltaFlow.getTotalCost() - baseCost;

                    resilienceScores.put(deltaFlow.getDamagedNodes(), deltaCost);

                    // Write the resilience scores in progress
                    writeSegments(resilienceScores, DAMAGE_GEOJSON);
                });

        System.out.println("Finished damage analysis");

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
        database = DBMaker.newFileDB(new File(DB_PATH))
                          .mmapFileEnable()
                          .closeOnJvmShutdown()
                          .make();


        // Load up the pre-built indices
        index = new OSMIndex(database);
        graph = new OSMGraph(graphDb);

        return new RailNetwork(graph, index, database);
    }

}
