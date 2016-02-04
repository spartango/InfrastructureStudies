package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.geom.ShapeUtils;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.spartango.infra.io.Writers.*;
import static org.neo4j.graphalgo.GraphAlgoFactory.aStar;
import static org.neo4j.graphdb.PathExpanders.allTypesAndDirections;

/**
 * Author: spartango
 * Date: 10/6/15
 * Time: 16:07.
 */
public class TraverseMain {
    private static final String PATH             = "data/";
    private static final String TARGET_PATH      = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH          = PATH + "rail.db";
    private static final String GRAPH_DB_PATH    = PATH + "graph.db";
    private static final String SOURCES_GEOJSON  = PATH + "2020/sources.geojson";
    private static final String SINKS_GEOJSON    = PATH + "2020/sinks.geojson";
    private static final String SEGMENTS_GEOJSON = PATH + "2020/bridges.geojson";
    private static final String DAMAGE_GEOJSON   = PATH + "2020/damage.geojson";

    private static final long SOURCE_COUNT    = 18;
    private static final long MIN_CRITICALITY = 15;
    private static final long SINK_COUNT      = 20;

    // Damage equivalent to a track extension
    private static final double DAMAGE_COST = 2400000; // 2,400,000m @ 100km/hr = 24 hours of delay

    private static GraphDatabaseService graphDb;
    private static TieredSeeker         seeker;

    private static Map<NodeStub, List<WeightedPath>> baselinePaths;

    public static void main(String[] args) {
        setupGraph();

        // Find all stations
        final Collection<NodeStub> stations = getStations();
        final Collection<WayStub> bridges = getBridges();
        System.out.println("Starting with " + stations.size() + " stations and " + bridges.size() + " bridges");

        // Look for shortest paths
        findPaths(stations, bridges);

        System.out.println("Shutting down");
        graphDb.shutdown();
    }

    private static void setupGraph() {
        // Neo4J
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(GRAPH_DB_PATH)
                                            .setConfig(GraphDatabaseSettings.node_keys_indexable, NeoNode.OSM_ID)
                                            .setConfig(GraphDatabaseSettings.node_auto_indexing, "true")
                                            .newGraphDatabase();

        graphDb.index().getNodeAutoIndexer().startAutoIndexingProperty(NeoNode.OSM_ID);

        // Seeker
        seeker = new TieredSeeker(TARGET_PATH, DB_PATH) {
            @Override protected boolean isTarget(Entity entity) {
                return ((entity instanceof Relation &&
                         (TagUtils.hasTag(entity, "route", "train")
                          || TagUtils.hasTag(entity, "route", "railway")))
                        || (entity instanceof Way
                            && TagUtils.hasTag(entity, "railway")));
            }
        };

        System.out.println("STATIC: No seeking, data expected");
//        seeker.run();
        System.out.println("Building graph from "
                           + seeker.getRelations().size()
                           + " routes, "
                           + seeker.getWays().size()
                           + " ways, and "
                           + seeker.getNodes().size()
                           + " nodes");

//        OSMGraph graph = new OSMGraph(graphDb);
        long nodeCount;
        long edgeCount;
        // Check that the graph has been built properly

        System.out.println("STATIC: No building, data expected");
//        graph.build(seeker.getIndex());

        // Quick stats
        nodeCount = GraphMain.getNodeCount(graphDb);
        edgeCount = GraphMain.getEdgeCount(graphDb);
        System.out.println("Graph built: " + nodeCount + " nodes & " + edgeCount + " links");
    }

    private static void findPaths(Collection<NodeStub> stations, Collection<WayStub> bridges) {
        System.out.println("DEBUG: Limiting paths to " + (SOURCE_COUNT + SINK_COUNT) + " simulated station targets");

        long startTime = System.currentTimeMillis();

        // Shuffle the set of stations so we get a nice sampling
        final List<NodeStub> shuffled = new ArrayList<>(stations);
        Collections.shuffle(shuffled);
        final List<NeoNode> simPool = shuffled.parallelStream()
                                              .unordered()
                                              .map(destination -> NeoNode.getNeoNode(destination.getId(),
                                                                                     graphDb))
                                              .filter(Optional::isPresent)
                                              .limit(SOURCE_COUNT + SINK_COUNT) // Sources + Sinks
                                              .map(Optional::get)
                                              .collect(Collectors.toList());
        // Simulates known sinks
//        final List<NeoNode> sinks = simPool.stream()
//                                           .limit(SINK_COUNT)
//                                           .collect(Collectors.toList());

        final List<NeoNode> sinks = Stream.of(3195094191l, 2874005142l, 1681825138l,
                                              2971189978l, 269838555l, 2051979297l,
                                              2699872473l, 3048742262l, 3476981835l,
                                              843052502l, 270146375l, 1658989377l,
                                              339089288l, 1582348731l, 677180563l,
                                              2023044210l, 2333085945l, 525377519l,
                                              843121428l, 1577895082l, 2483530943l,
                                              2651768079l, 661025343l, 3662634093l,
                                              1642904934l, 3019467507l, 2279127731l)
                                          .map(id -> NeoNode.getNeoNode(id, graphDb))
                                          .filter(Optional::isPresent)
                                          .map(Optional::get)
                                          .collect(Collectors.toList());

        // Simulates known sources
        final List<NeoNode> sources = simPool.stream()
//                                             .skip(SINK_COUNT) Skip the ones we've alrady seen
                                             .filter(node -> !sinks.contains(node)) // Paranoid
                                             .limit(SOURCE_COUNT)
                                             .collect(Collectors.toList());

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

        // Baseline
        startTime = System.currentTimeMillis();
        baselinePaths = generatePaths(sinks, sources);
        System.out.println("Calculated paths in "
                           + (System.currentTimeMillis() - startTime)
                           + "ms");

        // Write the baseline paths
        baselinePaths.forEach((station, paths) -> write(station,
                                                        paths,
                                                        PATH + "2020/20_" + station.getId() + "_path.geojson",
                                                        graphDb));

        // Calculate the total cost (baseline)
        double baselineCost = calculateCost(baselinePaths);

        // Histogram segments
        startTime = System.currentTimeMillis();
        Map<Set<NodeStub>, Set<NodeStub>> histogram = histogramPaths(baselinePaths);
        System.out.println("Histogrammed segments in "
                           + (System.currentTimeMillis() - startTime)
                           + "ms");


        // Filter the histogram for bridges
        final Map<Set<NodeStub>, Set<NodeStub>> bridgeHistogram = findBridges(histogram, bridges);
        writeHistogram(bridgeHistogram, SEGMENTS_GEOJSON);

        // For each segment
        final Map<Set<NodeStub>, Double> segmentCosts = damageSegments(sinks, sources, bridgeHistogram);
        writeSegments(segmentCosts, DAMAGE_GEOJSON);
    }

    private static Map<Set<NodeStub>, Double> damageSegments(List<NeoNode> sinks,
                                                             List<NeoNode> sources,
                                                             Map<Set<NodeStub>, Set<NodeStub>> bridgeHistogram) {
        long count = bridgeHistogram.entrySet()
                                    .stream() // This could really take a while
                                    .filter(entry -> entry.getValue().size()
                                                     >= MIN_CRITICALITY).count();
        System.out.println("Damaging " + count + " bridges, one at a time");
        return bridgeHistogram.entrySet()
                              .parallelStream() // This could really take a while
                              .filter(entry -> entry.getValue().size()
                                               >= MIN_CRITICALITY) // Eliminate low criticality bridges
                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                        entry -> {
                                                            final Set<NodeStub> pair = entry.getKey();
                                                            final Set<NodeStub> affected = entry.getValue();
                                                            // Don't recalculate paths from sources that don't include damage
                                                            final Set<NeoNode> affectedSources =
                                                                    sources.stream()
                                                                           .filter(source -> affected.contains(source.getOsmNode()))
                                                                           .collect(Collectors.toSet());

                                                            // Generate the new paths with the damage marked
                                                            long damageTime = System.currentTimeMillis();
                                                            final Map<NodeStub, List<WeightedPath>> adjustedRoutes =
                                                                    generatePaths(sinks, affectedSources, pair);

                                                            // Calculate the costs again
                                                            final double newCost = calculateCost(adjustedRoutes);

                                                            // Get the unaffected route costs

                                                            // Sum that up using the cached paths
                                                            final double oldCost = affected.stream()
                                                                                           .filter(baselinePaths::containsKey) // Just in case
                                                                                           .map(baselinePaths::get)
                                                                                           .flatMap(List::stream)
                                                                                           .mapToDouble(
                                                                                                   WeightedPath::weight)
                                                                                           .sum();

                                                            System.out.println("Calculated damage for "
                                                                               + pair.hashCode()
                                                                               + " w/ "
                                                                               + affected.size()
                                                                               + " in "
                                                                               + (System.currentTimeMillis()
                                                                                  - damageTime)
                                                                               + "ms: "
                                                                               + newCost
                                                                               + " vs "
                                                                               + oldCost);

                                                            // Compute the change from the baseline
                                                            return (newCost - oldCost);
                                                        }));
    }

    private static Map<Set<NodeStub>, Set<NodeStub>> findBridges(Map<Set<NodeStub>, Set<NodeStub>> histogram,
                                                                 Collection<WayStub> bridges) {

        // Build up a structure that makes it easy to check for bridges
        final Set<Long> bridgeNodeIds = bridges.stream()
                                               .flatMap(bridge -> bridge.getNodeIds().stream())
                                               .collect(Collectors.toSet());
        return histogram.entrySet()
                        .stream()
                        .filter(entry -> {
                            final Set<NodeStub> pair = entry.getKey();
                            return pair.stream()
                                       .map(NodeStub::getId)
                                       .allMatch(bridgeNodeIds::contains);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                                  Map.Entry::getValue));
    }

    private static double calculateCost(Map<NodeStub, List<WeightedPath>> generated) {
        return generated.values()
                        .stream()
                        .flatMap(List::stream)
                        .mapToDouble(WeightedPath::weight).sum();
    }

    private static Map<NodeStub, List<WeightedPath>> generatePaths(Collection<NeoNode> sinks,
                                                                   Collection<NeoNode> sources) {
        return generatePaths(sinks, sources, Collections.EMPTY_SET);
    }

    private static Map<NodeStub, List<WeightedPath>> generatePaths(Collection<NeoNode> sinks,
                                                                   Collection<NeoNode> sources,
                                                                   Set<NodeStub> damaged) {
        return sources.stream()
                      .peek(station -> System.out.println("Finding paths from station: " + station.getOsmNode()))
                      .collect(Collectors.toMap(NeoNode::getOsmNode,
                                                station -> sinks.parallelStream()
                                                                .unordered()
                                                                .filter(destination -> !destination.getOsmNode()
                                                                                                   .equals(station.getOsmNode()))

                                                                .map(neoDestination -> calculatePath(station,
                                                                                                     neoDestination,
                                                                                                     damaged))
                                                                .filter(path -> path != null
                                                                                && path.length()
                                                                                   != 0)
                                                                .collect(Collectors.toList())
                      ));
    }

    private static WeightedPath calculatePath(NeoNode station, NeoNode neoDestination, final Set<NodeStub> damaged) {
        PathFinder<WeightedPath> pathFinder = aStar(allTypesAndDirections(),
                                                    (rel, d) -> damageCost(rel, d, damaged), // Real cost (d + damage)
                                                    TraverseMain::lengthEstimate); // Estimate (distance only)
        final WeightedPath path;
        try (Transaction tx = graphDb.beginTx()) {
            path = pathFinder.findSinglePath(station.getNeoNode(),
                                             neoDestination.getNeoNode());
            tx.success();
        }
        return path;
    }

    private static double damageCost(Relationship relationship, Direction d, Set<NodeStub> damagedNodes) {
        boolean damaged = false;
        final Set<String> damagedIds = damagedNodes.stream()
                                                   .map(NodeStub::getId)
                                                   .map(String::valueOf)
                                                   .collect(Collectors.toSet());

        try (Transaction tx = graphDb.beginTx()) {
            if (!damagedNodes.isEmpty()) {
                // Just need to know the Identifiers
                final String startId = relationship.getStartNode().getProperty(NeoNode.OSM_ID).toString();
                final String endId = relationship.getEndNode().getProperty(NeoNode.OSM_ID).toString();

                // Check for damage
                damaged = damagedIds.contains(startId) && damagedIds.contains(endId);
                // TODO: Support multiple damaged legs properly
            }

            tx.success();
        }

        double length = linkLength(relationship, d);

        if (damaged) {
            length += DAMAGE_COST;
        }
        return length;
    }

    private static double lengthEstimate(Node start, Node end) {
        // Get node IDs
        final long startId = Long.parseLong(start.getProperty(NeoNode.OSM_ID).toString());
        final long endId = Long.parseLong(end.getProperty(NeoNode.OSM_ID).toString());

        // Look up the nodes in MapDB because it's faster than Neo4J and all we need is lat/long
        final NodeStub startNode = seeker.getIndex().getNode(startId);
        final NodeStub endNode = seeker.getIndex().getNode(endId);

        return ShapeUtils.calculateDistance(startNode, endNode);
    }

    private static double linkLength(Relationship relationship, Direction d) {
        double length;
        try (Transaction tx = graphDb.beginTx()) {
            if (relationship.hasProperty("distance")) {
                length = Double.parseDouble(String.valueOf(relationship.getProperty("distance")));
            } else {
                final NeoNode start = new NeoNode(relationship.getStartNode(), graphDb);
                final NeoNode end = new NeoNode(relationship.getEndNode(), graphDb);
                length = ShapeUtils.calculateDistance(start.getOsmNode(), end.getOsmNode());
                relationship.setProperty("distance", String.valueOf(length));
            }
            tx.success();
        }
        return length;
    }

    private static void writeStations(Collection<NeoNode> stations, String path) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Station");
        builder.add("the_geom", Point.class);
        builder.add("id", Long.class);
        builder.add("name", String.class);
        builder.add("name:en", String.class);
        builder.add("railway", String.class);

        // build the type
        final SimpleFeatureType stationType = builder.buildFeatureType();
        SimpleFeatureBuilder stationFeatureBuilder = new SimpleFeatureBuilder(stationType);
        final List<SimpleFeature> stationFeatures = new LinkedList<>();

        stations.stream().map(NeoNode::getOsmNode).forEach(startNode -> {
            final Point startPoint = geometryFactory.createPoint(new Coordinate(
                    startNode.getLongitude(),
                    startNode.getLatitude()));
            stationFeatureBuilder.add(startPoint);
            stationFeatureBuilder.add(startNode.getId());
            stationFeatureBuilder.add(startNode.getTag("name"));
            stationFeatureBuilder.add(startNode.getTag("name:en"));
            stationFeatureBuilder.add(startNode.getTag("railway"));
            final SimpleFeature startFeature = stationFeatureBuilder.buildFeature(String.valueOf(startNode.getId()));
            stationFeatures.add(startFeature);
        });

        writeFeature(path, stationType, stationFeatures);
    }

    private static Collection<NodeStub> getStations() {
        System.out.println("Finding stations...");
        final Set<NodeStub> stations = seeker.getDatabase().getHashSet("stations");
        if (stations.isEmpty()) {
            System.out.println("Building station cache");
            seeker.getNodes()
                  .stream()
                  .filter(node -> node.getTags().containsKey("railway")
                                  && node.getTag("railway").equals("station"))
                  .forEach(stations::add);
            seeker.getDatabase().commit();
        }
        return stations;
    }

    private static Collection<WayStub> getBridges() {
        System.out.println("Finding bridges...");
        final Set<WayStub> bridges = seeker.getDatabase().getHashSet("bridges");
        if (bridges.isEmpty()) {
            System.out.println("Building bridge cache");
            seeker.getWays()
                  .stream()
                  .filter(rel -> rel.getTags().containsKey("bridge"))
                  .forEach(bridges::add);
            seeker.getDatabase().commit();
        }
        return bridges;
    }

    private static Map<Set<NodeStub>, Set<NodeStub>> histogramPaths(Map<NodeStub, List<WeightedPath>> targets) {
        // Histogram
        Map<Set<NodeStub>, Set<NodeStub>> histogram = new HashMap<>();

        targets.forEach((source, paths) -> paths.forEach(path -> {
            // For each path
            if (path == null) {
                return;
            }
            try (Transaction tx = graphDb.beginTx()) {
                path.relationships().forEach(relationship -> {
                    // For each segment
                    // Pull up the nodes & data
                    final NeoNode startNode = new NeoNode(relationship.getStartNode(), graphDb);
                    final NeoNode endNode = new NeoNode(relationship.getEndNode(), graphDb);
                    final Set<NodeStub> pair = new HashSet<>(Arrays.asList(startNode.getOsmNode(),
                                                                           endNode.getOsmNode()));
                    // If we've seen this segment before
                    Set<NodeStub> set = histogram.get(pair);
                    if (set == null) {
                        set = new HashSet<>();
                        histogram.put(pair, set);
                    }
                    set.add(source);
                });

                tx.success();
            }
        }));

        System.out.println("Calculated segment histogram: " + histogram.size());
        return histogram;
    }
}
