package com.irislabs.sheet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Author: spartango
 * Date: 4/14/14
 * Time: 11:20 PM.
 */
public class FileSheet implements Sheet {

    private File         file;
    private List<String> fields;
    private String       delimiter;

    public FileSheet(String path) throws IOException {
        this(new File(path), "\t");
    }

    public FileSheet(File file, String delimiter) throws IOException {
        this.file = file;
        this.delimiter = delimiter;
        fields = Collections.emptyList();
        parseFields();
    }

    @Override public Stream<SheetEntry> stream() {
        try {
            return parseLines();
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    private void parseFields() throws IOException {
        Files.lines(file.toPath()).findFirst().ifPresent(line -> fields = Arrays.asList(line.split(delimiter)));
    }

    private Stream<SheetEntry> parseLines() throws IOException {
        return Files.lines(file.toPath()).skip(1).map(line -> SheetEntry.parseEntry(line, delimiter, fields));
    }

    @Override public List<String> fields() {
        return fields;
    }
}
