package com.sequenceiq.it.cloudbreak.listener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class PassedTestsReporter implements IReporter {

    private static final String REPORT_FILENAME = "passed-tests.txt";

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        Path reportPath = Paths.get(outputDirectory, REPORT_FILENAME);
        try {
            Set<String> reportLines = new HashSet<>(getAlreadyPassedTests());
            for (ISuite suite : suites) {
                for (ISuiteResult result : suite.getResults().values()) {
                    for (ITestResult testResult : result.getTestContext().getPassedTests().getAllResults()) {
                        String reportLine = getReportLine(testResult.getTestClass().getRealClass(), testResult.getMethod().getMethodName());
                        reportLines.add(reportLine);
                    }
                }
            }
            Files.write(reportPath, reportLines);
        } catch (Exception e) {
            throw new TestFailException("Failed to generate passed tests report", e);
        }
    }

    public static boolean isAlreadyPassedTest(Class<?> testClass, String methodName) {
        String reportLine = getReportLine(testClass, methodName);
        return getAlreadyPassedTests().contains(reportLine);
    }

    private static Set<String> getAlreadyPassedTests() {
        Path reportPath = Paths.get(REPORT_FILENAME);
        if (Files.exists(reportPath)) {
            try (Stream<String> lines = Files.lines(reportPath)) {
                return lines.collect(Collectors.toSet());
            } catch (IOException e) {
                throw new TestFailException("Failed to read passed tests report", e);
            }
        } else {
            return Set.of();
        }
    }

    private static String getReportLine(Class<?> testClass, String methodName) {
        return String.format("%s::%s", testClass.getSimpleName(), methodName);
    }

}
