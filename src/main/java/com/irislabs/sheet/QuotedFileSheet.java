package com.irislabs.sheet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Author: spartango
 * Date: 12/24/14
 * Time: 18:21.
 */
public class QuotedFileSheet extends FileSheet {
    public QuotedFileSheet(String path) throws IOException {
        super(path);
    }

    public QuotedFileSheet(String file, String delimiter) throws IOException {
        super(file, delimiter);
    }

    public QuotedFileSheet(File file, String delimiter) throws IOException {
        super(file, delimiter);
    }

    @Override public Stream<SheetEntry> stream() {
        try {
            return parseLines();
        } catch (IOException e) {
            e.printStackTrace();
            return Stream.empty();
        }
    }

    protected Stream<SheetEntry> parseLines() throws IOException {
        return lines(file.toPath())
                .skip(1) // Skip the header
                .map(line -> parseEntry(line, fields));
    }

    private SheetEntry parseEntry(String[] parts, List<String> fieldNames) {
        SheetEntry entry = new SheetEntry();
        int i = 0;
        for (String field : fieldNames) {
            entry.put(field, (i < parts.length ? parts[i] : ""));
            i++;
        }

        return entry;
    }

    private Stream<String[]> lines(Path path) {
        try {
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            Iterator<String[]> it = new Iterator<String[]>() {
                boolean haveNext = true;

                public String[] next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }

                    ArrayList<String> parts = new ArrayList<>(fields.size());
                    StringBuilder builder = new StringBuilder();
                    boolean inQuote = false;

                    // While we havent hit a newline
                    int read;
                    try {
                        // Read a character
                        while ((read = reader.read()) != -1 && (read != '\n' || inQuote)) {
                            //  While we havent hit a delimiter
                            if (read == '"') {
                                //      If the character is a quote
                                //          Start/Stop ignoring delimiters/newline
                                inQuote = !inQuote;
                                // builder.append(read);
                            } else if (!inQuote && String.valueOf((char) read).equals(delimiter)) {
                                //      If the character is a delimiter (not ignored)
                                //          Add a part and break out
                                parts.add(builder.toString());
                                // Clear
                                builder.delete(0, builder.length());
                            } else {
                                //      Else
                                //          Add the character to the part
                                builder.append((char) read);
                            }
                        }
                        if (read == -1) {
                            haveNext = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();  //TODO handle e
                        haveNext = false;
                    }
                    // Return the parts list
                    return parts.toArray(new String[parts.size()]);
                }

                public boolean hasNext() {
                    return haveNext;
                }
            };

            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it,
                                                                            Spliterator.IMMUTABLE
                                                                            | Spliterator.NONNULL),
                                        false);
        } catch (IOException e) {
            e.printStackTrace();  //TODO handle e
        }

        return Stream.empty();
    }

}
