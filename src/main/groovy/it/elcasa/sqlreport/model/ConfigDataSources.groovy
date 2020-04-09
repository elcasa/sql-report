package it.elcasa.sqlreport.model

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties
class ConfigDataSources {
    Map<String, String> datasource
    // TODO
    Map<String, Map<String, String>> datasources
}
