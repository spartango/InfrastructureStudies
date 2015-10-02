package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.RelationStub;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 13:07.
 */
public class RouteMain {

    private static final String TARGET_PATH = "data/china-latest.osm.pbf";
    private static final String DB_PATH     = "data/routes.db";
    private static final String OUTPUT_PATH = "data/routes.csv";
    private static final String GEO_PATH    = "data/routes.geojson";

    public static void main(String[] args) throws IOException, SchemaException {
        // Seeker
        TieredSeeker seeker = new TieredSeeker(TARGET_PATH, DB_PATH) {
            @Override protected boolean isTarget(Entity entity) {
                return (entity instanceof Relation &&
                        (TagUtils.hasTag(entity, "route", "train")
                         || TagUtils.hasTag(entity, "route", "railway")));
            }
        };
        seeker.run();
        final Collection<RelationStub> routes = seeker.getRelations();

        final GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
//        builder.setCRS(DefaultGeographicCRS.WGS84); // <- Coordinate reference system
        builder.add("the_geom", Point.class);
        builder.add("name", String.class);
        builder.add("name:en", String.class);
        builder.add("railway", String.class);

        // build the type
        final SimpleFeatureType type = builder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        final List<SimpleFeature> features = routes.stream()
                                                   .peek(route -> System.out.println("Finding stops for " + route))
                                                   .flatMap(route -> route.getNodes(seeker.getIndex()).stream())
                                                   .map(node -> {
                                                       final Point point = geometryFactory.createPoint(new Coordinate(
                                                               node.getLatitude(),
                                                               node.getLongitude()));
                                                       featureBuilder.add(point);
                                                       featureBuilder.add(node.getTag("name"));
                                                       featureBuilder.add(node.getTag("name:en"));
                                                       featureBuilder.add(node.getTag("railway"));
                                                       return featureBuilder.buildFeature(String.valueOf(node.getId()));
                                                   }).collect(Collectors.toList());

        System.out.println("Writing geojson for " + routes.size() + " relations and " + features.size() + " stops");
        FeatureJSON featureJSON = new FeatureJSON();
        featureJSON.setEncodeFeatureCollectionCRS(false);
        featureJSON.setEncodeFeatureBounds(false);

        final ListFeatureCollection collection = new ListFeatureCollection(type, features);
        featureJSON.writeFeatureCollection(collection, new File(GEO_PATH));
    }
}
