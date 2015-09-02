package com.irislabs.stream;

import com.irislabs.sheet.SheetEntry;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Author: spartango
 * Date: 6/23/14
 * Time: 22:50.
 */
public class Numeric {

    public static Function<SheetEntry, SheetEntry> numerify(Collection<String> fields) {
        Map<String, AtomicInteger> counterMap = new HashMap<>();
        Map<String, Integer> numericMap = new TreeMap<>();
        fields.forEach(field -> counterMap.put(field, new AtomicInteger(0)));

        return patient -> {
            SheetEntry newEntry = new SheetEntry();
            patient.forEach((key, value) -> {
                if (StringUtils.isNumeric(value)) {
                    // Try to parse the value as is
                    newEntry.put(key, Double.parseDouble(value));
                } else {
                    // If this value hasn't been seen before
                    String kvPair = key + "_" + value;

                    if (!numericMap.containsKey(kvPair)) {
                        int newValue = counterMap.get(key).getAndIncrement();
                        numericMap.put(kvPair, newValue);
                    }

                    newEntry.put(key, numericMap.get(kvPair));
                }
            });
            return newEntry;
        };
    }
}
