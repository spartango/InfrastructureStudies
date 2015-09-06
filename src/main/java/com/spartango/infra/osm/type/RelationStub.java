package com.spartango.infra.osm.type;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 11:53.
 */
public class RelationStub extends EntityStub implements Serializable {

    private final List<Long> nodeIds;
    private final List<Long> wayIds;

    private List<NodeStub> nodes;
    private List<WayStub>  ways;

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
        nodes = new LinkedList<>();
        ways = new LinkedList<>();
    }

    public RelationStub(long id,
                        Map<String, String> tags,
                        List<NodeStub> nodes, List<WayStub> ways) {
        super(id, tags);
        this.nodes = nodes;
        nodeIds = new LinkedList<>();
        nodeIds.addAll(nodes.stream().map(EntityStub::getId).collect(Collectors.toList()));
        this.ways = ways;
        wayIds = new LinkedList<>();
        wayIds.addAll(ways.stream().map(EntityStub::getId).collect(Collectors.toList()));
    }

    public List<Long> getNodeIds() {
        return nodeIds;
    }

    public List<Long> getWayIds() {
        return wayIds;
    }

    public List<NodeStub> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeStub> nodes) {
        this.nodes = nodes;
    }

    public List<WayStub> getWays() {
        return ways;
    }

    public void setWays(List<WayStub> ways) {
        this.ways = ways;
    }
}
