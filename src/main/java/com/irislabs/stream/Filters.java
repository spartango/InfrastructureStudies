package com.irislabs.stream;

import com.irislabs.sheet.SheetEntry;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Author: spartango
 * Date: 6/15/14
 * Time: 11:19.
 */
public class Filters {
    public static final List<String> NA_VALUES = Arrays.asList("", "[Unknown]", "[Not Available]", "[Not Applicable]");

    public static Predicate<SheetEntry> containsKeys(String... keys) {
        return (entry -> Arrays.asList(keys)
                               .stream()
                               .map(key -> entry.containsKey(key)
                                           && !entry.get(key).isEmpty()
                                           && !NA_VALUES.contains(entry.get(key)))
                               .collect(MultiCollectors.andCollector()));
    }

}
