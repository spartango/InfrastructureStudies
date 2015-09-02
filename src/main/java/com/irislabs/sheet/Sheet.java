package com.irislabs.sheet;

import java.util.List;
import java.util.stream.Stream;

/**
 * Author: spartango
 * Date: 4/14/14
 * Time: 9:56 PM.
 */
public interface Sheet {
    public Stream<SheetEntry> stream();

    public List<String> fields();
}
