package com.spartango.infra.osm.type;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 9/6/15
 * Time: 11:55.
 */
public class EntityStub implements Serializable {
    protected final long                id;
    protected final Map<String, String> tags;

    public EntityStub(long id) {
        this.id = id;
        tags = new LinkedHashMap<>();
    }

    public EntityStub(long id, Map<String, String> tags) {
        this.id = id;
        this.tags = tags;
    }

    public EntityStub(long id, Collection<Tag> tagSet) {
        this(id, tagsToMap(tagSet));
    }

    public long getId() {
        return id;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public String getTag(Object key) {
        return tags.get(key);
    }

    public String putTag(Tag tag) {
        return putTag(tag.getKey(), tag.getValue());
    }

    public String putTag(String key, String value) {
        return tags.put(key, value);
    }

    public void putAllTags(Collection<Tag> tags) {
        putAllTags(tagsToMap(tags));
    }

    public void putAllTags(Map<? extends String, ? extends String> m) {
        tags.putAll(m);
    }

    public static Map<String, String> tagsToMap(Collection<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EntityStub that = (EntityStub) o;

        return (id != that.id);
    }

    @Override public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + tags.hashCode();
        return result;
    }

    @Override public String toString() {
        return "EntityStub{" +
               "id=" + id +
               ", tags=" + tags +
               '}';
    }
}
