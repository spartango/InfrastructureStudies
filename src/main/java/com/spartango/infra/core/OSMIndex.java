package com.spartango.infra.core;

import com.spartango.infra.osm.type.EntityStub;
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
import java.util.stream.Stream;

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

    public void commit() {
        database.commit();
    }

    public List<NodeStub> getNodesById(Way target) {
        return target.getWayNodes()
                     .stream()
                     .filter(wayNode -> nodes.containsKey(wayNode.getNodeId()))
                     .map(wayNode -> nodes.get(wayNode.getNodeId()))
                     .collect(Collectors.toList());
    }

    public void addDesiredNode(long id) {
        if (!nodes.containsKey(id) && !desiredNodes.contains(id)) {
            desiredNodes.add(id);
            database.commit();
        }
    }

    public void addDesiredWay(long id) {
        if (!ways.containsKey(id) && !desiredWays.contains(id)) {
            desiredWays.add(id);
            database.commit();
        }
    }

    public void addDesiredRelation(long id) {
        if (!relations.containsKey(id) && !desiredRelations.contains(id)) {
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

    public void addWay(WayStub target) {
        final long id = target.getId();
        ways.putIfAbsent(id, target);
        desiredWays.remove(id);
        database.commit();
    }

    public void addWay(RelationStub parent, WayStub target) {
        addWay(target);
        relations.get(parent.getId()).addWay(target);
        database.commit();
    }

    public void addNode(NodeStub target) {
        final long id = target.getId();
        nodes.putIfAbsent(id, target);
        desiredNodes.remove(id);
        database.commit();
    }

    public void updateNode(NodeStub target) {
        final long id = target.getId();
        nodes.put(id, target);
        desiredNodes.remove(id);
        database.commit();
    }

    public void addRelation(Relation target) {
        final long id = target.getId();
        relations.putIfAbsent(id, new RelationStub(target));
        desiredRelations.remove(id);
        database.commit();
    }

    public void populateWays() {
        ways.forEach((id, target) -> target.setNodes(target.getNodeIds()
                                                           .stream()
                                                           .filter(this.nodes::containsKey)
                                                           .map(this.nodes::get)
                                                           .collect(Collectors.toList())));
        database.commit();
    }

    public void populateRelations() {
        relations.forEach((id, target) -> target.setNodes(target.getNodeIds()
                                                                .stream()
                                                                .filter(this.nodes::containsKey)
                                                                .map(this.nodes::get)
                                                                .collect(Collectors.toList())));
        relations.forEach((id, target) -> target.setWays(target.getWayIds()
                                                               .stream()
                                                               .filter(this.ways::containsKey)
                                                               .map(this.ways::get)
                                                               .collect(Collectors.toList())));
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


    public Map<Long, RelationStub> getRelationsById() {
        return relations;
    }

    public Collection<RelationStub> getRelations() {
        return relations.values();
    }

    public Map<Long, WayStub> getWaysById() {
        return ways;
    }

    public Collection<WayStub> getWays() {
        return ways.values();
    }

    public WayStub getWay(long id) {
        return ways.get(id);
    }

    public boolean hasNode(long id) {
        return nodes.containsKey(id);
    }

    public boolean hasRelation(long id) {
        return relations.containsKey(id);
    }

    public boolean hasWay(long id) {
        return ways.containsKey(id);
    }

    public NodeStub getNode(long id) {
        return nodes.get(id);
    }

    public RelationStub getRelation(long id) {
        return relations.get(id);
    }

    public Map<Long, NodeStub> getNodesById() {
        return nodes;
    }

    public Collection<NodeStub> getNodes() {
        return nodes.values();
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

    public Stream<NodeStub> streamNodesForTag(String key, String value) {
        return filterByTag(getNodes(), key, value);
    }

    public Stream<NodeStub> streamNodesForTag(String key) {
        return filterByTag(getNodes(), key);
    }

    public Stream<WayStub> streamWaysForTag(String key, String value) {
        return filterByTag(getWays(), key, value);

    }

    public Stream<WayStub> streamWaysForTag(String key) {
        return filterByTag(getWays(), key);
    }

    public Stream<RelationStub> streamRelationsForTag(String key, String value) {
        return filterByTag(getRelations(), key, value);

    }

    public Stream<RelationStub> streamRelationsForTag(String key) {
        return filterByTag(getRelations(), key);
    }

    public static <T extends EntityStub> Stream<T> filterByTag(Collection<T> entities, String key) {
        return entities.stream()
                       .filter(entity -> entity.getTags().containsKey(key));
    }

    public static <T extends EntityStub> Stream<T> filterByTag(Collection<T> entities, String key, String value) {
        return entities.stream()
                       .filter(entity -> entity.getTags().containsKey(key)
                                         && entity.getTag(key).equals(value));
    }

}
