package com.sequenceiq.it;

import java.io.File;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;
import org.uncommons.reportng.HTMLReporter;
import org.uncommons.reportng.ReportNGException;

public class CustomHTMLReporter extends HTMLReporter {

    private static final String INDEX_FILE = "custom.index.html";

    private static final String SUITES_KEY = "suites";

    private static final String ONLY_FAILURES_KEY = "onlyReportFailures";

    private static final String ONLY_FAILURES_PROPERTY = "org.uncommons.reportng.failures-only";

    private static final String SUITES_FILE = "custom.suites.html";

    private static final String REPORT_DIRECTORY = "html";

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectoryName) {
        try {
            super.generateReport(xmlSuites, suites, outputDirectoryName);
            boolean onlyFailures = "true".equals(System.getProperty(ONLY_FAILURES_PROPERTY, "false"));
            File outputDirectory = new File(outputDirectoryName, REPORT_DIRECTORY);
            createCustomFrameset(outputDirectory);
            createCustomSuiteList(suites, outputDirectory, onlyFailures);
        } catch (Exception ex) {
            throw new ReportNGException("Failed generating HTML report.", ex);
        }

    }

    private void createCustomSuiteList(List<ISuite> suites, File outputDirectory, boolean onlyFailures) throws Exception {
        VelocityContext context = createContext();
        context.put(SUITES_KEY, suites);
        context.put(ONLY_FAILURES_KEY, onlyFailures);
        generateFile(new File(outputDirectory, SUITES_FILE), SUITES_FILE + TEMPLATE_EXTENSION, context);
    }

    private void createCustomFrameset(File outputDirectory) throws Exception {
        VelocityContext context = createContext();
        generateFile(new File(outputDirectory, INDEX_FILE), INDEX_FILE + TEMPLATE_EXTENSION, context);
    }

}
