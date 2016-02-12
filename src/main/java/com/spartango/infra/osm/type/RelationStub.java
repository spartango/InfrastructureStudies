package com.spartango.infra.osm.type;

import com.spartango.infra.osm.OSMIndex;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 11:53.
 */
public class RelationStub extends EntityStub implements Serializable {

    protected final List<Long> nodeIds;
    protected final List<Long> wayIds;

    protected List<NodeStub> nodes;
    protected List<WayStub>  ways;

    public RelationStub(Relation target) {
        super(target.getId(), target.getTags());
        nodeIds = target.getMembers()
                        .stream()
                        .filter(member -> member.getMemberType() == EntityType.Node)
                        .map(RelationMember::getMemberId).collect(Collectors.toList());
        wayIds = target.getMembers()
                       .stream()
                       .filter(member -> member.getMemberType() == EntityType.Way)
                       .map(RelationMember::getMemberId).collect(Collectors.toList());
        nodes = new ArrayList<>();
        ways = new ArrayList<>();
    }

    public RelationStub(long id,
                        Map<String, String> tags,
                        List<NodeStub> nodes, List<WayStub> ways) {
        super(id, tags);
        this.nodes = nodes;
        nodeIds = new ArrayList<>();
        nodeIds.addAll(nodes.stream().map(EntityStub::getId).collect(Collectors.toList()));
        this.ways = ways;
        wayIds = new ArrayList<>();
        wayIds.addAll(ways.stream().map(EntityStub::getId).collect(Collectors.toList()));
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public List<Long> getWayIds() {
        return wayIds;
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

    public List<WayStub> getWays(OSMIndex index) {
        if (ways.size() != wayIds.size()) {
            ways = wayIds.stream()
                         .filter(index::hasWay)
                         .map(index::getWay)
                         .collect(Collectors.toList());
        }
        return ways;
    }

    public List<NodeStub> getNodes() {
        return nodes;
    }

    public List<WayStub> getWays() {
        return ways;
    }

    public void addNode(NodeStub nodeStub) {
        if (!nodeIds.contains(nodeStub.getId())) {
            nodes.add(nodeStub);
            nodeIds.add(nodeStub.getId());
        }
    }

    public void addWay(WayStub newWay) {
        if (!wayIds.contains(newWay.getId())) {
            ways.add(newWay);
            wayIds.add(newWay.getId());
        }
    }

    public void setNodes(List<NodeStub> nodes) {
        this.nodes = nodes;
    }

    public void setWays(List<WayStub> ways) {
        this.ways = ways;
    }

    @Override public String toString() {
        return "RelationStub{" +
               "id=" + id +
               "tags=" + tags +
               "nodeIds=" + nodeIds +
               ", wayIds=" + wayIds +
               ", nodes=" + nodes +
               ", ways=" + ways +
               '}';
    }


}
