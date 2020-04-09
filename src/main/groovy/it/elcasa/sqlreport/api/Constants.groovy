package it.elcasa.sqlreport.api

import it.elcasa.sqlreport.model.Formats

class Constants {

    // Defaults
    static final String DEFAULT_MAIL_FROM = 'noreply@sql-report.it'
    static final String DEFAULT_WORKBOOK_CREATOR = 'sql-report'
    static final String DEFAULT_REPORT_OUTPUT_PATH = 'output'

    static final Formats DEFAULT_FORMATS = new Formats([
            dateFormatAttachment : 'yyyy-mm-dd',
            dateFormatMailBody   : 'yyyy-mm-dd',
            timestampFormat      : 'yyyyMMdd_HHmmss',
        ])

    // SXSSFWorkbook settings
    static final int DEFAULT_ROW_ACCESS_WINDOW_SIZE = 100
    static final boolean DEFAULT_COMPRESS_TMP_FILES = true
    static final boolean DEFAULT_USE_SHARED_STRINGS_TABLE = true

    // Filename
    static final String REPORT_FILENAME_DEFAULT = 'Report'
    static final String REPORT_FILENAME_REGEX = '[^a-zA-Z0-9_]'
    static final String REPORT_FILENAME_REPLACEMENT_CHARACTER = '_'
    static final String REPORT_ATTACHMENT_FILENAME_EXTENSION = '.xlsx'
    static final String REPORT_MAILBODY_FILENAME_EXTENSION = '.html'

}
