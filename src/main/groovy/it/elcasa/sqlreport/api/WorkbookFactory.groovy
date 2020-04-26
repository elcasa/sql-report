package it.elcasa.sqlreport.api

import org.apache.poi.ooxml.POIXMLProperties
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.DataFormat
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class WorkbookFactory {

    Workbook wb
    HashMap<String, CellStyle> cellStylesMap

    public static final String CELL_STYLE_STRING = 'CELL_STYLE_STRING'
    public static final String CELL_STYLE_NUMBER_FLOAT = 'CELL_STYLE_NUMBER_FLOAT'
    public static final String CELL_STYLE_DATE = 'CELL_STYLE_DATE'

    WorkbookFactory(boolean streaming, Map<String,String> formats){
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook()
        POIXMLProperties xmlProps = xssfWorkbook.getProperties()
        xmlProps.getCoreProperties().setCreator(Constants.DEFAULT_WORKBOOK_CREATOR)

        if(streaming) {
            // Low Memory usage:
            // - keep 100 rows in memory, exceeding rows will be flushed to disk
            int rowAccessWindowSize = Constants.DEFAULT_ROW_ACCESS_WINDOW_SIZE
            // - temp files will be gzipped
            boolean compressTmpFiles = Constants.DEFAULT_COMPRESS_TMP_FILES
            // - Optimize the use of repeated strings stores a single instance of the string in a table
            boolean useSharedStringsTable = Constants.DEFAULT_USE_SHARED_STRINGS_TABLE

            wb = new SXSSFWorkbook(xssfWorkbook, rowAccessWindowSize, compressTmpFiles, useSharedStringsTable)
        }
        else {
            wb = xssfWorkbook
        }

        // Workbook styles
        // POI data type
        // https://howtodoinjava.com/library/readingwriting-excel-files-in-java-poi-tutorial
        // https://stackoverflow.com/questions/5794659/poi-how-do-i-set-cell-value-to-date-and-apply-default-excel-date-format
        cellStylesMap = new HashMap<String, CellStyle>()

        DataFormat dataFormat = wb.createDataFormat()

        def cellStyle = wb.createCellStyle()
        cellStyle.setDataFormat(dataFormat.getFormat("@"))
        cellStylesMap.put(CELL_STYLE_STRING, cellStyle)

        cellStyle = wb.createCellStyle()
        cellStyle.setDataFormat(dataFormat.getFormat('0.0'))
        cellStylesMap.put(CELL_STYLE_NUMBER_FLOAT, cellStyle)

        cellStyle = wb.createCellStyle()
        cellStyle.setDataFormat(dataFormat.getFormat(
                formats[CELL_STYLE_DATE] ?: Constants.DEFAULT_FORMATS.dateFormatWorkbook))
        cellStylesMap.put(CELL_STYLE_DATE, cellStyle)
    }

    Workbook getWorkbook(){
        return wb
    }

    CellStyle getCellStyleString(){
        return cellStylesMap[CELL_STYLE_STRING]
    }

    CellStyle getCellStyleNumberFloat(){
        return cellStylesMap[CELL_STYLE_NUMBER_FLOAT]
    }

    CellStyle getCellStyleDate(){
        return cellStylesMap[CELL_STYLE_DATE]
    }

}
