package com.spartango.infra.utils;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Author: spartango
 * Date: 9/1/15
 * Time: 16:51.
 */
public class OSMTagUtils {
    public static boolean hasTag(Entity entity, String key, String value) {
        return entity.getTags().stream().filter(tagEquals(key, value)).findAny().isPresent();
    }

    public static boolean hasTag(Entity entity, String key) {
        return entity.getTags().stream().filter(tagEquals(key)).findAny().isPresent();
    }

    public static Optional<String> getTag(Entity entity, String key) {
        return entity.getTags().stream().filter(tagEquals(key)).findFirst().map(Tag::getValue);
    }

    public static Predicate<Tag> tagEquals(String key) {
        return tag -> tag.getKey().equals(key);
    }

    public static Predicate<Tag> tagEquals(String key, String value) {
        return tag -> tag.getKey().equals(key) && tag.getValue().equals(value);
    }
}
