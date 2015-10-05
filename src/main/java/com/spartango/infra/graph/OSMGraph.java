package com.spartango.infra.graph;

import com.spartango.infra.graph.types.NeoNode;
import com.spartango.infra.osm.OSMIndex;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.osm.type.RelationStub;
import com.spartango.infra.osm.type.WayStub;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.RelationshipType;

import java.util.*;
import java.util.stream.Collectors;

import static com.spartango.infra.geom.ShapeUtils.calculateDistance;
import static com.spartango.infra.graph.OSMGraph.RelTypes.RAIL_LINK;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 17:24.
 */


public class OSMGraph {

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
        public final WayStub origin;

        public ScanContext(WayStub target, NeoNode lastStation, WayStub origin) {
            this.lastStation = lastStation;
            this.target = target;
            this.origin = origin;
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
        index.getRelations()
             .values()
             .forEach(route -> {
                 // Pull up the graph nodes for the route
                 final Set<NeoNode> neoStops = retrieveStations(index, route);
                 final Set<WayStub> ways = new HashSet<>(route.getWays(index));
                 System.out.println("Scanning route: "
                                    + route.getId() + " -> "
                                    + route.getTags()
                                    + " w/ "
                                    + ways.size()
                                    + " ways and "
                                    + neoStops.size()
                                    + " stops");

                 // Reachability
//                 linkReachable(neoStops);
                 linkAdjacent(index, neoStops, ways);
             });
    }

    private void linkAdjacent(OSMIndex index, Set<NeoNode> neoStops, Set<WayStub> ways) {
        // Find the containing ways & adjacents
        Set<ScanContext> checked = new HashSet<>();
        final Stack<ScanContext> targets = new Stack<>();

        // Seed with known ways and no priors
        ways.forEach(way -> targets.push(new ScanContext(way, null, way)));

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
                                                      entry.origin,
                                                      neoStops,
                                                      index,
                                                      graphDb);
            // Save the new targets, but only the ones we've not done before.
            newTargets.stream()
                      .filter(target -> !checked.contains(target))
                      .forEach(targets::push);
        }
        System.out.print("Scanning complete\r");
    }

    private Set<NeoNode> retrieveStations(OSMIndex index, RelationStub route) {
        // Get all database nodes that are part of this route

        return route.getNodes(index)
                    .stream()
                    .map(stop -> NeoNode.getNeoNode(stop.getId(), graphDb))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
    }

    private void buildStations(OSMIndex index) {
        // For explicitly declared stations on routes
        index.getRelations()
             .values()
             .stream()
             .flatMap(route -> {
                 final Collection<NodeStub> stations = route.getNodes(index);
                 return stations.stream()
                                .peek(station -> {
                                    // Check that this station is on a way
                                    final Optional<WayStub> adjoinedWay = index.getWays()
                                                                               .values()
                                                                               .stream()
                                                                               .filter(way -> way.contains(station))
                                                                               .findAny();
                                    if (!adjoinedWay.isPresent()) {
                                        // If we're not on a way, find the nearest node and put ourselves on it
                                        System.out.print("Finding closest node to " + station.getId()
                                                         + " -> " + station.getTags());
                                        Optional<NodeStub> closest = route.getWays(index)
                                                                          .stream()
                                                                          .flatMap(way -> way.getNodes(index)
                                                                                             .stream())
                                                                          .sorted(Comparator.comparingDouble(
                                                                                  node -> calculateDistance(
                                                                                          station,
                                                                                          node)))
                                                                          .findFirst();
                                        if (!closest.isPresent()) {
                                            // Check if there's a nearby way in ALL the ways
                                            closest = index.getWays()
                                                           .values()
                                                           .stream()
                                                           .flatMap(way -> way.getNodes(index)
                                                                              .stream())
                                                           .sorted(Comparator.comparingDouble(
                                                                   node -> calculateDistance(
                                                                           station,
                                                                           node)))
                                                           .findFirst();
                                        }

                                        if (closest.isPresent()) {
                                            final NodeStub close = closest.get();
                                            // Make a way between the station and this target
                                            long id;
                                            while (index.hasWay(id = (long) (Math.random() * Long.MAX_VALUE))) {
                                                System.out.println("Way ID generation clash!");
                                            }

                                            Map<String, String> tags = new HashMap<>();
                                            tags.put("virtual", "station link");
                                            tags.put("railway", "rail");
                                            WayStub newWay = new WayStub(id,
                                                                         tags,
                                                                         Arrays.asList(close, station, close));
                                            index.addWay(route, newWay);
                                            System.out.println(" => " + newWay);
                                        } else {
                                            System.out.println(" => Nothing found");
                                        }
                                    }
                                });
             })
             .distinct()
             .forEach(nodeStub -> new NeoNode(nodeStub, graphDb));

        // TODO: Search for undeclared stations
        // TODO: Attach unattached stations
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
     * @param index
     * @param graphDb
     * @return targets, consisting of adjacent edges and the station we want to link
     */
    private List<ScanContext> scan(WayStub target,
                                   NeoNode lastStation,
                                   WayStub origin,
                                   Set<NeoNode> stations,
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

                // Purge the list of pendings, as we've found a new station they're close to
                for (final WayStub wayStub : pending) {
                    adjacents.add(new ScanContext(wayStub, currentStation, target));
                }
                pending.clear();
            }

            // if the node is on another way, find those ways, ignoring this
            final List<WayStub> newWays = index.getWays()
                                               .values()
                                               .stream()
                                               .filter(way -> !target.equals(way) && !origin.equals(way) && way.contains(node))
                                               .collect(Collectors.toList());
            // Target adjacent ways with the current station
            if (currentStation != lastStation) {
                for (final WayStub newWay : newWays) {
                    adjacents.add(new ScanContext(newWay, currentStation, target));
                }
            } else {
                // If we haven't found a new station on this leg, we'll want to check in again once we find one
                pending.addAll(newWays);
            }
        }

        // Purge the list of pendings
        for (final WayStub wayStub : pending) {
            // These are places to search with the previous station
            adjacents.add(new ScanContext(wayStub, currentStation, target));
        }
        pending.clear();

        return adjacents;
    }

}
