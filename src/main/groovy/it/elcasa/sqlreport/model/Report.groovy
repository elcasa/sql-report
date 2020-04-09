package it.elcasa.sqlreport.model

class Report {
    String name
    String type
    ReportTypeEnum typeEnum
    String datasource
    WorkbookConfig workbookConfig
    Mail mail
    Formats formats
    //TODO
    //ConfigGlobal overrideGlobal
}
