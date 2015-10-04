package com.spartango.infra.osm.type;

import com.spartango.infra.osm.OSMIndex;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 11:52.
 */
public class WayStub extends EntityStub implements Serializable {
    protected final List<Long>     nodeIds;
    protected       List<NodeStub> nodes;

    public WayStub(long id, Map<String, String> tags, List<NodeStub> nodes) {
        super(id, tags);
        this.nodes = nodes;
        this.nodeIds = nodes.stream().map(NodeStub::getId).collect(Collectors.toList());
    }

    public WayStub(Way target) {
        super(target.getId(), target.getTags());
        nodeIds = target.getWayNodes().stream().map(WayNode::getNodeId).collect(Collectors.toList());
        nodes = new ArrayList<>();
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public void setNodes(List<NodeStub> nodes) {
        this.nodes = nodes;
    }

    public List<NodeStub> getNodes(OSMIndex index) {
        if (nodes.size() != nodeIds.size()) {
            nodes = nodeIds.stream()
                           .filter(index::hasNode)
                           .map(index::getNode)
                           .collect(Collectors.toList());
        }
        return nodes;
    }

    public List<NodeStub> getNodes() {
        return nodes;
    }

    public boolean contains(NodeStub o) {
        return getNodeIds().contains(o.getId());
    }

    @Override public String toString() {
        return "WayStub{" +
               "id=" + id +
               "tags=" + tags +
               "nodeIds=" + nodeIds +
               ", nodes=" + nodes +
               '}';
    }
}
