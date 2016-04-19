package com.irislabs.sheet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;
import java.util.function.*;

/**
 * Author: spartango
 * Date: 4/14/14
 * Time: 10:00 PM.
 */
public class SheetEntry {
    private LinkedHashMap<String, String> fields;

    public SheetEntry() {
        fields = new LinkedHashMap<>();
    }

    public SheetEntry(Map<String, String> fields) {
        this.fields = new LinkedHashMap<>(fields);
    }

    public SheetEntry(SheetEntry target) {
        this(target.fields);
    }

    public Set<String> fields() {
        return fields.keySet();
    }

    public Collection<String> values() {
        return fields.values();
    }

    public void forEach(BiConsumer<? super String, ? super String> action) {
        fields.forEach(action);
    }

    public String putIfAbsent(String key, String value) {
        return fields.putIfAbsent(key, value);
    }

    public String getPrimary() {
        return fields.values().stream().findFirst().get();
    }

    public Integer getPrimaryInt() {
        return Integer.parseInt(fields.values().stream().findFirst().get());
    }

    public String get(String key) {
        return fields.get(key);
    }

    public Double getDouble(String key) throws NumberFormatException {
        return Double.parseDouble(fields.get(key));
    }

    public Integer getInt(String key) throws NumberFormatException {
        return Integer.parseInt(fields.get(key));
    }

    public String put(String key, String value) {
        return fields.put(key, value);
    }

    public String put(String key, double value) {
        return fields.put(key, String.valueOf(value));
    }

    public String put(String key, int value) {
        return fields.put(key, String.valueOf(value));
    }

    public String remove(String key) {
        return fields.remove(key);
    }

    public void putAll(Map<? extends String, ? extends String> m) {
        fields.putAll(m);
    }

    public void putAllNumbers(Map<? extends String, ? extends Number> m) {
        m.forEach((key, value) -> fields.put(key, String.valueOf(value)));
    }

    public boolean containsField(String key) {
        return fields.containsKey(key) && !fields.get(key).isEmpty();
    }

    public boolean isNumeric(String key) {
        try {
            return fields.containsKey(key) && !fields.get(key).isEmpty() && !Double.isNaN(getDouble(key));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getOrDefault(String key, String defaultValue) {
        return fields.getOrDefault(key, defaultValue);
    }

    public Double getDoubleOrDefault(String key, double defaultValue) {
        try {
            return fields.containsKey(key) ? getDouble(key) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Integer getIntOrDefault(String key, int defaultValue) {
        try {
            return fields.containsKey(key) ? getInt(key) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Optional<Double> getDoubleOption(String key) {
        try {
            return fields.containsKey(key) ? Optional.of(getDouble(key)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<Integer> getIntOption(String key) {
        try {
            return fields.containsKey(key) ? Optional.of(getInt(key)) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return fields.entrySet();
    }

    public SheetEntry append(SheetEntry entry) {
        putAll(entry.fields);
        return this;
    }

    public int getFieldCount() {
        return fields.size();
    }

    public JsonObject toJSON() {
        JsonObject obj = new JsonObject();
        this.forEach(obj::addProperty);
        return obj;
    }

    public String toJSONString() {
        JsonObject obj = new JsonObject();
        this.forEach(obj::addProperty);
        return obj.toString();
    }

    @Override public String toString() {
        return "{" + fields + '}';
    }

    @Override protected SheetEntry clone() {
        final SheetEntry newEntry = new SheetEntry();
        newEntry.append(this);
        return newEntry;
    }

    // Parsing tools

    public static SheetEntry parseTabEntry(String line, List<String> fieldNames) {
        return parseEntry(line, "\t", fieldNames);
    }

    public static SheetEntry parseCommaEntry(String line, List<String> fieldNames) {
        return parseEntry(line, ",", fieldNames);
    }

    public static SheetEntry parseEntry(String line, String delimiter, List<String> fieldNames) {
        SheetEntry entry = new SheetEntry();
        String[] parts = line.split(delimiter);
        int i = 0;
        for (String field : fieldNames) {
            entry.put(field, (i < parts.length ? new String(parts[i]) : ""));
            i++;
        }

        return entry;
    }

    public static SheetEntry fromJSON(String jsonString) {
        JsonParser parser = new JsonParser();
        final JsonElement parsed = parser.parse(jsonString);

        SheetEntry newEntry = new SheetEntry();
        parsed.getAsJsonObject()
              .entrySet()
              .forEach(entry -> newEntry.put(entry.getKey(), entry.getValue().getAsString()));

        return newEntry;
    }

    // Mutability tools

    public static SheetEntry byAppending(SheetEntry entry, String key, String value) {
        SheetEntry newEntry = new SheetEntry();
        newEntry.append(entry);
        newEntry.put(key, value);
        return newEntry;
    }

    public static SheetEntry merge(SheetEntry entry, SheetEntry secondEntry) {
        SheetEntry newEntry = entry.clone();
        newEntry.append(secondEntry);
        return newEntry;
    }

    // ---- Functional utilities ----

    public static Function<SheetEntry, String> getField(String key) {
        return (entry -> entry.get(key));
    }

    public static ToIntFunction<SheetEntry> getIntField(String key) {
        return (entry -> entry.getInt(key));
    }

    public static ToIntFunction<SheetEntry> getIntField(String key, int defaultValue) {
        return (entry -> entry.getIntOrDefault(key, defaultValue));
    }

    public static ToDoubleFunction<SheetEntry> getDoubleField(String key) {
        return (entry -> entry.getDouble(key));
    }

    public static ToDoubleFunction<SheetEntry> getDoubleForField(String key, double defaultValue) {
        return (entry -> entry.getDoubleOrDefault(key, defaultValue));
    }

    public static Function<SheetEntry, Optional<Double>> getDoubleOptionForField(String key) {
        return (entry -> entry.getDoubleOption(key));
    }

    public static Function<SheetEntry, Optional<Integer>> getIntOptionforField(String key) {
        return (entry -> entry.getIntOption(key));
    }

    public static Predicate<SheetEntry> fieldEmpty(String key) {
        return (entry -> !entry.containsField(key) || entry.get(key).isEmpty());
    }

    public static Predicate<SheetEntry> fieldNotEmpty(String key) {
        return fieldEmpty(key).negate();
    }

    public static Predicate<SheetEntry> fieldEquals(String key, Object ref) {
        return (entry -> entry.get(key).equals(ref));
    }

    public static Predicate<SheetEntry> fieldContains(String key, String value) {
        return entry -> entry.get(key).contains(value);
    }

    public static Predicate<SheetEntry> fieldAmong(String key, Collection<Object> values) {
        return entry -> values.contains(entry.get(key));
    }

    public static Predicate<SheetEntry> fieldAmong(String key, Object... values) {
        return entry -> Arrays.asList(values).contains(entry.get(key));
    }

    public static Predicate<SheetEntry> fieldInRange(String key, double lowerBound, double upperBound) {
        return entry -> lowerBound < entry.getDouble(key)
                        && entry.getDouble(key) < upperBound;
    }

    public static Predicate<SheetEntry> fieldGreaterThan(String key, double lowerBound) {
        return entry -> entry.getDouble(key) > lowerBound;
    }

    public static Predicate<SheetEntry> fieldLessThan(String key, double upperBound) {
        return entry -> entry.getDouble(key) < upperBound;
    }
}
