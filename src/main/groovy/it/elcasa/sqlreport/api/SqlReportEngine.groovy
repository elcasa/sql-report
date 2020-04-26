package it.elcasa.sqlreport.api

import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import it.elcasa.sqlreport.model.ConfigDataSources
import it.elcasa.sqlreport.model.ConfigGlobal
import it.elcasa.sqlreport.model.ConfigReports
import it.elcasa.sqlreport.model.Report
import it.elcasa.sqlreport.model.ReportChartTypeEnum
import it.elcasa.sqlreport.model.ReportTypeEnum
import it.elcasa.sqlreport.model.TableStyle
import it.elcasa.sqlreport.model.TemplatePlaceholders
import org.apache.commons.text.StringEscapeUtils
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xddf.usermodel.PresetColor
import org.apache.poi.xddf.usermodel.XDDFColor
import org.apache.poi.xddf.usermodel.XDDFShapeProperties
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties
import org.apache.poi.xddf.usermodel.chart.AxisCrosses
import org.apache.poi.xddf.usermodel.chart.AxisPosition
import org.apache.poi.xddf.usermodel.chart.BarDirection
import org.apache.poi.xddf.usermodel.chart.ChartTypes
import org.apache.poi.xddf.usermodel.chart.LegendPosition
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis
import org.apache.poi.xddf.usermodel.chart.XDDFChartData
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFChart
import org.apache.poi.xssf.usermodel.XSSFClientAnchor
import org.apache.poi.xssf.usermodel.XSSFDrawing
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.stereotype.Component

import javax.activation.DataHandler
import javax.mail.BodyPart
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.text.SimpleDateFormat

@Slf4j
@Component
@CompileStatic
class SqlReportEngine {

    @Autowired
    ConfigGlobal configGlobal

    @Autowired
    ConfigReports configReports

    @Autowired
    ConfigDataSources configDataSources

    SimpleDateFormat sdfTimestamp
    String charsetName


    private final static String CONFIGURATION_ERROR = "Configuration KO, "

    void doLogic(ApplicationArguments args){
        log.info 'Uno Due Sei Nove!'

        // TODO global paramter charset with utf 8 default
        charsetName = StandardCharsets.UTF_8.name()

        // Reports to be launched
        def launchReportList = configReports.launchReportList

        // Checks
        try {
            // TODO Args parameter, all, or report list name
            if(!launchReportList){
                throw new SqlReportException(CONFIGURATION_ERROR +
                        "lauchReportList is empty!")
            }
            if(!configReports.reports){
                throw new SqlReportException(CONFIGURATION_ERROR +
                        "there's no report in configuration. Add at least one report")
            }

            ////////////////////////
            // Reports config validation
            configReports.reports.keySet().each { String key ->
                Report report = configReports.reports[key]
                report.name = key

                // FIXME Old behavior... now not necessary
                if(!report.name || !report.name.trim()){
                    throw new SqlReportException(CONFIGURATION_ERROR +
                            "report name is mandatory. Be sure that all reports have a name set in configuration")
                }

                String logHeader = "${CONFIGURATION_ERROR}Report ${report.name} of Report type ${report.type}: "

                // Report Type
                ReportTypeEnum typeEnum = ReportTypeEnum.retrieveType(report.type)
                if(!typeEnum){
                    throw new SqlReportException(logHeader +
                            "type not recognized. Allowed report types are ${ReportTypeEnum.values()}")
                }
                report.typeEnum = typeEnum

                if(!report.workbookConfig?.query && !report.mail?.tableQuery){
                    throw new SqlReportException(logHeader +
                            "Report requires attachmentQuery or mailBodyQuery")
                }

                if(report.typeEnum == ReportTypeEnum.SEND_MAIL){
                    if(!report.mail?.to && !report.mail?.cc && !report.mail?.bcc){
                        throw new SqlReportException(logHeader +
                                "Report type ${ReportTypeEnum.SEND_MAIL} require a mail recipient")
                    }
                }

                if(report.typeEnum == ReportTypeEnum.CREATE_REPORT){
                    // ??
                }

                ////////
                // Workbook checks
                if(report.workbookConfig?.query) {

                    if (report.workbookConfig.isStreamingWorkbook == null) {
                        report.workbookConfig.isStreamingWorkbook = true
                    }

                    // Chart Type
                    // if the report has a chart, it will be created in NON-STREAMING mode
                    if (report.workbookConfig.chartType) {
                        ReportChartTypeEnum chartEnum = ReportChartTypeEnum.retrieveType(report.workbookConfig.chartType)
                        if (!chartEnum) {
                            throw new SqlReportException(logHeader +
                                    "chart type not recognized. Allowed chart types are ${ReportChartTypeEnum.values()}")
                        }
                        report.workbookConfig.chartTypeEnum = chartEnum
                        report.workbookConfig.isStreamingWorkbook = false
                        log.info "Report ${report.name}: Workbook containing Chart are created in NON-STREAMING mode!"
                    }
                    if (report.workbookConfig.query) {
                        if (report.workbookConfig.isStreamingWorkbook) {
                            log.info "Report ${report.name}: Workbook will be created in STREAMING mode (SXSSF), this allows to write very large files without running out of memory"
                        } else {
                            log.warn "Report ${report.name}: Workbook will be created in NON-STREAMING mode (XSSF)! This could cause out of memory with enormous Workbook"
                        }
                    }
                }

            }

            log.info "Report defined in configuration: ${configReports.reports.keySet()}"
            log.info "launchReportList: ${launchReportList}"
        } catch (SqlReportException sre){
            log.error(sre.message)
            return
        }

        sdfTimestamp =  new SimpleDateFormat(
                configGlobal.formats?.timestampFormat ?: Constants.DEFAULT_FORMATS.timestampFormat)

        launchReportList.each { reportName ->
            def report = configReports.reports[reportName]
            if (report) {
                try {
                    launchReport(report)
                }
                catch (SqlReportException sre){
                    log.error("Report '$reportName' of type '$report.type': $sre.message")
                }
            } else {
                log.error "${reportName} not found in reports configuration"
            }
        }

        /*

        println '\n ****** eachRow'
        sql.eachRow(SAMPLE_QUERY) { row ->
            println "${row.DRIVERS}, ${row.ENTRIES}, ${row.POLES}, ${row.PERCENTAGE}, ${row.LAST_POLE}"
        }


        // Oracle
        //def db = [url   : 'jdbc:oracle:thin:@localhost:1521/XEPDB1', user: 'SYSTEM', password: 'password',
        //          driver: 'oracle.jdbc.driver.OracleDriver']
        //def sqlOracle = Sql.newInstance(db.url, db.user, db.password, db.driver)

        def query = """
            SELECT 1 AS NUMBER_COL, 'VARCHAR' AS STRING_COL,
                SYSDATE AS DATE_COL FROM DUAL
            UNION
            SELECT 2, 'VARCHAR2', SYSDATE + 2 FROM DUAL
            """

        //sqlOracle.eachRow(query) { row ->
        //    println "${row.NUMBER_COL}, ${row.STRING_COL}, ${row.DATE_COL}"
        //}

        */

    }

    private void launchReport(Report report){
        log.info "########"
        log.info "Start report ${report.name}"

        // TODO get Hikari Spring boot DS
        Connection sampleDbConn = DriverManager.getConnection(
                configDataSources.datasource['url'],
                configDataSources.datasource['username'],
                configDataSources.datasource['password'],

                //"jdbc:hsqldb:mem:sampledb",
                //"SA",
                //""
            )
         // "org.hsqldb.jdbc.JDBCDriver"
        def sql = new Sql(sampleDbConn)

        ////////////////////////
        // Create Workbook
        Workbook wb = null
        if (report?.workbookConfig?.query) {
            def workbookConfig = report.workbookConfig

            Map<String, String> formats = [:]
            formats[WorkbookFactory.CELL_STYLE_DATE] =
                    report.formats?.dateFormatWorkbook ?: configGlobal.formats?.dateFormatWorkbook ?: Constants.DEFAULT_FORMATS.dateFormatWorkbook

            WorkbookFactory workbookFactory = new WorkbookFactory(workbookConfig.isStreamingWorkbook, formats)
            wb = workbookFactory.workbook
            Sheet sheet = wb.createSheet()

            if (workbookConfig.isStreamingWorkbook){
                (sheet as SXSSFSheet).trackAllColumnsForAutoSizing()
            }

            // Metadata
            // http://groovy-lang.org/databases.html#_fetching_metadata
            int queryColumnCount
            def columnTypeList = []

            sql.eachRow(workbookConfig.query,
                    { meta ->
                        // Query metadata: get column name and type
                        queryColumnCount = meta.columnCount
                        Row reportRow = sheet.createRow(sheet.lastRowNum + 1)

                        (1..queryColumnCount).each { index ->
                            // Save column types
                            columnTypeList.add(meta.getColumnType(index))

                            // Write header
                            Cell cell = reportRow.createCell(index - 1)
                            cell.setCellValue(meta.getColumnLabel(index) as String)
                        }
                    },
                    { row ->
                        // Query row: set values in report
                        Row reportRow = sheet.createRow(sheet.lastRowNum + 1)
                        (0..queryColumnCount - 1).each { index ->
                            def value = row[index]
                            Cell cell = reportRow.createCell(index)

                            switch (value) {
                            //case java.sql.Date:
                                case Date:
                                    cell.setCellValue(value as Date) // .time ?
                                    cell.setCellStyle(workbookFactory.cellStyleDate)
                                    break
                            //case String:
                            //    cell.setCellValue(value as String)
                            //    cell.setCellStyle(workbookFactory.cellStyleString)
                            //    break
                                case Number:
                                    cell.setCellValue(value as Double)
                                    cell.setCellStyle(workbookFactory.cellStyleNumberFloat)
                                    break
                                default:
                                    cell.setCellValue(value as String)
                                    cell.setCellStyle(workbookFactory.cellStyleString)
                                    break
                            }
                        }
            })

            log.info "Report ${report.name}: attachmentQuery DONE"

            ////////
            // Style

            // Header style
            def header = sheet.getRow(0)

            CellStyle headingRowCellStyle = wb.createCellStyle()
            // Background color
            // TODO globalConfig
            headingRowCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)
            headingRowCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index)
            // Text style
            XSSFFont headingFont = wb.createFont() as XSSFFont
            headingFont.setBold(true)
            headingRowCellStyle.setFont(headingFont)

            // Set style and auto size column width for all defined cells (must be contiguous)
            (0..header.physicalNumberOfCells - 1).each { index ->
                sheet.autoSizeColumn(index)
                header.getCell(index).setCellStyle(headingRowCellStyle)

            }

            //def cell
            //for (int i = 0; i < header.getPhysicalNumberOfCells(); i++) {
            //    cell = header.getCell(i)
            //    cell.setCellStyle(headingRowCellStyle)
            //    sheet.autoSizeColumn(i)
            //}

            if (workbookConfig.autoFilter) {
                // Set header empty filter
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, header.getPhysicalNumberOfCells() - 1))
            }

            if (workbookConfig.lockFirstRow) {
                // Lock the entire first row
                sheet.createFreezePane(0, 1)
            }

            // Suppress cell Warning
            //sheet.addIgnoredErrors(
            //      new CellRangeAddress(sheet.getFirstRowNum() + 1, sheet.getLastRowNum(), 0, 0),
            //    IgnoredErrorType.NUMBER_STORED_AS_TEXT)

            ////
            // Workbook Chart
            if (report.workbookConfig.chartTypeEnum){
                XSSFSheet chartSheet = sheet as XSSFSheet

                // Columns to show in chart
                // TODO
                int xAxisColumn = 0
                int[] yAxisColumnList = [1,2]
                int sheetLastRow =  sheet.getPhysicalNumberOfRows()

                /*
                XSSFSheet chartSheet = wb.createSheet("barchart") as XSSFSheet

                final int NUM_OF_ROWS = 3;
                final int NUM_OF_COLUMNS = 10;

                // Create a row and put some cells in it. Rows are 0 based.
                Row row;
                Cell cell;
                for (int rowIndex = 0; rowIndex < NUM_OF_ROWS; rowIndex++) {
                    row = chartSheet.createRow((short) rowIndex);
                    for (int colIndex = 0; colIndex < NUM_OF_COLUMNS; colIndex++) {
                        cell = row.createCell((short) colIndex);
                        cell.setCellValue((colIndex * (rowIndex + 1.0)) as int);
                    }
                }

                */

                XSSFDrawing drawing = chartSheet.createDrawingPatriarch();
                XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, 5, 10, 15);

                XSSFChart chart = drawing.createChart(anchor);
                chart.setTitleText("x = 2x and x = 3x");
                chart.setTitleOverlay(false);
                XDDFChartLegend legend = chart.getOrAddLegend();
                legend.setPosition(LegendPosition.TOP_RIGHT);

                // Use a category axis for the bottom axis.
                XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
                bottomAxis.setTitle("x"); // https://stackoverflow.com/questions/32010765
                XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
                leftAxis.setTitle("f(x)");
                leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

                XDDFDataSource<Double> xs = XDDFDataSourcesFactory.fromNumericCellRange(chartSheet,
                        new CellRangeAddress(1, sheetLastRow-1, 0, 0));
                XDDFNumericalDataSource<Double> ys1 = XDDFDataSourcesFactory.fromNumericCellRange(chartSheet,
                        new CellRangeAddress(1, sheetLastRow-1, 1, 1));
                XDDFNumericalDataSource<Double> ys2 = XDDFDataSourcesFactory.fromNumericCellRange(chartSheet,
                        new CellRangeAddress(1, sheetLastRow-1, 2, 2));

                if(workbookConfig.chartTypeEnum == ReportChartTypeEnum.BAR_CHART) {

                    XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
                    XDDFChartData.Series series1 = data.addSeries(xs, ys1);
                    series1.setTitle("2x", null); // https://stackoverflow.com/questions/21855842
                    XDDFChartData.Series series2 = data.addSeries(xs, ys2);
                    series2.setTitle("3x", null);
                    chart.plot(data);

                    // in order to transform a bar chart into a column chart, you just need to change the bar direction
                    XDDFBarChartData bar = (XDDFBarChartData) data;
                    bar.setBarDirection(BarDirection.COL);
                    // looking for "Stacked Bar Chart"? uncomment the following line
                    // bar.setBarGrouping(BarGrouping.STACKED);

                    solidFillSeries(data, 0, PresetColor.CHARTREUSE);
                    solidFillSeries(data, 1, PresetColor.TURQUOISE);
                }

            }
        }

        ////////////////////////
        // HTML Mail body

        String mailBody = null
        if (report.mail) {
            // Report configuration overrides global configuration
            // TODO defaults hardcoded
            mailBody = report.mail?.template ?: configGlobal.mail?.template
            TemplatePlaceholders templatePlaceholders = report.mail?.templatePlaceholders ?: configGlobal.mail?.templatePlaceholders
            TableStyle tableStyle = report.mail?.tableStyle ?: configGlobal.mail?.tableStyle

            String htmlDataTable = null
            if(report.mail.tableQuery) {
                // Structure
                /*
                  <table ${tableStyle?.table}>
                    <caption ${tableStyle?.caption}>Monthly savings</caption>

                    <tr style="text-align:center;">
                      <th ${tableStyle?.tableHeaderColumn}>Month</th>
                      <th ${tableStyle?.tableHeaderColumn}>Savings</th>
                    </tr>

                    <tr>
                      <td ${tableStyle?.tableDataColumn}>January</td>
                      <td ${tableStyle?.tableDataColumn}>\$100</td>
                    </tr>
                    <tr>
                      <td ${tableStyle?.tableDataColumn}>February</td>
                      <td ${tableStyle?.tableDataColumn}>\$50</td>
                    </tr>
                  </table>
                 */

                def sdfDate = new SimpleDateFormat(
                        report.formats?.dateFormatMailBody ?: configGlobal.formats?.dateFormatMailBody ?: Constants.DEFAULT_FORMATS.dateFormatMailBody)

                String indent4 = '    '
                String tableHeader = ''
                String tableData = ''

                // TODO Temp file
                /*
                File tempFile = null
                if(configGlobal.tempFileMailBody){
                    Path path = Files.createTempFile(report.name, ".sqlreporttmp")
                    tempFile = path.toFile()
                    tempFile.deleteOnExit()
                }
                */

                // Query mail data table
                int queryColumnCount
                def columnTypeList = []
                sql.eachRow(report.mail.tableQuery,
                    { meta ->
                        // Query metadata: get column name and type
                        queryColumnCount = meta.columnCount

                        tableHeader += "\n${indent4}"
                        tableHeader += """<tr style="text-align:center;">"""
                        (1..queryColumnCount).each { index ->
                            // Save column types
                            columnTypeList.add(meta.getColumnType(index))

                            String value = meta.getColumnLabel(index) as String

                            value = value?.trim()
                            // Escape HTML characters that could mess with table HTML code
                            value = StringEscapeUtils.escapeHtml4(value)

                            // Write header column
                            tableHeader += "\n${indent4}${indent4}"
                            tableHeader += "<th ${tableStyle?.tableHeaderColumn ?: ''}>${value}</th>"
                        }
                        tableHeader += "\n${indent4}</tr>"
                    },
                    { row ->
                        tableData += "\n${indent4}<tr>"

                        // Query row: get table data column
                        (0..queryColumnCount - 1).each { index ->

                            def value = row[index]
                            String stringValue
                            switch (value) {
                            //case java.sql.Date:
                                case Date:
                                    stringValue = sdfDate.format(value as Date)
                                    break
                            //case String:
                            //    cell.setCellValue(value as String)
                            //    cell.setCellStyle(workbookFactory.cellStyleString)
                            //    break
                                case Number:
                                    stringValue = (value as Double).toString()
                                    break
                                default:
                                    stringValue = value as String
                                    break
                            }

                            stringValue = stringValue?.trim()
                            // Escape HTML characters that could mess with table HTML code
                            stringValue = StringEscapeUtils.escapeHtml4(stringValue)

                            tableData += "\n${indent4}${indent4}"
                            tableData += "<td ${tableStyle?.tableDataColumn ?: ''}>${stringValue ?: ''}</td>"
                        }

                        tableData += "\n${indent4}</tr>"

                    })

                log.info "Report ${report.name}: mailBodyQuery DONE"

                htmlDataTable = """\
                |
                |<table ${tableStyle?.table ?: ''}> 
                |    <caption ${tableStyle?.caption ?: ''}>${StringEscapeUtils.escapeHtml4(report.mail.tableCaption) ?: ''}</caption>
                |    
                |    ${tableHeader}
                |    
                |    ${tableData}
                |    
                |</table>
                |
                """.stripMargin()

                log.info htmlDataTable
            }

            // Replace placeholders
            Map<CharSequence, CharSequence> replacementsMap = [:]
            replacementsMap[templatePlaceholders.heading] = report.mail.heading ?: ''
            replacementsMap[templatePlaceholders.table] = htmlDataTable ?: ''
            replacementsMap[templatePlaceholders.body] = report.mail.body ?: ''

            mailBody = mailBody.replace(replacementsMap)
        }

        ////////////////////////
        // Output

        // Report Attachment Filename
        String timestamp = sdfTimestamp.format(new Date())
        String baseFileName = report.workbookConfig?.filename ?: report.name
        baseFileName = baseFileName + Constants.REPORT_FILENAME_REPLACEMENT_CHARACTER
        baseFileName = baseFileName.replaceAll(
                Constants.REPORT_FILENAME_REGEX, Constants.REPORT_FILENAME_REPLACEMENT_CHARACTER)
        baseFileName = baseFileName.replaceAll(
                "${Constants.REPORT_FILENAME_REPLACEMENT_CHARACTER}+", Constants.REPORT_FILENAME_REPLACEMENT_CHARACTER)

        // Html Mail Body Filename
        String mailBodyFilename = baseFileName + timestamp + Constants.REPORT_MAILBODY_FILENAME_EXTENSION
        String reportAttachmentName = baseFileName + timestamp + Constants.REPORT_ATTACHMENT_FILENAME_EXTENSION

        if(report.typeEnum == ReportTypeEnum.CREATE_REPORT){
            if(!mailBody && !wb){
                throw new SqlReportException("There's nothing to create!")
            }
            String outputPath = configGlobal.pathReport ?: Constants.DEFAULT_REPORT_OUTPUT_PATH

            // Create directory
            File path = new File(outputPath)
            path.mkdirs()

            if(mailBody){
                def pathname = "${outputPath}/${mailBodyFilename}"

                def f = new File(pathname)
                f.write(mailBody, charsetName)
                log.info "Written file: $pathname"
            }

            if(wb) {
                def filePathname = "${outputPath}/${reportAttachmentName}"

                def out = new FileOutputStream(filePathname)
                wb.write(out)
                out.close()

                if(report.workbookConfig.isStreamingWorkbook){
                    // dispose of temporary files backing this workbook on disk
                    (wb as SXSSFWorkbook).dispose()
                }

                log.info "Written file: $filePathname"
            }
        }

        if(report.typeEnum == ReportTypeEnum.SEND_MAIL){
            // Connect to SMTP Server

            // Set directly from property:
            // mail.smtp.host, mail.smtp.auth, mail.smtp.starttls.enable, mail.smtp.port
            javax.mail.Authenticator auth = null
            if (configGlobal.mailSmtp.properties['mail.smtp.auth']?.equals('true')){
                auth = new javax.mail.Authenticator() {
                    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new javax.mail.PasswordAuthentication(
                                configGlobal.mailSmtp.username,
                                configGlobal.mailSmtp.password)
                    }
                }
            }
            // Session session = Session.getDefaultInstance(configMailSmtp.properties)
            Session session = Session.getInstance(configGlobal.mailSmtp.properties, auth)

            MimeMessage message = new MimeMessage(session)
            try {

                try {
                    InternetAddress from = new InternetAddress(report.mail?.from ?: Constants.DEFAULT_MAIL_FROM)
                    message.setFrom(from)

                    InternetAddress[] to = createAddressArray(report.mail.to)
                    if(to){ message.setRecipients(Message.RecipientType.TO, to) }

                    InternetAddress[] cc = createAddressArray(report.mail.cc)
                    if(cc){ message.setRecipients(Message.RecipientType.CC, cc) }

                    InternetAddress[] bcc = createAddressArray(report.mail.bcc)
                    if(bcc){ message.setRecipients(Message.RecipientType.BCC, bcc) }
                } catch (AddressException e) {
                    throw new SqlReportException("Create mail InternetAddress failed", e)
                }

                message.setSubject(report.mail.subject)

                // Compose email message
                Multipart multipart = new MimeMultipart()
                // Create HTML Part
                BodyPart htmlBodyPart = new MimeBodyPart()
                htmlBodyPart.setContent(mailBody,"text/html")
                //htmlBodyPart.setContent(mailBody, "text/html; charset=utf-8") //TODO charsetName variable
                multipart.addBodyPart(htmlBodyPart)

                if(wb) {
                    // Create Attachment
                    ByteArrayOutputStream bos = new ByteArrayOutputStream()
                    wb.write(bos)
                    bos.close()
                    ByteArrayDataSource attachmentDataSource = new ByteArrayDataSource(bos.toByteArray(), "application/vnd.ms-excel")

                    if(report.workbookConfig.isStreamingWorkbook) {
                        // dispose of temporary files backing this workbook on disk
                        (wb as SXSSFWorkbook).dispose()
                    }

                    // Create attachment part
                    BodyPart attachmentBodyPart = new MimeBodyPart()
                    attachmentBodyPart.setDataHandler(new DataHandler(attachmentDataSource))
                    attachmentBodyPart.setFileName(reportAttachmentName)
                    multipart.addBodyPart(attachmentBodyPart)
                }

                // Set the Multipart's to be the email's content
                message.setContent(multipart)
                Transport.send(message)
                log.info("Mail sent")

            } catch (MessagingException e) {
                throw new SqlReportException("Send mail failed", e)
            }
        }
    }

    private static InternetAddress[] createAddressArray(List<String> adresses){
        if(!adresses){
            return null
        }
        List<InternetAddress> internetAddressList = []
        adresses.each { address ->
            internetAddressList.add(new InternetAddress(address))
        }

        InternetAddress[] array = internetAddressList as InternetAddress[]
        return array
    }

    private static void solidFillSeries(XDDFChartData data, int index, PresetColor color) {
        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(XDDFColor.from(color));
        XDDFChartData.Series series = data.getSeries().get(index);
        XDDFShapeProperties properties = series.getShapeProperties();
        if (properties == null) {
            properties = new XDDFShapeProperties();
        }
        properties.setFillProperties(fill);
        series.setShapeProperties(properties);
    }
}
