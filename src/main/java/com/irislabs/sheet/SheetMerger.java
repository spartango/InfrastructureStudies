package com.irislabs.sheet;

import java.util.*;

/**
 * Author: spartango
 * Date: 6/14/14
 * Time: 13:08.
 */
public class SheetMerger {

    protected Map<String, SheetEntry> baseEntries;
    protected Set<String>             fields;

    public SheetMerger() {
        baseEntries = new HashMap<>();
        fields = new LinkedHashSet<>();
    }

    public void append(FileSheet sheet) {
        sheet.stream().forEach(this::append);
    }

    public void append(SheetEntry entry) {
        if (baseEntries.containsKey(entry.getPrimary())) {
            SheetEntry existingEntry = baseEntries.get(entry.getPrimary());
            existingEntry.append(entry);
        } else {
            baseEntries.put(entry.getPrimary(), entry);
        }

        fields.addAll(entry.fields());
    }

    public Collection<SheetEntry> getMerged() {
        return baseEntries.values();
    }

    public Set<String> getFields() {
        return fields;
    }
}
