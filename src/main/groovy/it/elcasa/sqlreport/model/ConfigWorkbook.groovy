package it.elcasa.sqlreport.model

class ConfigWorkbook {
    String query
    String filename
    Boolean csvFile

    Boolean isStreamingWorkbook
    Boolean autoFilter
    Boolean lockFirstRow

    ConfigWorkbookChart chart
}
