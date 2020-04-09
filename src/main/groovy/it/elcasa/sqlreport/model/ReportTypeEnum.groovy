package it.elcasa.sqlreport.model

enum ReportTypeEnum {
    CREATE_REPORT('create-report'),
    SEND_MAIL('send-mail')

    String name

    private ReportTypeEnum(String name) {
        this.name = name
    }

    static ReportTypeEnum retrieveType(String name){
        return values().find { it.name == name}
    }

}
