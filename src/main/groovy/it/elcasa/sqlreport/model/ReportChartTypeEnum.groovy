package it.elcasa.sqlreport.model

enum ReportChartTypeEnum {
    BAR_CHART('bar-chart'),
    LINE_CHART('line-chart')

    String name

    private ReportChartTypeEnum(String name) {
        this.name = name
    }

    static ReportChartTypeEnum retrieveType(String name){
        return values().find { it.name == name}
    }

}
