package com.irislabs.sheet;

import java.util.*;
import java.util.function.BiConsumer;

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

    public int getPrimaryInt() {
        return Integer.parseInt(fields.values().stream().findFirst().get());
    }

    public String get(String key) {
        return fields.get(key);
    }

    public double getDouble(String key) throws NumberFormatException {
        return Double.parseDouble(fields.get(key));
    }

    public int getInt(String key) throws NumberFormatException {
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

    public boolean containsKey(String key) {
        return fields.containsKey(key) && !fields.get(key).isEmpty();
    }

    public String getOrDefault(String key, String defaultValue) {
        return fields.getOrDefault(key, defaultValue);
    }

    public double getOrDefaultDouble(String key, double defaultValue) {
        try {
            return fields.containsKey(key) ? getDouble(key) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public int getOrDefaultInt(String key, int defaultValue) {
        try {
            return fields.containsKey(key) ? getInt(key) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return fields.entrySet();
    }

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
            entry.put(field, (i < parts.length ? parts[i] : ""));
            i++;
        }

        return entry;
    }

    public void append(SheetEntry entry) {
        putAll(entry.fields);
    }

    public int getFieldCount() {
        return fields.size();
    }

    public static SheetEntry byAppending(SheetEntry entry, String key, String value) {
        SheetEntry newEntry = new SheetEntry();
        newEntry.append(entry);
        newEntry.put(key, value);
        return newEntry;
    }

    public static SheetEntry merge(SheetEntry entry, SheetEntry secondEntry) {
        SheetEntry newEntry = new SheetEntry();
        newEntry.append(entry);
        newEntry.append(secondEntry);
        return newEntry;
    }

    @Override public String toString() {
        return "{" + fields + '}';
    }
}
