package com.spartango.infra.osm.type;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 11:52.
 */
public class WayStub extends EntityStub implements Serializable {
    private final List<Long>     nodeIds;
    private       List<NodeStub> nodes;

    public WayStub(long id, Map<String, String> tags, List<NodeStub> nodes) {
        super(id, tags);
        this.nodes = nodes;
        this.nodeIds = nodes.stream().map(NodeStub::getId).collect(Collectors.toList());
    }

    public WayStub(Way target) {
        super(target.getId(), target.getTags());
        nodeIds = target.getWayNodes().stream().map(WayNode::getNodeId).collect(Collectors.toList());
        nodes = new LinkedList<>();
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public List<NodeStub> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeStub> nodes) {
        this.nodes = nodes;
    }

    public boolean contains(NodeStub o) {
        return getNodeIds().contains(o.getId());
    }
}
