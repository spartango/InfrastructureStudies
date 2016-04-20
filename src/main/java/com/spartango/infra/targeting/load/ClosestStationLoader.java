package com.spartango.infra.targeting.load;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.network.RailNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.spartango.infra.utils.ShapeUtils.calculateDistance;
import static java.util.Comparator.comparingDouble;

/**
 * Author: spartango
 * Date: 2/19/16
 * Time: 10:59.
 */
public class ClosestStationLoader implements NodeLoader {
    public static final long TEMP_ID = -1L;
    private List<NodeStub> points;
    private RailNetwork    railNetwork;

    public ClosestStationLoader(RailNetwork railNetwork) {
        this(new ArrayList<>(), railNetwork);
    }

    public ClosestStationLoader(NodeLoader loader, RailNetwork railNetwork) {
        this(loader.loadNodes(), railNetwork);
    }

    public ClosestStationLoader(List<NodeStub> points, RailNetwork railNetwork) {
        this.points = points;
        this.railNetwork = railNetwork;
    }

    public void addPoint(NodeStub point) {
        points.add(point);
    }

    public void addPoint(double latitude, double longitude) {
        // Temporary point
        points.add(new NodeStub(TEMP_ID, latitude, longitude));
    }

    @Override public List<NodeStub> loadNodes() {
        // Find the nearest point in the set of stations
        return points.parallelStream()
                     .map(target -> railNetwork.getStations()
                                               .stream()
                                               .sorted(comparingDouble(station -> calculateDistance(target, station)))
                                               .findFirst())
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .distinct()
                     .collect(Collectors.toList());
    }

    @Override public List<NeoNode> loadGraphNodes() {
        return points.parallelStream()
                     .map(target -> railNetwork.getStations()
                                               .stream()
                                               .sorted(comparingDouble(station -> calculateDistance(target, station)))
                                               .limit(20)
                                               .map(railNetwork::getGraphNode)
                                               .filter(Optional::isPresent)
                                               .map(Optional::get)
                                               .findFirst())
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .distinct()
                     .collect(Collectors.toList());
    }
}
