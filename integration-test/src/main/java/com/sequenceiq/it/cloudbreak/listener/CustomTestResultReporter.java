package com.sequenceiq.it.cloudbreak.listener;

import java.util.List;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;

public class CustomTestResultReporter implements IReporter {
    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        // TODO report results in a format that can be easily processed for ImageValidatorE2ETestUtil.getXmlSuites
        IReporter.super.generateReport(xmlSuites, suites, outputDirectory);
    }
}
