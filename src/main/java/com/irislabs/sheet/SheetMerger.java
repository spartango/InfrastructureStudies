package com.irislabs.sheet;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author: spartango
 * Date: 6/14/14
 * Time: 13:08.
 */
public class SheetMerger {
    private static final String joiner = "::";

    protected Map<String, SheetEntry> baseEntries;
    protected Set<String>             fields;

    public SheetMerger() {
        baseEntries = new HashMap<>();
        fields = new LinkedHashSet<>();
    }

    public void append(Sheet sheet) {
        sheet.stream().forEach(this::append);
    }

    public void append(SheetEntry entry, Function<SheetEntry, String> accessor) {
        String primary = accessor.apply(entry);
        if (baseEntries.containsKey(primary)) {
            SheetEntry existingEntry = baseEntries.get(primary);
            existingEntry.append(entry);
        } else {
            baseEntries.put(primary, entry);
        }

        fields.addAll(entry.fields());
    }

    public void append(SheetEntry entry) {
        append(entry, SheetEntry::getPrimary);
    }

    public void append(SheetEntry entry, String... keys) {
        append(entry, line -> Arrays.asList(keys)
                                    .stream()
                                    .map(key -> line.getOrDefault(key, ""))
                                    .collect(Collectors.joining(joiner)));
    }

    public Collection<SheetEntry> getMerged() {
        return baseEntries.values();
    }

    public Set<String> getFields() {
        return fields;
    }
}
