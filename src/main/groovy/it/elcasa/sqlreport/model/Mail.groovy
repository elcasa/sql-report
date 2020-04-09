package it.elcasa.sqlreport.model

class Mail {
    // Report Configuration
    String subject
    String heading
    String tableCaption
    String tableQuery
    String body

    String from
    List<String> to
    List<String> cc
    List<String> bcc

    // Global Configuration
    String template
    TemplatePlaceholders templatePlaceholders
    TableStyle tableStyle
}
