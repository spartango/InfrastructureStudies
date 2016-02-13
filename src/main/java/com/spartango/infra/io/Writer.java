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
import java.util.stream.StreamSupport;

/**
 * Author: spartango
 * Date: 2/3/16
 * Time: 17:02.
 */
public class Writer {
    public static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    public static void writeHistogram(Map<Set<NodeStub>, Set<NodeStub>> histogram, String filePath) {
        final Map<Set<NodeStub>, Double> counted = histogram.entrySet()
                                                            .stream()
                                                            .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                      entry -> (double) entry.getValue()
                                                                                                             .size()));
        writeSegments(counted, filePath);
    }

    public static void writeSegments(Map<Set<NodeStub>, Double> histogram, String filePath) {
        // Feature type definitions
        SimpleFeatureTypeBuilder rBuilder = new SimpleFeatureTypeBuilder();
        rBuilder.setName("Rail Segment");
        rBuilder.add("the_geom", LineString.class);
        rBuilder.add("criticality", Double.class);
//        rBuilder.add("mean_cost", Double.class);

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

            // Add the Criticality
            linkFeatureBuilder.add(lineString);
            linkFeatureBuilder.add(criticality);

            // Add it to the collection
            final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(pair.hashCode()));
            linkFeatures.add(linkFeature);
        });

        // Write the collection
        writeFeature(filePath, linkType, linkFeatures);
    }

    public static void write(NodeStub station,
                             Collection<WeightedPath> paths,
                             String filePath,
                             RailNetwork railNetwork) {
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
            try (Transaction tx = railNetwork.beginGraphTx()) {
                final List<Coordinate> coordinateList =
                        StreamSupport.stream(path.nodes().spliterator(), false)
                                     .map(railNetwork::getGraphNode)
                                     .map(nodeStub -> new Coordinate(nodeStub.getLongitude(), nodeStub.getLatitude()))
                                     .collect(Collectors.toList());
                tx.success();

                final LineString lineString = geometryFactory.createLineString(
                        coordinateList.toArray(new Coordinate[coordinateList.size()]));
                linkFeatureBuilder.add(lineString);
                linkFeatureBuilder.add(path.weight());

                final SimpleFeature linkFeature = linkFeatureBuilder.buildFeature(String.valueOf(path.hashCode()));
                linkFeatures.add(linkFeature);
            }
        });

        writeFeature(filePath, linkType, linkFeatures);
    }

    public static void writeFeature(String filePath, SimpleFeatureType linkType, List<SimpleFeature> linkFeatures) {
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

    public static void writeStations(Collection<NeoNode> stations, String path) {
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

    public static void writeFlow(RailFlow flow, String path) {
        flow.getPaths()
            .forEach((station, paths) -> write(station,
                                               paths,
                                               path + station.getId() + "_path.geojson",
                                               flow.getRailNetwork()));

    }
}
