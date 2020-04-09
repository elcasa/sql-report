package it.elcasa.sqlreport.api

class SqlReportException extends Exception {

    SqlReportException(String message){
        super(message)
    }

    SqlReportException(String message, Throwable cause) {
        super(message, cause)
    }

}
