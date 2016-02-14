package com.spartango.infra.core.graph;

import com.spartango.infra.utils.ShapeUtils;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
import org.neo4j.graphdb.*;

import java.util.Map;
import java.util.Optional;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 22:10.
 */
public class NeoNode {

    public static final String OSM_ID = "id";

    public enum OsmLabels implements Label {
        OSM_NODE,
        RAIL_STATION
    }

    protected Node     neoNode;
    protected NodeStub osmNode;

    public NeoNode(Node target, GraphDatabaseService graphDb) {
        this.neoNode = target;
        try (Transaction tx = graphDb.beginTx()) {
            final long id = Long.parseLong(neoNode.getProperty(OSM_ID).toString());
            final double latitude = Double.parseDouble(neoNode.getProperty("Latitude").toString());
            final double longitude = Double.parseDouble(neoNode.getProperty("Longitude").toString());
            this.osmNode = new NodeStub(id, longitude, latitude);
            neoNode.getPropertyKeys().forEach(key -> osmNode.putTag(key, String.valueOf(neoNode.getProperty(key))));
            tx.success();
        }
    }

    public NeoNode(NodeStub osmNode, GraphDatabaseService graphDb) {
        this.osmNode = osmNode;
        try (Transaction tx = graphDb.beginTx()) {
            neoNode = graphDb.createNode(OsmLabels.OSM_NODE);
            osmNode.getTags().forEach(neoNode::setProperty);
            // Force these properties
            neoNode.setProperty("Latitude", String.valueOf(osmNode.getLatitude()));
            neoNode.setProperty("Longitude", String.valueOf(osmNode.getLongitude()));
            neoNode.setProperty(OSM_ID, String.valueOf(osmNode.getId()));
            tx.success();
        }
    }

    public Map<String, String> getTags() {
        return osmNode.getTags();
    }

    public String getTag(Object key) {
        return osmNode.getTag(key);
    }

    public double getLongitude() {
        return osmNode.getLongitude();
    }

    public double getLatitude() {
        return osmNode.getLatitude();
    }

    public long getId() {
        return osmNode.getId();
    }

    public Iterable<Relationship> getRelationships() {
        return neoNode.getRelationships();
    }

    public Relationship createRelationshipTo(NeoNode otherNode, RelationshipType type, GraphDatabaseService graphDb) {
        Relationship relationshipTo;
        try (Transaction tx = graphDb.beginTx()) {
            relationshipTo = neoNode.createRelationshipTo(otherNode.neoNode, type);
            final double distance = ShapeUtils.calculateDistance(osmNode, otherNode.osmNode);
            relationshipTo.setProperty("distance", String.valueOf(distance));
            tx.success();
        }
        return relationshipTo;
    }

    public Relationship createRelationshipTo(NeoNode otherNode,
                                             WayStub way,
                                             RelationshipType type,
                                             GraphDatabaseService graphDb) {
        Relationship relationshipTo;
        try (Transaction tx = graphDb.beginTx()) {
            relationshipTo = neoNode.createRelationshipTo(otherNode.neoNode, type);
            way.getTags().forEach(relationshipTo::setProperty);
//            final double distance = ShapeUtils.calculateLength(way.getNodesById());
//            relationshipTo.setProperty("distance", String.valueOf(distance));
            tx.success();
        }
        return relationshipTo;
    }

    // Hopefully we don't need these later
    public Node getNeoNode() {
        return neoNode;
    }

    public NodeStub getOsmNode() {
        return osmNode;
    }

    public static Optional<NeoNode> getNeoNode(long id, GraphDatabaseService graphDb) {
        Optional<NeoNode> node = Optional.empty();
        Node nNode = null;

        try (Transaction tx = graphDb.beginTx()) {
            nNode = graphDb.findNode(OsmLabels.OSM_NODE, OSM_ID, String.valueOf(id));
            tx.success();
        }

        if (nNode != null) {
            node = Optional.of(new NeoNode(nNode, graphDb));
        }

        return node;
    }

}
