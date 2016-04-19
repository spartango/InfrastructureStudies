package com.spartango.infra.targeting.load;

import com.irislabs.sheet.Sheet;
import com.spartango.infra.core.graph.NeoNode;
import com.spartango.infra.osm.type.NodeStub;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 4/18/16
 * Time: 21:34.
 */
public class SheetLoader implements NodeLoader {
    private Sheet sheet;

    public SheetLoader(Sheet sheet) {
        this.sheet = sheet;
    }

    @Override public List<NodeStub> loadNodes() {
        // Builds up nodestubs from latlng, but doesn't include database IDs.
        return sheet.stream()
                    .map(row -> {
                        final Optional<Double> latitude = row.getDoubleOption("Latitude");
                        final Optional<Double> longitude = row.getDoubleOption("Longitude");
                        String primary = row.getPrimary();

                        if (latitude.isPresent() && longitude.isPresent()) {
                            return Optional.of(new NodeStub(primary.hashCode(), longitude.get(), latitude.get()));
                        } else {
                            return Optional.empty();
                        }

                    }).filter(Optional::isPresent)
                    .map(o -> (NodeStub) o.get())
                    .collect(Collectors.toList());
    }

    @Override public List<NeoNode> loadGraphNodes() {
        // None of these nodes will be in the graph, it doesnt make sense to return anything here.
        throw new UnsupportedOperationException("Cannot get graph nodes from a spreadsheet");
    }
}
