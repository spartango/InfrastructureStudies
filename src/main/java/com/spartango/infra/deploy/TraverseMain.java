package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.NodeStub;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.GraphDatabaseService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.spartango.infra.geom.ShapeUtils.calculateDistance;
import static org.neo4j.graphalgo.GraphAlgoFactory.aStar;
import static org.neo4j.graphdb.PathExpanders.allTypesAndDirections;

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

    private static GraphDatabaseService graphDb;
    private static TieredSeeker         seeker;
    private static GeometryFactory      geometryFactory;

    public static void main(String[] args) {
        setupGraph();

        // Find all stations
        final List<NeoNode> stations = getStations();
        System.out.println("Seeding with " + stations.size());

        // Look for shortest paths
        final PathFinder<WeightedPath> pathFinder = aStar(allTypesAndDirections(),
                                                          (relationship, direction) -> 1.0d,
                                                          (start, end) -> calculateDistance(new NeoNode(start, graphDb)
                                                                                                    .getOsmNode(),
                                                                                            new NeoNode(end, graphDb)
                                                                                                    .getOsmNode()));

        System.out.println("DEBUG: Limiting paths to one station source");
        stations.stream()
                .limit(1) // TODO: DEBUG plotting just one set for now
                .peek(station -> System.out.println("Finding paths from station: " + station.getOsmNode()))
                .map(station -> stations.stream()
                                        .filter(destination -> !destination.equals(station))
                        .peek(destination -> System.out.print(station.getId()
                                                              + " -> "
                                                              + destination.getId()
                                                              + "\r"))
                        .map(destination -> {
                            final WeightedPath path;
                            try (Transaction tx = graphDb.beginTx()) {
                                path = pathFinder.findSinglePath(station.getNeoNode(),
                                                                 destination.getNeoNode());
                                tx.success();
                            }
                            return path;
                        }))
                .forEach(pathStream -> pathStream.forEach(TraverseMain::write));

        System.out.println("Shutting down");
        graphDb.shutdown();
    }

    private static List<NeoNode> getStations() {
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
        return stations.stream()
                       .map(node -> NeoNode.getNeoNode(node.getId(), graphDb))
                       .filter(Optional::isPresent)
                       .map(Optional::get)
                       .collect(Collectors.toList());
    }

    private static void write(WeightedPath path) {
        // Setup schema
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Link");
        rBuilder.add("the_geom", LineString.class);
        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        try (Transaction tx = graphDb.beginTx()) {
            path.relationships()
                .forEach(relationship -> {
                    final NeoNode startNode = new NeoNode(relationship.getStartNode(), graphDb);
                    final NeoNode endNode = new NeoNode(relationship.getEndNode(), graphDb);

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

            final ListFeatureCollection linkCollection = new ListFeatureCollection(linkType, linkFeatures);
            featureJSON.writeFeatureCollection(linkCollection, new File(PATH + path.hashCode() + "_path.geojson"));
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

}
