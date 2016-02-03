package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.geom.ShapeUtils;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    private static final long SOURCE_COUNT = 20;
    private static final long SINK_COUNT   = 20;

    private static GraphDatabaseService graphDb;
    private static TieredSeeker         seeker;
    private static GeometryFactory      geometryFactory;

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
        writeStations(sources.stream().map(NeoNode::getOsmNode).collect(Collectors.toList()), SOURCES_GEOJSON);

        // Write sinks
        writeStations(sinks.stream().map(NeoNode::getOsmNode).collect(Collectors.toList()), SINKS_GEOJSON);

        // Baseline
        startTime = System.currentTimeMillis();
        final Map<NodeStub, List<WeightedPath>> generated = generatePaths(sinks, sources);
        System.out.println("Calculated paths in "
                           + (System.currentTimeMillis() - startTime)
                           + "ms");

        // Write the baseline paths
        generated.forEach((station, paths) -> write(station,
                                                    paths,
                                                    PATH + "2020/20_" + station.getId() + "_path.geojson"));

        // Write histogrammed segments
        startTime = System.currentTimeMillis();
        writeSegments(generated, bridges, SEGMENTS_GEOJSON);
        System.out.println("Wrote bridges in "
                           + (System.currentTimeMillis() - startTime)
                           + "ms");
        // For each segment
        // Mark it damaged
        // TODO: Calculate the costs again
    }

    private static Map<NodeStub, List<WeightedPath>> generatePaths(List<NeoNode> sinks, List<NeoNode> sources) {
        return sources.stream()
                      .peek(station -> System.out.println("Finding paths from station: " + station.getOsmNode()))
                      .collect(Collectors.toMap(NeoNode::getOsmNode,
                                                station -> sinks.parallelStream()
                                                                .unordered()
                                                                .filter(destination -> !destination.getOsmNode()
                                                                                                   .equals(station.getOsmNode()))

                                                                .map(neoDestination -> calculatePath(station,
                                                                                                     neoDestination))
                                                                .filter(path -> path != null
                                                                                && path.length()
                                                                                   != 0)
                                                                .collect(Collectors.toList())
                      ));
    }

    private static WeightedPath calculatePath(NeoNode station, NeoNode neoDestination) {
        PathFinder<WeightedPath> pathFinder = aStar(allTypesAndDirections(),
                                                    (TraverseMain::linkLength),
                                                    (start, end) -> 1.0d);
        final WeightedPath path;
        try (Transaction tx = graphDb.beginTx()) {
            path = pathFinder.findSinglePath(station.getNeoNode(),
                                             neoDestination.getNeoNode());
            tx.success();
        }
        return path;
    }

    private static void writeStations(Collection<NodeStub> stations, String path) {
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

        stations.forEach(startNode -> {
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

    private static void writeSegments(Map<NodeStub, List<WeightedPath>> targets,
                                      Collection<WayStub> bridges,
                                      String filePath) {
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

        // Feature type definitions
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Segment");
        rBuilder.add("the_geom", LineString.class);
        rBuilder.add("criticality", Integer.class);
//        rBuilder.add("mean_cost", Double.class);

        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);

        // Collection of segments
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        // Build up a structure that makes it easy to check for bridges
        final Set<Long> bridgeNodeIds = bridges.stream()
                                               .flatMap(bridge -> bridge.getNodeIds().stream())
                                               .collect(Collectors.toSet());

        // For each segment we've seen
        histogram.forEach((pair, criticality) -> {
//            if (criticality.size() > SOURCE_COUNT / 2) { // Ignore low criticality segments
            // Check if this is a bridge
            final boolean isBridge = pair.stream()
                                         .map(NodeStub::getId)
                                         .allMatch(bridgeNodeIds::contains);
            if (isBridge) {
                // Create it a line feature
                final List<Coordinate> coordinateList = pair.stream()
                                                            .map(nodeStub -> new Coordinate(nodeStub.getLongitude(),
                                                                                            nodeStub.getLatitude()))
                                                            .collect(Collectors.toList());

                final LineString lineString = geometryFactory.createLineString(
                        coordinateList.toArray(new Coordinate[coordinateList.size()]));

                // Add the Criticality
                linkFeatureBuilder.add(lineString);
                linkFeatureBuilder.add(criticality.size());

                // Add it to the collection
                final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(pair.hashCode()));
                linkFeatures.add(linkFeature);
            }
//            }
        });

        // Write the collection
        writeFeature(filePath, linkType, linkFeatures);
    }

    private static void write(NodeStub station, Collection<WeightedPath> paths, String filePath) {
        // Setup schema
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Link");
        rBuilder.add("the_geom", LineString.class);
        rBuilder.add("cost", Double.class);
        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        paths.forEach(path -> {
            if (path == null) {
                return;
            }
            // Build the geometry
            try (Transaction tx = graphDb.beginTx()) {
                final List<Coordinate> coordinateList =
                        StreamSupport.stream(path.nodes().spliterator(), false)
                                     .map(neoNode -> new NeoNode(neoNode, graphDb))
                                     .map(nodeStub -> new Coordinate(nodeStub.getLongitude(), nodeStub.getLatitude()))
                                     .collect(Collectors.toList());
                final LineString lineString = geometryFactory.createLineString(
                        coordinateList.toArray(new Coordinate[coordinateList.size()]));
                linkFeatureBuilder.add(lineString);
                linkFeatureBuilder.add(path.weight());

                final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(path.hashCode()));
                linkFeatures.add(linkFeature);

                tx.success();
            }
        });

        writeFeature(filePath, linkType, linkFeatures);
    }

    private static void writeFeature(String filePath, SimpleFeatureType linkType, List<SimpleFeature> linkFeatures) {
        try {
            FeatureJSON featureJSON = new FeatureJSON();
            featureJSON.setEncodeFeatureCollectionCRS(false);
            featureJSON.setEncodeFeatureBounds(false);

            final ListFeatureCollection linkCollection = new ListFeatureCollection(linkType, linkFeatures);
            featureJSON.writeFeatureCollection(linkCollection,
                                               new File(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setupGraph() {
        geometryFactory = JTSFactoryFinder.getGeometryFactory();

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
}
