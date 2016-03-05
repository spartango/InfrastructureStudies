package com.spartango.infra.targeting;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.io.Writer;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.damage.HistogramTargeter;
import com.spartango.infra.targeting.load.NodeLoader;
import com.spartango.infra.targeting.network.RailFlow;
import com.spartango.infra.targeting.network.RailNetwork;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.currentTimeMillis;

/**
 * Author: spartango
 * Date: 3/5/16
 * Time: 11:18.
 */
public class Simulation implements Runnable {
    private static final String PATH         = "data/elevation/";
    private static final long   BRIDGE_LIMIT = 10;

    private final long id;

    private RailNetwork railNet;
    private NodeLoader  sourceLoader;
    private NodeLoader  sinkLoader;
    private long        targetCount;

    public Simulation(long id,
                      RailNetwork railNet,
                      NodeLoader sourceLoader,
                      NodeLoader sinkLoader) {
        this(id, railNet, sourceLoader, sinkLoader, BRIDGE_LIMIT);
    }

    public Simulation(long id,
                      RailNetwork railNet,
                      NodeLoader sourceLoader,
                      NodeLoader sinkLoader,
                      long targetCount) {
        this.id = id;
        this.railNet = railNet;
        this.sinkLoader = sinkLoader;
        this.sourceLoader = sourceLoader;
        this.targetCount = targetCount;
    }

    public void run() {
        final Writer writer = new Writer(PATH + id, railNet);
        long startTime = currentTimeMillis();

        // Load up the sources and sinks
        final List<NeoNode> sources = sourceLoader.loadGraphNodes();
        final List<NeoNode> sinks = sinkLoader.loadGraphNodes();

        log("Loaded "
            + sources.size() + " sources and "
            + sinks.size() + " sinks in "
            + (currentTimeMillis() - startTime) + "ms");

        // Write sources & sinks
        writer.writeStationNodes("sources", sources);
        writer.writeStationNodes("sinks", sinks);

        // Calculate the baseline flow from these sources and sinks
        startTime = currentTimeMillis();
        log("Calculating baseline...");

        RailFlow baselineFlow = new RailFlow(railNet, sources, sinks);
        writer.writeFlow("baseline", baselineFlow);

        log("Calculated baseline in " + (currentTimeMillis() - startTime) + "ms");

        // Histogram the segments, only including bridges
        final Map<Set<NodeStub>, Set<NodeStub>> histogram = baselineFlow.histogramPaths(railNet.getBridges());
        writer.writeSharedSegments("bridges", histogram);

        // Rank targets with the histogram
        HistogramTargeter targeter = new HistogramTargeter(baselineFlow, histogram, targetCount);

        // Simulate Damage, get the changes
        final long damageStartTime = currentTimeMillis();
        final Map<Set<NodeStub>, Double> resilienceScores = new ConcurrentHashMap<>();
        targeter.deltaStream()
                .peek((x) -> log("Calculated damage in " +
                                 (currentTimeMillis() - damageStartTime) + "ms"))
//                .peek(deltaFlow -> writer.writeFlow(deltaFlow.getDamagedNodes().hashCode() + "_damage", deltaFlow))
                .forEach(deltaFlow -> {
                    // Calculate the cost of adjustment
                    double baseCost = baselineFlow.calculateCost(deltaFlow.getSources());
                    double deltaCost = deltaFlow.getTotalCost() - baseCost;
                    resilienceScores.put(deltaFlow.getDamagedNodes(), deltaCost);

                    // Write the resilience scores in progress
                    writer.writeHistogram("damage", resilienceScores);
                });

        log("Finished damage analysis");
    }

    private void log(String logString) {
        System.out.println("Sim " + id + ": " + logString);
    }
}
