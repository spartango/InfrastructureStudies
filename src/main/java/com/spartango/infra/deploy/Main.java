package com.spartango.infra.deploy;

import com.irislabs.sheet.SheetEntry;
import com.irislabs.sheet.SheetWriter;
import com.spartango.infra.framework.WaySeeker;
import com.spartango.infra.geom.ShapeUtils;
import com.spartango.infra.osm.NodeStub;
import com.spartango.infra.osm.TagUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 14:27.
 */
public class Main {

    private static final String TARGET_PATH = "data/china-latest.osm.pbf";
    private static final String DB_PATH     = "data/nodes.db";
    private static final String OUTPUT_PATH = "data/results.csv";
    private static final String GEO_PATH    = "data/results.geojson";

    public static void main(String[] args) throws IOException {
        final List<Geometry> geometries = new ArrayList<>();
        final SheetWriter writer = new SheetWriter(new File(OUTPUT_PATH),
                                                   ",",
                                                   Arrays.asList("id",
                                                                 "name",
                                                                 "english name",
                                                                 "start Latitude",
                                                                 "start Longitude",
                                                                 "end Latitude",
                                                                 "end Longitude",
                                                                 "Length (m)",
                                                                 "gauge",
                                                                 "usage",
                                                                 "maxspeed",
                                                                 "electrified",
                                                                 "voltage",
                                                                 "frequency",
                                                                 "highspeed",
                                                                 "bridge",
                                                                 "operator"));

        WaySeeker seeker = new WaySeeker(TARGET_PATH, DB_PATH) {
            @Override protected boolean isTarget(Way entity) {
                boolean rail = TagUtils.hasTag(entity, "railway");
                boolean bridge = TagUtils.hasTag(entity, "bridge");
                boolean tunnel = TagUtils.hasTag(entity, "tunnel");
                return rail && bridge;
            }

            @Override protected void onWayFound(Way entity, List<NodeStub> path) {
                SheetEntry entry = new SheetEntry();
                entry.put("id", entity.getId());
                Optional<String> name = TagUtils.getTag(entity, "name");
                entry.put("name", name.orElse("Unnamed"));
                Optional<String> nameEn = TagUtils.getTag(entity, "name:en");
                entry.put("english name", nameEn.orElse("Unnamed"));

                // Build the geometry
                final List<Coordinate> coordinateList = path.stream()
                                                            .map(nodeStub -> new Coordinate(nodeStub.getLongitude(),
                                                                                            nodeStub.getLatitude()))
                                                            .collect(Collectors.toList());
                if (coordinateList.size() >= 2) {
                    geometries.add(JTSFactoryFinder.getGeometryFactory()
                                                   .createLineString(coordinateList.toArray(
                                                           new Coordinate[coordinateList.size()])));

                } else {
                    System.err.println("Invalid bridge with too few points: " + path);
                }

                // Grab start and end
                path.stream().findFirst().ifPresent(node -> {
                    entry.put("start Latitude", node.getLatitude());
                    entry.put("start Longitude", node.getLongitude());
                });
                path.stream().skip(path.size() - 1).findFirst().ifPresent(node -> {
                    entry.put("end Latitude", node.getLatitude());
                    entry.put("end Longitude", node.getLongitude());
                });

                // Calculate the length
                final double length = ShapeUtils.calculateLength(path);
                entry.put("Length (m)", length);
                entity.getTags().forEach(tag -> entry.put(tag.getKey(), tag.getValue()));

                // Write this out
                try {
                    writer.write(entry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            @Override protected void onSearchComplete() {
                System.out.println("Writing geojson");
                GeometryJSON geometryJSON = new GeometryJSON();

                try {
                    writer.close();
                    geometryJSON.write(new GeometryCollection(geometries.toArray(new Geometry[geometries.size()]),
                                                              JTSFactoryFinder.getGeometryFactory()),
                                       new File(GEO_PATH));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        seeker.run();
    }
}
