package it.elcasa.sqlreport.model

class WorkbookConfig {
    String query
    String filename

    Boolean isStreamingWorkbook
    Boolean autoFilter
    Boolean lockFirstRow

    String chartType
    ReportChartTypeEnum chartTypeEnum
    // TODO ChartColumns

}
