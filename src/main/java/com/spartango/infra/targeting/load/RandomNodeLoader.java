package com.spartango.infra.targeting.load;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.network.RailNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/12/16
 * Time: 19:20.
 */
public class RandomNodeLoader implements NodeLoader {

    public static final int DEFAULT_COUNT = 20;

    private long        count;
    private RailNetwork network;

    public RandomNodeLoader(RailNetwork network) {
        this(DEFAULT_COUNT, network);
    }

    public RandomNodeLoader(long count, RailNetwork network) {
        this.count = count;
        this.network = network;
    }

    public RandomNodeLoader setCount(long count) {
        this.count = count;
        return this;
    }

    @Override public List<NeoNode> loadGraphNodes() {
        // Make a copy so we can randomize the order
        final List<NodeStub> shuffled = new ArrayList<>(network.getStations());
        Collections.shuffle(shuffled);
        return shuffled.parallelStream()
                       .map(station -> network.getGraphNode(station.getId()))
                       .filter(Optional::isPresent)
                       .limit(count)
                       .map(Optional::get)
                       .collect(Collectors.toList());
    }

    @Override public List<NodeStub> loadNodes() {
        // Make a copy so we can randomize the order
        final List<NodeStub> shuffled = new ArrayList<>(network.getStations());
        Collections.shuffle(shuffled);
        return shuffled.parallelStream()
                       .map(station -> network.getNode(station.getId()))
                       .filter(Optional::isPresent)
                       .limit(count)
                       .map(Optional::get)
                       .collect(Collectors.toList());
    }
}
