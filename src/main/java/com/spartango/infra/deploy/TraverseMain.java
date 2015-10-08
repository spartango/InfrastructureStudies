package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.geom.ShapeUtils;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.NodeStub;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.neo4j.graphalgo.GraphAlgoFactory.aStar;
import static org.neo4j.graphdb.PathExpanders.allTypesAndDirections;

/**
 * Author: spartango
 * Date: 10/6/15
 * Time: 16:07.
 */
public class TraverseMain {
    private static final String PATH            = "data/";
    private static final String TARGET_PATH     = PATH + "china-latest.osm.pbf";
    private static final String DB_PATH         = PATH + "rail.db";
    private static final String GRAPH_DB_PATH   = PATH + "graph.db";
    private static final String STATION_GEOJSON = PATH + "stations.geojson";
    private static final long   TARGET_COUNT    = 20;

    private static GraphDatabaseService graphDb;
    private static TieredSeeker         seeker;
    private static GeometryFactory      geometryFactory;

    public static void main(String[] args) {
        setupGraph();

        // Find all stations
        final Collection<NodeStub> stations = getStations();
        System.out.println("Seeding with " + stations.size());

        writeStations(stations);

        // Look for shortest paths
        findPaths(stations);

        System.out.println("Shutting down");
        graphDb.shutdown();
    }

    private static void findPaths(Collection<NodeStub> stations) {
        System.out.println("DEBUG: Limiting paths to 10 simulated station targets");

        // Simulates known sinks
        final List<NodeStub> shuffled = new ArrayList<>(stations);
        Collections.shuffle(shuffled);
        final List<NeoNode> targets = shuffled.parallelStream().unordered()
                                              .map(destination -> NeoNode.getNeoNode(destination.getId(),
                                                                                     graphDb))
                                              .filter(Optional::isPresent)
                                              .map(Optional::get)
                                              .limit(2 * TARGET_COUNT)
                                              .collect(Collectors.toList());

        System.out.println("Loaded targets");

        final long startTime = System.currentTimeMillis();
        final AtomicLong count = new AtomicLong();
        stations.stream()
                .map(station -> NeoNode.getNeoNode(station.getId(), graphDb))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(station -> System.out.println("Finding paths from station: " + station.getOsmNode()))
                .forEach(station -> {
                             final List<WeightedPath> paths =
                                     targets.parallelStream().unordered()
                                            .filter(destination -> !destination.getOsmNode()
                                                                               .equals(station.getOsmNode()))
                                            .limit(TARGET_COUNT)
                                            .peek(destination -> {
                                                long time = System.currentTimeMillis();
                                                long progress = count.incrementAndGet();
                                                System.out.print(station.getId()
                                                                 + " -> "
                                                                 + destination.getId()
                                                                 + " | "
                                                                 + progress
                                                                 + " @ "
                                                                 + (1000.0
                                                                    *
                                                                    progress
                                                                    / (time
                                                                       - startTime))
                                                                 + "/s \r");
                                            })
                                            .map(neoDestination -> {
                                                PathFinder<WeightedPath> pathFinder =
                                                        aStar(allTypesAndDirections(),
                                                              (TraverseMain::linkLength),
                                                              (start, end) -> 1.0d);


                                                final WeightedPath path;
                                                try (Transaction tx = graphDb.beginTx()) {
                                                    path = pathFinder.findSinglePath(station.getNeoNode(),
                                                                                     neoDestination.getNeoNode());
                                                    tx.success();
                                                }
                                                return path;
                                            }).filter(path -> path != null
                                                              && path.length() != 0)
                                            .collect(Collectors.toList());
                             write(station.getOsmNode(), paths);
                         }
                );
    }

    private static void writeStations(Collection<NodeStub> stations) {
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

        try {
            FeatureJSON featureJSON = new FeatureJSON();
            featureJSON.setEncodeFeatureCollectionCRS(false);
            featureJSON.setEncodeFeatureBounds(false);

            final ListFeatureCollection stationCollection = new ListFeatureCollection(stationType,
                                                                                      stationFeatures);
            featureJSON.writeFeatureCollection(stationCollection, new File(STATION_GEOJSON));

        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private static void write(NodeStub station, Collection<WeightedPath> paths) {
        // Setup schema
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Link");
        rBuilder.add("the_geom", LineString.class);
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

                final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(path.hashCode()));
                linkFeatures.add(linkFeature);

                tx.success();
            }
        });

        try {
            FeatureJSON featureJSON = new FeatureJSON();
            featureJSON.setEncodeFeatureCollectionCRS(false);
            featureJSON.setEncodeFeatureBounds(false);

            final ListFeatureCollection linkCollection = new ListFeatureCollection(linkType, linkFeatures);
            featureJSON.writeFeatureCollection(linkCollection, new File(PATH + station.getId() + "_path.geojson"));
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
