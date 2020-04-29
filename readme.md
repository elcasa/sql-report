# sql-report

Create a report (spreadsheet) from a database query and send it as an email attachment

## Description

sql-report uses Groovy, Spring Boot and Gradle to give you a simple project that you can use without knowing Java or Groovy.
* All functionalities are set in YAML configuration, you only have to learn the configuration structure, set your database, query, smtp mail server and you are done
* Working samples inside the project

Features
* Send mail with spreadsheet attached
* Send mail with an HTML table in mail body showing data from query
* Send mail with both, with two different queries
* Save only the spreadsheet report on disk

## Getting Started

### Dependencies

* JDK 8
* Java Database Connectivity (JDBC) have to be added as dependency in build.gradle or as .jar file in lib directory (e.g. Oracle jdbc, that cannot be downloaded from maven)

### Installing

* Install IntelliJ, set Git and JDK 8 location
* Checkout project, File -> New -> Project from existing sources
* Import Gradle Project, Use Auto Import, Use Default Gradle Wrapper
* Gradle -> application -> bootRun will launch report samples, you will see reports in the output folder

### Executing program

Steps:
* Put your jdbc driver in libs folder, if its not available on maven (ojdbc for example)
* Configure datasource
* Create your own report by editing yml configuration
* Configure your mail server
* Launch: Gradle -> application -> bootRun

### Creating a new report
TODO
```
code blocks for commands
```

Suggestions
* Try sql-report installing a local Oracle Database Express Edition (XE)
    * Start Database service on Windows: Start, Services, OracleServiceXE, Start Stop
    * localhost:1521/XEPDB1 SYSTEM password
    * localhost:3300/EM

## Help

Any advise for common problems or issues.
```
command to run if program contains helper info
```

## TODO List
* migliorare template mail html mail no reply ricevute
    * fare anche un esempio con un immagine
* grafici excel!!
    * sistema dimensione chart
    * vengono tagliate la prima e l'ultima barra
    * crea line-chart
    * recupera nome colonna da prima riga
* converti fine riga in lf?
* loggare su file
* ultimare readme .md
    * intro su yml config
    * gestione config per i vari ambienti
* parametro per i colori header html e excel usando il codice ffffff (un parametro unico??)
* testa tipi dato e formati
    * parametri formato sia per la data excel che mail e mettere un formato default 
    * formato number
* mappa di datasource hikaricp da configurazione
* mettere config global in report config per override?
* distribution: creazione pacchetti con config differenziati per ambiente
    * come gestire diversi ambienti? config yml per DB e application yml per report ? 


___________________________
* step 2: 
    * migliorare template mail html da mail no reply ricevute
    * scrivere su disco gradualmente la tabella HTML per non fare esplodere la RAM (File.createTempFile) CON PARAMETRO
    * advanced report con N fogli, n query e grafici non a barre e rette
    * type single value: sostituisci un placehoder con il valore prima riga prima colonna della query
    * multi mail con colonna SQL_REPORT_MAIL_TO , CC e BCC?
    * step z: grails version


## Authors

Contributors names and contact info

elcasa

## Version History

* 0.1
    * Initial Release

## License

This project is licensed under the GNU Affero General Public License v3.0 License - see the LICENSE file for details

## Acknowledgments

Inspiration, code snippets, etc.
* HTML Email: don't use _div_, use _table_
    * Basic Template: https://github.com/leemunroe/responsive-html-email-template
    * Best Practices: https://stackoverflow.com/questions/2229822/best-practices-considerations-when-writing-html-emails/21437734#21437734 
    * Visually test HTML Template: https://jsfiddle.net/
    * Template sample: https://webdesign.tutsplus.com/articles/build-an-html-email-template-from-scratch--webdesign-12770
    * Template sample 2: https://webdesign.tutsplus.com/articles/creating-a-simple-responsive-html-email--webdesign-12978
* YAML
    * https://www.mkyong.com/spring-boot/spring-boot-yaml-example/
    * https://docs.ansible.com/ansible/latest/reference_appendices/YAMLSyntax.html
* Spreadsheet generation
    * https://howtodoinjava.com/library/readingwriting-excel-files-in-java-poi-tutorial
    * https://stackoverflow.com/questions/5794659/poi-how-do-i-set-cell-value-to-date-and-apply-default-excel-date-format
* Sending Emails: https://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
* DB Access: http://groovy-lang.org/databases.html#_fetching_metadata