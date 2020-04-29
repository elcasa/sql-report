package it.elcasa.sqlreport.model

class ConfigWorkbook {
    String query
    String filename

    Boolean isStreamingWorkbook
    Boolean autoFilter
    Boolean lockFirstRow

    ConfigWorkbookChart chart
}
