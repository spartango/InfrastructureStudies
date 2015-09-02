package com.spartango.infra.osm;

import org.mapdb.DB;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 14:37.
 */
public class OSMIndex {

    private DB database;

    // Basic types
    private Map<Long, NodeStub> nodes;
    private Set<Long>           desired;

    public OSMIndex(DB database) {
        this.database = database;
        nodes = database.getTreeMap("nodes");
        desired = database.getTreeSet("desired");

    }

    public List<NodeStub> getNodes(Way target) {
        return target.getWayNodes()
                     .stream()
                     .filter(wayNode -> nodes.containsKey(wayNode.getNodeId()))
                     .map(wayNode -> nodes.get(wayNode.getNodeId()))
                     .collect(Collectors.toList());
    }

    public void addDesiredNode(long id) {
        if (!nodes.containsKey(id)) {
            desired.add(id);
            database.commit();
        }
    }

    public void addNode(Node target) {
        final long id = target.getId();
        nodes.put(id, new NodeStub(target));
        desired.remove(id);
        database.commit();
    }

    public Map<Long, NodeStub> getNodes() {
        return nodes;
    }

    public Set<Long> getDesired() {
        return desired;
    }

    public boolean isDesired(Node target) {
        return desired.contains(target.getId());
    }

}
