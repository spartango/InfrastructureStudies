package com.irislabs.sheet;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Author: spartango
 * Date: 11/29/14
 * Time: 09:48.
 */
public class ExcelSheet implements Sheet {
    private Workbook                          workbook;
    private org.apache.poi.ss.usermodel.Sheet sheet;

    private List<String> fields;

    public ExcelSheet(String file) throws IOException, InvalidFormatException {
        this(new File(file));
    }

    public ExcelSheet(File file) throws IOException, InvalidFormatException {
        workbook = WorkbookFactory.create(file);
        sheet = workbook.getSheetAt(0);
        parseFields();
    }

    @Override public Stream<SheetEntry> stream() {
        try {
            return parseLines();
        } catch (IOException e) {
            e.printStackTrace();  //TODO handle e
            return Stream.empty();
        }
    }

    @Override public List<String> fields() {
        return fields;
    }

    private void parseFields() throws IOException {
        Row firstRow = sheet.getRow(0);
        fields = new ArrayList<>(firstRow.getPhysicalNumberOfCells());
        firstRow.cellIterator()
                .forEachRemaining(cell -> fields.add(cell.getStringCellValue()));
    }

    private Stream<SheetEntry> parseLines() throws IOException {
        return StreamSupport.stream(sheet.spliterator(), false)
                .skip(1) // Skip the first row; headers
                .map(row -> {
                    SheetEntry entry = new SheetEntry();
                    row.cellIterator()
                       .forEachRemaining(cell -> {
                           int col = cell.getColumnIndex();
                           if (col < fields.size()) {
                               String field = fields.get(col);
                               if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
                                   entry.put(field, cell.getStringCellValue());
                               } else if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                                   entry.put(field, cell.getNumericCellValue());
                               }
                           }
                       });
                    return entry;
                });
    }

    public CollectionSheet toCollectionSheet() {
        return new CollectionSheet(stream().collect(Collectors.toList()), fields());
    }
}
