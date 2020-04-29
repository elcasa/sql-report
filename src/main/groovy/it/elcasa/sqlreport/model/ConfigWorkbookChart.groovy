package it.elcasa.sqlreport.model

class ConfigWorkbookChart {
    String chartType
    ReportChartTypeEnum chartTypeEnum

    String titleText
    String xAxisTitle
    String yAxisTitle

    Integer xAxisColumn
    Boolean xAxisNumerical
    List<Integer> yAxisColumns

}
