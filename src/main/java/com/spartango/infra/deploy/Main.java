package com.spartango.infra.deploy;

import com.irislabs.sheet.SheetEntry;
import com.irislabs.sheet.SheetWriter;
import com.spartango.infra.osm.NodeStub;
import com.spartango.infra.osm.OSMIndex;
import com.spartango.infra.osm.TagUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import org.geotools.geojson.geom.GeometryJSON;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.GeodeticCalculator;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.pbf2.v0_6.PbfReader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 14:27.
 */
public class Main {

    private static final String TARGET_PATH = "data/china-latest.osm.pbf";
    private static final int    WORKERS     = 1;
    private static final String DB_PATH     = "data/nodes.db";
    private static final String OUTPUT_PATH = "data/results.csv";
    private static final String GEO_PATH    = "data/results.geojson";

    public static void main(String[] args) throws IOException {
        // Build up node index
        DB database = DBMaker.newFileDB(new File(DB_PATH))
                             .mmapFileEnable()
                             .closeOnJvmShutdown()
                             .make();

        OSMIndex index = new OSMIndex(database);

        // First Pass
        PbfReader targetReader = new PbfReader(new File(TARGET_PATH), WORKERS);
        targetReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Way && isRailBridge((Way) entity)) {
                    // Capture the waynodes it wants
                    ((Way) entity).getWayNodes().forEach(node -> index.addDesiredNode(node.getNodeId()));
                    System.out.print("Desired Nodes: " + index.getDesired().size() + "\r");
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting targeting pass");
            }

            @Override public void complete() {
                System.out.println("Targeting pass complete");
            }

            @Override public void release() {
            }
        });
        targetReader.run();

        // Second Pass
        PbfReader nodeReader = new PbfReader(new File(TARGET_PATH), WORKERS);
        nodeReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Node && index.isDesired((Node) entity)) {
                    // Capture the node data
                    index.addNode((Node) entity);
                    System.out.print("Remaining Nodes: " + index.getDesired().size() + "\r");
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting indexing pass");
            }

            @Override public void complete() {
                System.out.println("Indexing pass complete");
            }

            @Override public void release() {
            }
        });
        nodeReader.run();

        // Third Pass
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
//                                                                 "other information"));

        final List<Geometry> geometries = new ArrayList<>();

        PbfReader wayReader = new PbfReader(new File(TARGET_PATH), WORKERS);
        wayReader.setSink(new Sink() {
            @Override public void process(EntityContainer entityContainer) {
                final Entity entity = entityContainer.getEntity();
                if (entity instanceof Way && isRailBridge((Way) entity)) {
                    // Retrieve the path
                    SheetEntry entry = new SheetEntry();
                    entry.put("id", entity.getId());
                    Optional<String> name = TagUtils.getTag(entity, "name");
                    entry.put("name", name.orElse("Unnamed"));
                    Optional<String> nameEn = TagUtils.getTag(entity, "name:en");
                    entry.put("english name", nameEn.orElse("Unnamed"));

                    final List<NodeStub> path = index.getNodes((Way) entity);
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
                    final double length = calculateLength(path);
                    entry.put("Length (m)", length);
                    entity.getTags().forEach(tag -> entry.put(tag.getKey(), tag.getValue()));

                    // Write this out
                    try {
                        writer.write(entry);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override public void initialize(Map<String, Object> metaData) {
                System.out.println("Starting tracing pass");
            }

            @Override public void complete() {
                System.out.println("Tracing pass complete");
                database.close();

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

            @Override public void release() {
            }
        });

        wayReader.run();
    }

    private static double calculateLength(List<NodeStub> path) {
        final Iterator<NodeStub> iterator = path.iterator();
        double sum = 0;
        NodeStub current = iterator.hasNext() ? iterator.next() : null;
        while (current != null && iterator.hasNext()) {
            NodeStub next = iterator.next();
            GeodeticCalculator calculator = new GeodeticCalculator();
            calculator.setStartingGeographicPoint(current.getLongitude(), current.getLatitude());
            calculator.setDestinationGeographicPoint(next.getLongitude(), next.getLatitude());
            double distance = calculator.getOrthodromicDistance();
            sum += distance;
            current = next;
        }
        return sum;
    }

    private static boolean isRailBridge(Way entity) {
        boolean rail = TagUtils.hasTag(entity, "railway");
        boolean bridge = TagUtils.hasTag(entity, "bridge");

        return rail && bridge;
    }

}
