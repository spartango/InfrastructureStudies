package com.spartango.infra.targeting.load;

import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;
import com.spartango.infra.targeting.network.RailNetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/12/16
 * Time: 19:16.
 */
public class NodeIdLoader implements NodeLoader {
    private Collection<Long> ids;
    private RailNetwork      railNetwork;

    public NodeIdLoader(RailNetwork network) {
        railNetwork = network;
        this.ids = new ArrayList<>();
    }

    public NodeIdLoader addIds(Collection<Long> ids) {
        this.ids.addAll(ids);
        return this;
    }

    public NodeIdLoader addId(long id) {
        this.ids.add(id);
        return this;
    }

    public List<NeoNode> loadGraphNodes() {
        return ids.parallelStream()
                  .map(railNetwork::getGraphNode)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(Collectors.toList());
    }

    public List<NodeStub> loadNodes() {
        return ids.parallelStream()
                  .map(railNetwork::getNode)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .collect(Collectors.toList());
    }
}
