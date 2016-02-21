package com.spartango.infra.io;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.network.RailFlow;
import com.spartango.infra.targeting.network.RailNetwork;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Transaction;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Author: spartango
 * Date: 2/3/16
 * Time: 17:02.
 */
public class Writer {
    public static final String GEOJSON = ".geojson";

    private GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    private String      rootPath;
    private RailNetwork network;

    public Writer(String rootPath, RailNetwork network) {
        this.rootPath = rootPath;
        this.network = network;
        if (!rootPath.endsWith("/")) {
            this.rootPath += "/";
        }
    }

    public void writeStationNodes(String name, Collection<NeoNode> stations) {
        writeStations(name, stations.stream().map(NeoNode::getOsmNode));
    }

    public void writeStations(String name, Collection<NodeStub> stations) {
        writeStations(name, stations.stream());
    }

    public void writeStations(String name, Stream<NodeStub> stations) {
        String path = rootPath + name + GEOJSON;

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Station");
        builder.add("the_geom", Point.class);
        builder.add("id", Long.class);
        builder.add("name", String.class);
        builder.add("name:en", String.class);
        builder.add("railway", String.class);
        builder.add("elevation", Double.class);

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
            stationFeatureBuilder.add(network.getElevation(startNode).orElse(null));
            final SimpleFeature startFeature = stationFeatureBuilder.buildFeature(String.valueOf(startNode.getId()));
            stationFeatures.add(startFeature);
        });

        writeFeature(path, stationType, stationFeatures);
    }

    public void writeSharedSegments(String name, Map<Set<NodeStub>, Set<NodeStub>> histogram) {
        Map<Set<NodeStub>, Double> counted = histogram.entrySet()
                                                      .stream()
                                                      .collect(Collectors.toMap(
                                                              Map.Entry::getKey,
                                                              entry -> (double) entry.getValue().size()));
        writeHistogram(name, counted);
    }

    public void writeHistogram(String name, Map<Set<NodeStub>, Double> histogram) {
        String filePath = rootPath + name + GEOJSON;

        // Feature type definitions
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Segment");
        rBuilder.add("the_geom", LineString.class);
        rBuilder.add("criticality", Double.class);
//        rBuilder.add("length", Double.class);
        rBuilder.add("elevations", String.class);

        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);

        // Collection of segments
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        // For each segment we've seen
        histogram.forEach((pair, criticality) -> {
            // Create it a line feature
            final List<Coordinate> coordinateList = pair.stream()
                                                        .map(nodeStub -> new Coordinate(nodeStub.getLongitude(),
                                                                                        nodeStub.getLatitude()))
                                                        .collect(Collectors.toList());

            final LineString lineString = geometryFactory.createLineString(
                    coordinateList.toArray(new Coordinate[coordinateList.size()]));

            linkFeatureBuilder.add(lineString);

            // Add the Criticality
            linkFeatureBuilder.add(criticality);

            // Calculate the distance
//            final double distance = ShapeUtils.calculateLength(pair);
//            linkFeatureBuilder.add(distance);

            // Calculate the elevation string
            String elevationString = "[" + pair.stream()
                                               .map(network::getElevation)
                                               .map(ele -> ele.orElse(0.0))
                                               .map(String::valueOf)
                                               .collect(Collectors.joining(",")) + "]";
            linkFeatureBuilder.add(elevationString);

            // Add it to the collection
            final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(pair.hashCode()));
            linkFeatures.add(linkFeature);
        });

        // Write the collection
        writeFeature(filePath, linkType, linkFeatures);
    }

    public void writePaths(String name,
                           Collection<WeightedPath> paths) {
        String filePath = rootPath + name + GEOJSON;

        // Setup schema
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Link");
        rBuilder.add("the_geom", LineString.class);
        rBuilder.add("cost", Double.class);
//        rBuilder.add("length", Double.class);
//        rBuilder.add("elevations", String.class);

        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        paths.forEach(path -> {
            if (path == null) {
                return;
            }
            // Build the geometry
            linkFeatures.add(buildGeometry(linkFeatureBuilder, path));
        });

        writeFeature(filePath, linkType, linkFeatures);
    }

    public void writeFlow(String name, RailFlow flow) {
        String filePath = rootPath + name + GEOJSON;

        // Setup schema
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Link");
        rBuilder.add("the_geom", LineString.class);
        rBuilder.add("cost", Double.class);
//        rBuilder.add("length", Double.class);
//        rBuilder.add("elevations", String.class);

        final SimpleFeatureType linkType = rBuilder.buildFeatureType();
        SimpleFeatureBuilder linkFeatureBuilder = new SimpleFeatureBuilder(linkType);
        final List<SimpleFeature> linkFeatures = new ArrayList<>();

        flow.getPaths()
            .values()
            .stream()
            .flatMap(List::stream)
            .forEach(path -> {
                if (path == null) {
                    return;
                }
                // Build the geometry
                linkFeatures.add(buildGeometry(linkFeatureBuilder, path));
            });

        writeFeature(filePath, linkType, linkFeatures);
    }

    private void writeFeature(String filePath, SimpleFeatureType linkType, List<SimpleFeature> linkFeatures) {
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

    private SimpleFeature buildGeometry(SimpleFeatureBuilder linkFeatureBuilder,
                                        WeightedPath path) {
        try (Transaction tx = network.beginGraphTx()) {
            final List<NeoNode> nodes = StreamSupport.stream(path.nodes().spliterator(), false)
                                                     .map(network::getGraphNode)
                                                     .collect(Collectors.toList());
            tx.success();

            final List<Coordinate> coordinateList = nodes.stream()
                                                         .map(nodeStub -> new Coordinate(nodeStub.getLongitude(),
                                                                                         nodeStub.getLatitude()))
                                                         .collect(Collectors.toList());

            final LineString lineString = geometryFactory.createLineString(
                    coordinateList.toArray(new Coordinate[coordinateList.size()]));
            linkFeatureBuilder.add(lineString);
            linkFeatureBuilder.add(path.weight());

//            // Calculate the distance
//            final double distance = ShapeUtils.calculateLength(nodes);
//            linkFeatureBuilder.add(distance);

            // Calculate the elevation string
//            String elevationString = "[" + nodes.stream()
//                                                .map(network::getElevation)
//                                                .map(ele -> ele.orElse(0.0))
//                                                .map(String::valueOf)
//                                                .collect(Collectors.joining(",")) + "]";
//            linkFeatureBuilder.add(elevationString);

            return linkFeatureBuilder.buildFeature(String.valueOf(path.hashCode()));
        }
    }
}
