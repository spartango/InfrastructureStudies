package com.irislabs.sheet;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Author: spartango
 * Date: 12/22/14
 * Time: 16:55.
 */
public class CollectionSheet implements Sheet {

    private List<String>           fields;
    private Collection<SheetEntry> source;

    public CollectionSheet(Collection<SheetEntry> source, List<String> fields) {
        this.fields = fields;
        this.source = source;
    }

    @Override public Stream<SheetEntry> stream() {
        return source.stream();
    }

    @Override public List<String> fields() {
        return fields;
    }
}
