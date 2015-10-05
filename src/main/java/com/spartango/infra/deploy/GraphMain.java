package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.graph.OSMGraph;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.RelationStub;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.tooling.GlobalGraphOperations;
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

/**
 * Author: spartango
 * Date: 10/1/15
 * Time: 21:56.
 */
public class GraphMain {
    private static final String TARGET_PATH     = "data/china-latest.osm.pbf";
    private static final String DB_PATH         = "data/routes.db";
    private static final String GRAPH_DB_PATH   = "data/" + System.currentTimeMillis() + "_graph.db";
    private static final String GEO_PATH        = "data/graph.geojson";
    public static final  String STATION_GEOJSON = "data/s_station_graph.geojson";
    public static final  String LINK_GEOJSON    = "data/s_link_graph.geojson";

    public static void main(String[] args) {
        // Neo4J
        final GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(GRAPH_DB_PATH)
                                                                       .setConfig(
                                                                               GraphDatabaseSettings.node_keys_indexable,
                                                                               NeoNode.OSM_ID)
                                                                       .setConfig(GraphDatabaseSettings.node_auto_indexing,
                                                                                  "true")
                                                                       .newGraphDatabase();

        graphDb.index().getNodeAutoIndexer().startAutoIndexingProperty(NeoNode.OSM_ID);


        // Seeker
        TieredSeeker seeker = new TieredSeeker(TARGET_PATH, DB_PATH) {
            @Override protected boolean isTarget(Entity entity) {
                return ((entity instanceof Relation &&
                         (TagUtils.hasTag(entity, "route", "train")
                          || TagUtils.hasTag(entity, "route", "railway")))
                        || (entity instanceof Way
                            && TagUtils.hasTag(entity, "railway")));
            }
        };
        seeker.run();
        final Collection<RelationStub> routes = seeker.getRelations();
        System.out.println("Building graph from " + routes.size() + " routes");
        OSMGraph graph = new OSMGraph(graphDb);

        long nodeCount;
        long edgeCount;
        // Check that the graph has been built properly
        // Quick stats
        nodeCount = getNodeCount(graphDb);
        edgeCount = getEdgeCount(graphDb);

        if (edgeCount < 2000 || nodeCount < 4000) {
            graph.build(seeker.getIndex());
        }

        nodeCount = getNodeCount(graphDb);
        edgeCount = getEdgeCount(graphDb);

        System.out.println("Graph built: " + nodeCount + " nodes & " + edgeCount + " links");

        // Dump out geojson
        final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Station");
        builder.add("the_geom", Point.class);
        builder.add("name", String.class);
        builder.add("name:en", String.class);
        builder.add("railway", String.class);

        // build the type
        final SimpleFeatureType stationType = builder.buildFeatureType();
        SimpleFeatureBuilder stationFeatureBuilder = new SimpleFeatureBuilder(stationType);

        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Link");
        rBuilder.add("the_geom", LineString.class);
//        rBuilder.add("railway", String.class);

        // build the type
        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);

        final Map<Long, SimpleFeature> stationFeatures = new HashMap<>();
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {
            final Stream<Relationship> relationStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                                                                     GlobalGraphOperations.at(graphDb)
                                                                                                          .getAllRelationships()
                                                                                                          .iterator(),
                                                                                     Spliterator.DISTINCT),
                                                                             false);

            relationStream.forEach(relationship -> {
//            seeker.getWays().stream().forEach(way -> {

                // Grab the ends
                final NeoNode startNode = new NeoNode(relationship.getStartNode(), graphDb);
                final NeoNode endNode = new NeoNode(relationship.getEndNode(), graphDb);

//                final List<NodeStub> nodes = way.getNodes(seeker.getIndex());
//                final NodeStub startNode = nodes.get(0);
//                final NodeStub endNode = nodes.get(nodes.size() - 1);

                if (!stationFeatures.containsKey(startNode.getId())) {
                    final Point startPoint = geometryFactory.createPoint(new Coordinate(
                            startNode.getLongitude(),
                            startNode.getLatitude()));
                    stationFeatureBuilder.add(startPoint);
                    stationFeatureBuilder.add(startNode.getTag("name"));
                    stationFeatureBuilder.add(startNode.getTag("name:en"));
                    stationFeatureBuilder.add(startNode.getTag("railway"));
                    final SimpleFeature startFeature = stationFeatureBuilder.buildFeature(String.valueOf(startNode.getId()));
                    stationFeatures.put(startNode.getId(), startFeature);
                }

                if (!stationFeatures.containsKey(endNode.getId())) {
                    final Point endPoint = geometryFactory.createPoint(new Coordinate(
                            endNode.getLongitude(),
                            endNode.getLatitude()));
                    stationFeatureBuilder.add(endPoint);
                    stationFeatureBuilder.add(endNode.getTag("name"));
                    stationFeatureBuilder.add(endNode.getTag("name:en"));
                    stationFeatureBuilder.add(endNode.getTag("railway"));
                    final SimpleFeature endFeature = stationFeatureBuilder.buildFeature(String.valueOf(endNode.getId()));
                    stationFeatures.put(endNode.getId(), endFeature);
                }
                // Build the geometry
                final List<Coordinate> coordinateList = Stream.of(startNode, endNode)
                                                              .map(nodeStub -> new Coordinate(
                                                                      nodeStub.getLongitude(),
                                                                      nodeStub.getLatitude()))
                                                              .collect(Collectors.toList());
                final LineString lineString = geometryFactory.createLineString(
                        coordinateList.toArray(new Coordinate[coordinateList.size()]));
                linkFeatureBuilder.add(lineString);
                final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(relationship.getId()));
                linkFeatures.add(linkFeature);
            });
            tx.success();
        }

        try {
            FeatureJSON featureJSON = new FeatureJSON();
            featureJSON.setEncodeFeatureCollectionCRS(false);
            featureJSON.setEncodeFeatureBounds(false);

            final ListFeatureCollection stationCollection = new ListFeatureCollection(stationType,
                                                                                      new ArrayList<>(stationFeatures.values()));
            final ListFeatureCollection linkCollection = new ListFeatureCollection(linkType, linkFeatures);
            featureJSON.writeFeatureCollection(stationCollection, new File(STATION_GEOJSON));
            featureJSON.writeFeatureCollection(linkCollection, new File(LINK_GEOJSON));

        } catch (IOException e) {
            e.printStackTrace();
        }
        graphDb.shutdown();

    }

    private static long getEdgeCount(GraphDatabaseService graphDb) {
        final long count;
        try (Transaction tx = graphDb.beginTx()) {
            count = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                                 GlobalGraphOperations.at(graphDb)
                                                                      .getAllRelationships()
                                                                      .iterator(),
                                                 Spliterator.DISTINCT),
                                         false)
                                 .count();
            tx.success();
        }
        return count;
    }

    private static long getNodeCount(GraphDatabaseService graphDb) {
        final long nodeCount;
        try (Transaction tx = graphDb.beginTx()) {

            nodeCount = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                                                     GlobalGraphOperations.at(graphDb)
                                                                          .getAllNodes()
                                                                          .iterator(),
                                                     Spliterator.DISTINCT),
                                             false)
                                     .count();
            tx.success();
        }
        return nodeCount;
    }
}
