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
//    private static final String TARGET_PATH   = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH       = PATH + "rail.db";
    private static final String GRAPH_DB_PATH = PATH + "graph.db";

    private static final String OUTPUT_PATH      = "testing/";
    private static final String SOURCES_GEOJSON  = PATH + OUTPUT_PATH + "sources.geojson";
    private static final String SINKS_GEOJSON    = PATH + OUTPUT_PATH + "sinks.geojson";
    private static final String SEGMENTS_GEOJSON = PATH + OUTPUT_PATH + "bridges.geojson";
    private static final String BASELINE_GEOJSON = PATH + OUTPUT_PATH + "baseline.geojson";
    private static final String DAMAGE_GEOJSON   = PATH + OUTPUT_PATH + "damage.geojson";

    private static final long BRIDGE_LIMIT = 5000;
    private static GraphDatabaseService graphDb;

    public static void main(String[] args) {
        // Read the rail network files
        System.out.println("Loading existing data...");
        final RailNetwork railNet = loadNetwork();

        long startTime = System.currentTimeMillis();

        // Load up the sources and sinks
        final List<NeoNode> sources = new NodeIdLoader(railNet)
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
                                      2987122176L)).load();

        final List<NeoNode> sinks = new NodeIdLoader(railNet)
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
                                      2451329911L)).load();

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
                .peek((x) -> System.out.println("Calculated damaged flow after " + (System.currentTimeMillis()
                                                                                    - damageStartTime) + "ms"))
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
