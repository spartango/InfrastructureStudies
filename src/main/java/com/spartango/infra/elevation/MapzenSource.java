package com.spartango.infra.elevation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spartango.infra.osm.type.NodeStub;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 2/20/16
 * Time: 16:28.
 */
public class MapzenSource implements ElevationSource {
    public static final String BASE_URL = "http://elevation.mapzen.com/height?json="; //srtm3?lat=50.01&lng=10.2&username=demo&style=full

    private String key;

    public MapzenSource(String key) {
        this.key = key;
    }

    @Override public Optional<Double> getElevation(double latitude, double longitude) {
        String urlString = BASE_URL
                           + "{\"range\":false,\"shape\":[{"
                           + "\"lat\":" + latitude
                           + ",\"lon\":" + longitude
                           + "}"
                           + "]}&api_key=" + key;

        try {
            ObjectMapper mapper = new ObjectMapper();
            final JsonNode json = mapper.readTree(new URL(urlString));
            return Optional.of(json.get("height").get(0).asDouble());// Should only be one
        } catch (Exception e) {
            System.err.println(e.getCause() + ": Failed to get elevation for ("
                               + latitude
                               + ", "
                               + longitude
                               + ") due to"
                               + e.getMessage());
        }

        return Optional.empty();
    }

    public Map<NodeStub, Double> getElevations(List<NodeStub> nodes) {
        String urlString = BASE_URL
                           + "{\"range\":false,\"shape\":[";

        urlString += nodes.stream()
                          .map(node -> "{"
                                       + "\"lat\":" + node.getLatitude()
                                       + ",\"lon\":" + node.getLongitude()
                                       + "}")
                          .collect(Collectors.joining(","));

        urlString += "]}&api_key=" + key;
        final HashMap<NodeStub, Double> result = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            final JsonNode json = mapper.readTree(new URL(urlString));
            for (int i = 0; i < nodes.size(); i++) {
                try {
                    final double height = json.get("height").get(i).asDouble();
                    final NodeStub nodeStub = nodes.get(i);
                    // Ensure that it's the same point
//                    if (json.get("shape").get(i).get("lat").asDouble() == nodeStub.getLatitude()
//                        && json.get("shape").get(i).get("lon").asDouble() == nodeStub.getLongitude()) {
                    result.put(nodeStub, height);
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
