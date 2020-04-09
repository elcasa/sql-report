package it.elcasa.sqlreport.model


import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "reports")
class ConfigReports {
    List<String> launchReportList
    Map<String,Report> reports
}
