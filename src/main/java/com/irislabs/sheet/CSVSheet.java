package com.irislabs.sheet;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Author: spartango
 * Date: 12/23/14
 * Time: 23:18.
 */
public class CSVSheet implements Sheet {

    private File         file;
    private List<String> fields;

    public CSVSheet(String file) throws IOException {
        this(new File(file));
    }

    public CSVSheet(File file) throws IOException {
        this.file = file;
        parseFields();
    }

    @Override public Stream<SheetEntry> stream() {
        try {
            CSVReader reader = new CSVReader(new FileReader(file));
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(reader.iterator(), Spliterator.IMMUTABLE),
                                        false)
                                .skip(1)
                                .map(line -> {
                                    SheetEntry entry = new SheetEntry();
                                    int i = 0;
                                    for (String field : fields) {
                                        entry.put(field, (i < line.length ? new String(line[i]) : ""));
                                        i++;
                                    }
                                    return entry;
                                });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    @Override public List<String> fields() {
        return fields;
    }

    private void parseFields() throws IOException {
        CSVReader reader = new CSVReader(new FileReader(file));
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(reader.iterator(),
                                                                 Spliterator.IMMUTABLE | Spliterator.NONNULL),
                             false)
                     .findFirst()
                     .map(Arrays::asList)
                     .ifPresent(list -> fields = list);
        reader.close();
    }
}
