package com.spartango.infra.deploy;

import com.aol.cyclops.types.futurestream.LazyFutureStream;
import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.elevation.MapzenSource;
import com.spartango.infra.osm.type.NodeStub;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/20/16
 * Time: 15:32.
 */
public class ElevationMain {
    private static final String PATH    = "data/";
    //    private static final String TARGET_PATH   = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH = PATH + "rail.db";

    public static void main(String[] args) {
        // MapDB
        final DB database = DBMaker.newFileDB(new File(DB_PATH))
                                   .mmapFileEnable()
                                   .closeOnJvmShutdown()
                                   .make();

        // Load up the pre-built indices
        final OSMIndex index = new OSMIndex(database);

        // Get all the nodes in the Index
        final Collection<NodeStub> nodes = index.getNodes();
        // Load everything into memory. This is a terrible idea.
        System.out.println("Loading nodes...");
        final List<NodeStub> memNodes = nodes.stream()
                                             .filter(node -> (node.getTag("ele") == null))
                                             .collect(Collectors.toList());
        final long total = memNodes.size();
        System.out.println("Fetching elevation for " + total + " nodes");

        MapzenSource elevationSource = new MapzenSource("elevation-FFeA06k");
        final AtomicInteger count = new AtomicInteger();
        LazyFutureStream.lazyFutureStreamFromIterable(memNodes)
                        .filter(node -> (node.getTag("ele") == null) || (Double.parseDouble(node.getTag("ele"))
                                                                         == -32768))
                        .grouped(125)
                        .forEach(batch -> {
                            long startTime = System.currentTimeMillis();
                            final Map<NodeStub, Double> elevations = elevationSource.getElevations(batch);
                            final List<NodeStub> updated = elevations.entrySet()
                                                                     .stream()
                                                                     .map(entry -> {
                                                                         entry.getKey()
                                                                              .putTag("ele",
                                                                                      String.valueOf(entry.getValue()));
                                                                         return entry.getKey();
                                                                     }).collect(Collectors.toList());
                            index.updateNodes(updated);
                            long endTime = System.currentTimeMillis();
                            System.out.print("\rGot elevations for " + updated.size()
                                             + " -> " + count.addAndGet(updated.size())
                                             + "/" + total
                                             + " in " + (endTime - startTime) + "ms");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();  //TODO handle e
                            }
                        });
        System.out.println("\nFetched elevation for " + count + " nodes");

    }
}
