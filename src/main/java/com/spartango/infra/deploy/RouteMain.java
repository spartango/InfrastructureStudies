package com.spartango.infra.deploy;

import com.spartango.infra.framework.TieredSeeker;
import com.spartango.infra.osm.TagUtils;
import com.spartango.infra.osm.type.RelationStub;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.Point;
import org.geotools.factory.Hints;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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

    public static void main(String[] args) throws IOException {
        // Seeker
        TieredSeeker seeker = new TieredSeeker(TARGET_PATH, DB_PATH) {
            @Override protected boolean isTarget(Entity entity) {
                return entity instanceof Relation &&
                       (TagUtils.hasTag(entity, "route", "train")
                        || TagUtils.hasTag(entity, "route", "railway"));
            }
        };
        seeker.run();

        final Collection<RelationStub> routes = seeker.getRelations();
        // Grab the stops
        final List<Point> geometries = routes.stream()
                                             .filter(route -> route.getNodes() != null)
                                             .peek(route -> System.out.println("Finding stops for " + route))
                                             .flatMap(route -> route.getNodes().stream())
                                             .map(node -> JTSFactoryFinder.getGeometryFactory()
                                                                          .createPoint(new Coordinate(node.getLongitude(),
                                                                                                      node.getLatitude())))
                                             .collect(Collectors.toList());

        System.out.println("Writing geojson for " + routes.size() + " relations and " + geometries.size() + " stops");
        GeometryJSON geometryJSON = new GeometryJSON();

        try {
            geometryJSON.write(new GeometryCollection(geometries.toArray(new Geometry[geometries.size()]),
                                                      JTSFactoryFinder.getGeometryFactory(new Hints(Hints.CRS,
                                                                                                    DefaultGeographicCRS.WGS84))),
                               new File(GEO_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
