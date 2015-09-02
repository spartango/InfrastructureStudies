package com.spartango.infra.osm;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 17:09.
 */
public class NodeStub implements Serializable {

    private final long                id;
    private final double              longitude;
    private final double              latitude;
    private final Map<String, String> tags;

    public NodeStub(Node target) {
        this(target.getId(),
             target.getLongitude(),
             target.getLatitude());
    }


    public NodeStub(long id, double longitude, double latitude) {
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
        tags = new LinkedHashMap<>();
    }

    public long getId() {
        return id;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getTag(Object key) {
        return tags.get(key);
    }

    public String putTag(String key, String value) {
        return tags.put(key, value);
    }

    public void putAllTags(Map<? extends String, ? extends String> m) {
        tags.putAll(m);
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NodeStub nodeStub = (NodeStub) o;

        if (id != nodeStub.id) {
            return false;
        }
        if (Double.compare(nodeStub.longitude, longitude) != 0) {
            return false;
        }
        if (Double.compare(nodeStub.latitude, latitude) != 0) {
            return false;
        }
        return tags.equals(nodeStub.tags);

    }

    @Override public int hashCode() {
        int result;
        long temp;
        result = (int) (id ^ (id >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + tags.hashCode();
        return result;
    }

    @Override public String toString() {
        return "NodeStub{" +
               "id=" + id +
               ", longitude=" + longitude +
               ", latitude=" + latitude +
               ", tags=" + tags +
               '}';
    }
}
