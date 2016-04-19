package com.irislabs.sheet;

import java.util.Optional;

/**
 * Author: spartango
 * Date: 4/21/15
 * Time: 20:23.
 */
public class SheetFactory {
    public static Optional<Sheet> buildFromFile(String filename) {
        try {
            if (filename.endsWith(".csv")) {
                return Optional.of(new CSVSheet(filename));
            } else if (filename.endsWith(".xlsx")) {
                return Optional.of(new ExcelSheet(filename));
            } else if (filename.endsWith(".xls")) {
                return Optional.of(new ExcelSheet(filename));
            } else if (filename.endsWith(".tsv")) {
                return Optional.of(new FileSheet(filename, "\t"));
            } else if (filename.endsWith(".txt")) {
                // TODO: ry to guess the most frequent character
                return Optional.of(new FileSheet(filename));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}
