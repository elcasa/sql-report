package it.elcasa.sqlreport

import groovy.util.logging.Slf4j
import it.elcasa.sqlreport.api.SqlReportEngine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@Slf4j
@SpringBootApplication
class SqlReportApplication implements ApplicationRunner{

    @Autowired
    SqlReportEngine sqlReport

    static void main(String[] args) {
        // SpringApplication.run(SqlReportApplication, args)

        def applicationClass = SqlReportApplication
        def applicationProperties = [
                'spring.config.name'      : 'application, config, datasources',
                'spring.batch.job.enabled': false,
        ]

        // def application =
        new SpringApplicationBuilder(applicationClass)
                .properties(applicationProperties)
                .web(WebApplicationType.NONE)
                .run(args)

        // System.exit(SpringApplication.exit(application))
    }

    @Override
    void run(ApplicationArguments args) {
        sqlReport.doLogic(args)
    }

    /*
    Es. line arg
    nohup /appl/batch/bin/run --spring.config.location=/appl/batch/ '--dataRiferimento=2019-05-09'  &
    */
}
