package it.elcasa.sqlreport.model


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "global")
class ConfigGlobal {

    Formats formats

    // Colors
    String headingColorAttachment
    String headingColorMailTable

    // Technical
    Boolean tempFileMailBody
    String pathLog
    String pathReport
    Mail mail
    MailSmtp mailSmtp
}
