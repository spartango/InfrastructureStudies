package com.spartango.infra.deploy;

import com.spartango.infra.core.OSMIndex;
import com.spartango.infra.elevation.ElevationSource;
import com.spartango.infra.elevation.GeonamesSource;
import com.spartango.infra.osm.type.NodeStub;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/20/16
 * Time: 15:32.
 */
public class ElevationMain {
    private static final String PATH        = "data/";
    //    private static final String TARGET_PATH   = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH     = PATH + "rail.db";
    private static final String ELE_DB_PATH = PATH + "rail_e.db";

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

        ElevationSource elevationSource = new GeonamesSource(GeonamesSource.SRTM, "spartango");
        final AtomicInteger count = new AtomicInteger();
        memNodes.parallelStream()
                .filter(node -> (node.getTag("ele") == null) || (Double.parseDouble(node.getTag("ele")) == -32768))
                .forEach(node -> {
                    long startTime = System.currentTimeMillis();
                    Optional<Double> elevation = elevationSource.getElevation(node.getLatitude(), node.getLongitude());
                    elevation.ifPresent(value -> {
                        node.putTag("ele", String.valueOf(value));
                        System.out.println("Got Elevation: " + elevation
                                           + " for #" + node.getId()
                                           + " -> " + count.incrementAndGet()
                                           + "/" + total
                                           + " in " + (System.currentTimeMillis() - startTime) + "ms");
                        index.updateNode(node);
                    });
                });
        System.out.println("Fetched elevation for " + count + " nodes");

    }
}
