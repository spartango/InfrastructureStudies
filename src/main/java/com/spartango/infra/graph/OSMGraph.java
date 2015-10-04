package com.spartango.infra.graph;

import com.spartango.infra.geom.ShapeUtils;
import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.OSMIndex;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.WayStub;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

import java.util.*;
import java.util.stream.Collectors;

import static com.spartango.infra.graph.OSMGraph.RelTypes.RAIL_LINK;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 17:24.
 */


public class OSMGraph {
    private static final double PROXIMITY_THRESHOLD = 100; // meters

    public enum RelTypes implements RelationshipType {
        NEARBY,
        RAIL_LINK,
        REACHABLE,
        POWER_LINK,
        WATERWAY_LINK,
        ROAD_LINK,
        AIR_LINK
    }

    private class ScanContext {
        public final WayStub target;
        public final NeoNode lastStation;

        public ScanContext(WayStub target, NeoNode lastStation) {
            this.lastStation = lastStation;
            this.target = target;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ScanContext that = (ScanContext) o;

            if (target != null ? !target.equals(that.target) : that.target != null) {
                return false;
            }
            return !(lastStation != null ? !lastStation.equals(that.lastStation) : that.lastStation != null);

        }

        @Override public int hashCode() {
            int result = target != null ? target.hashCode() : 0;
            result = 31 * result + (lastStation != null ? lastStation.hashCode() : 0);
            return result;
        }

        @Override public String toString() {
            return "ScanContext{" +
                   "lastStation=" + lastStation +
                   ", target=" + target +
                   '}';
        }
    }

    private GraphDatabaseService graphDb;

    public OSMGraph(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    public void build(OSMIndex index) {
        // Pull out the stops as nodes, and uniquely enter them into the db
        System.out.println("Building stations...");
        buildStations(index);

        // Attach the appropriate links
        linkStations(index);
    }

    private void linkStations(OSMIndex index) {
        final Set<NeoNode> neoStops = retrieveStations(index);
        final Set<WayStub> ways = new HashSet<>(index.getWays().values());
        linkAdjacent(index, neoStops, ways);
    }

    private void linkAdjacent(OSMIndex index, Set<NeoNode> neoStops, Set<WayStub> ways) {
        // Find the containing ways & adjacents
        Set<ScanContext> checked = new HashSet<>();
        final Stack<ScanContext> targets = new Stack<>();

        // Seed with known ways and no priors
        ways.forEach(way -> targets.push(new ScanContext(way, null)));

        while (!targets.isEmpty()) {
            final ScanContext entry = targets.pop();
            if (checked.contains(entry)) {
                continue;
            } else {
                checked.add(entry);
            }
            System.out.print("Scanning: " + targets.size() + " targets remaining\r");

            // Scan this target
            final List<ScanContext> newTargets = scan(entry.target,
                                                      entry.lastStation,
                                                      neoStops,
                                                      ways,
                                                      index,
                                                      graphDb);
            // Save the new targets, but only the ones we've not done before.
            newTargets.stream()
                      .filter(target -> !checked.contains(target))
                      .forEach(targets::push);
        }
        System.out.print("Scanning complete\r");
    }

    private Set<NeoNode> retrieveStations(OSMIndex index) {
        // Get all database nodes that are stations
        return index.getNodes()
                    .values()
                    .stream()
                    .filter(node -> node.getTag("railway").equals("station"))
                    .map(stop -> NeoNode.getNeoNode(stop.getId(), graphDb))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
    }

    private void buildStations(OSMIndex index) {
        // For explicitly declared stations on ways
        index.getNodes()
             .values()
             .stream()
                .filter(node -> node.getTag("railway").equals("station")) // Extract stations
                .map(station -> {
                    final Optional<WayStub> adjoinedWay = index.getWays()
                                                               .values()
                                                               .stream()
                                                               .filter(way -> way.contains(station))
                                                               .findAny();
                    if (!adjoinedWay.isPresent()) {
                        // If we're not on a way, find the nearest node and put ourselves on it
                        System.out.print("Finding closest node to " + station.getId()
                                         + " -> " + station.getTags() + "\r");
                        final Optional<NodeStub> closest = index.getWays()
                                                                .values()
                                                                .stream()
                                                                .flatMap(way -> way.getNodes(index)
                                                                                   .stream())
                                                                .sorted(Comparator.comparingDouble(
                                                                        node -> ShapeUtils.calculateDistance(
                                                                                station,
                                                                                node)))
                                                                .findFirst();
                        if (closest.isPresent()) {
                            // Tag this as an actual station. Slightly shady
                            final NodeStub close = closest.get();
                            close.putAllTags(station.getTags());
                            return close;
                        }
                    }
                    return station;
                })
                .distinct()
                .forEach(nodeStub -> new NeoNode(nodeStub, graphDb));
    }

    private void linkReachable(Set<NeoNode> neoStops) {
        neoStops.forEach(stop -> neoStops.stream()
                                         .filter(otherStop -> !stop.equals(otherStop))
                                         .forEach(otherStop -> stop.createRelationshipTo(otherStop,
                                                                                         RelTypes.REACHABLE,
                                                                                         graphDb)));
    }

    /**
     * Crazy helper function
     * Trying to connect lastStation to any station in target
     * Trying to connect any station in Target to any other station in target
     *
     * @param target
     * @param lastStation
     * @param stations
     * @param ways
     * @param index
     * @param graphDb
     * @return targets, consisting of adjacent edges and the station we want to link
     */
    private List<ScanContext> scan(WayStub target,
                                   NeoNode lastStation,
                                   Set<NeoNode> stations,
                                   Set<WayStub> ways,
                                   OSMIndex index,
                                   GraphDatabaseService graphDb) {

        List<ScanContext> adjacents = new LinkedList<>();
        List<WayStub> pending = new LinkedList<>();

        NeoNode currentStation = lastStation;
        // walk the target's nodes in order
        for (NodeStub node : target.getNodes(index)) {
            // if node is a station
            Optional<NeoNode> stationCheck = stations.stream()
                                                     .filter(station -> station.getOsmNode().equals(node))
                                                     .findAny();
            if (stationCheck.isPresent()) {
                // link to last station
                final NeoNode station = stationCheck.get();
                if (currentStation != null && !currentStation.equals(station)) {
                    currentStation.createRelationshipTo(station, RAIL_LINK, graphDb);
                }

                // set last station to this one, prior stations are no long adjacent
                currentStation = station;

                // Purge the list of pendings
                for (final WayStub wayStub : pending) {
                    adjacents.add(new ScanContext(wayStub, currentStation));
                }
                pending.clear();
            }

            // if the node is on another way, find those ways, ignoring this and the past one
            final List<WayStub> newWays = ways.stream()
                                              .filter(way -> !target.equals(way))
                                              .filter(way -> way.contains(node)).collect(Collectors.toList());
            if (currentStation != lastStation) {
                for (final WayStub newWay : newWays) {
                    adjacents.add(new ScanContext(newWay, currentStation));
                }
            } else {
                pending.addAll(newWays);
            }
        }

        // Purge the list of pendings
        for (final WayStub wayStub : pending) {
            adjacents.add(new ScanContext(wayStub, currentStation));
        }
        pending.clear();

        return adjacents;
    }

}
