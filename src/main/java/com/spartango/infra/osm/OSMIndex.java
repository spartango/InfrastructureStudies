package com.spartango.infra.osm;

import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.RelationStub;
import com.spartango.infra.osm.type.WayStub;
import org.mapdb.DB;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.Collection;
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
    private Map<Long, NodeStub>     nodes;
    private Map<Long, WayStub>      ways;
    private Map<Long, RelationStub> relations;
    private Set<Long>               desiredNodes;
    private Set<Long>               desiredWays;
    private Set<Long>               desiredRelations;

    public OSMIndex(DB database) {
        this.database = database;
        nodes = database.getTreeMap("nodes");
        relations = database.getTreeMap("relations");
        ways = database.getTreeMap("ways");
        desiredNodes = database.getTreeSet("desiredNodes");
        desiredWays = database.getTreeSet("desiredWays");
        desiredRelations = database.getTreeSet("desiredRelations");
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
            desiredNodes.add(id);
            database.commit();
        }
    }

    public void addDesiredWay(long id) {
        if (!ways.containsKey(id)) {
            desiredWays.add(id);
            database.commit();
        }
    }

    public void addDesiredRelation(long id) {
        if (!relations.containsKey(id)) {
            desiredRelations.add(id);
            database.commit();
        }
    }

    public void addEntity(Entity entity) {
        if (entity instanceof Node) {
            addNode((Node) entity);
        } else if (entity instanceof Way) {
            addWay((Way) entity);
        } else if (entity instanceof Relation) {
            addRelation((Relation) entity);
        }
    }

    public void addNode(Node target) {
        final long id = target.getId();
        nodes.putIfAbsent(id, new NodeStub(target));
        desiredNodes.remove(id);
        database.commit();
    }

    public void addWay(Way target) {
        final long id = target.getId();
        ways.putIfAbsent(id, new WayStub(target));
        desiredWays.remove(id);
        database.commit();
    }

    public void addRelation(Relation target) {
        final long id = target.getId();
        relations.putIfAbsent(id, new RelationStub(target));
        desiredRelations.remove(id);
        database.commit();
    }

    public void populateWay(WayStub target) {
        final List<NodeStub> wayNodes = target.getNodeIds()
                                              .stream()
                                              .filter(this.nodes::containsKey)
                                              .map(this.nodes::get)
                                              .collect(Collectors.toList());
        target.setNodes(wayNodes);
        database.commit();
    }

    public void populateRelation(RelationStub target) {
        final List<NodeStub> relationNodes = target.getNodeIds()
                                                   .stream()
                                                   .filter(this.nodes::containsKey)
                                                   .map(this.nodes::get)
                                                   .collect(Collectors.toList());
        target.setNodes(relationNodes);
        final List<WayStub> relationWays = target.getWayIds()
                                                 .stream()
                                                 .filter(this.ways::containsKey)
                                                 .map(this.ways::get)
                                                 .collect(Collectors.toList());
        target.setWays(relationWays);
        database.commit();
    }

    public Collection<WayStub> waysContaining(NodeStub target) {
        return ways.values()
                   .stream()
                   .filter(way -> way.getNodeIds().contains(target.getId()))
                   .collect(Collectors.toList());
    }

    public Collection<WayStub> waysIntersecting(WayStub target) {
        return ways.values()
                   .stream()
                   .filter(way -> target.getNodeIds()
                                        .stream()
                                        .filter(id -> way.getNodeIds().contains(id))
                                        .findAny()
                                        .isPresent())
                   .collect(Collectors.toList());
    }


    public Map<Long, RelationStub> getRelations() {
        return relations;
    }

    public Map<Long, WayStub> getWays() {
        return ways;
    }

    public Map<Long, NodeStub> getNodes() {
        return nodes;
    }

    public Set<Long> getDesiredNodes() {
        return desiredNodes;
    }

    public Set<Long> getDesiredRelations() {
        return desiredRelations;
    }

    public Set<Long> getDesiredWays() {
        return desiredWays;
    }

    public boolean isDesired(Entity target) {
        if (target instanceof Node) {
            return desiredNodes.contains(target.getId());
        } else if (target instanceof Way) {
            return desiredWays.contains(target.getId());
        } else if (target instanceof Relation) {
            return desiredRelations.contains(target.getId());
        } else {
            return false;
        }

    }

}
