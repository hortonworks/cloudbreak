package com.sequenceiq.it.cloudbreak.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IClass;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;
import org.uncommons.reportng.HTMLReporter;
import org.uncommons.reportng.ReportNGException;

import com.sequenceiq.it.cloudbreak.performance.KeyPerformanceIndicator;
import com.sequenceiq.it.cloudbreak.performance.Measure;
import com.sequenceiq.it.cloudbreak.performance.Util;

public class CustomHTMLReporter extends HTMLReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomHTMLReporter.class);

    private static final String ONLY_FAILURES_PROPERTY = "org.uncommons.reportng.failures-only";

    private static final String RESULTS_FILE = "results.html";

    private static final String RESULT_KEY = "result";

    private static final String FAILED_CONFIG_KEY = "failedConfigurations";

    private static final String SKIPPED_CONFIG_KEY = "skippedConfigurations";

    private static final String FAILED_TESTS_KEY = "failedTests";

    private static final String SKIPPED_TESTS_KEY = "skippedTests";

    private static final String PASSED_TESTS_KEY = "passedTests";

    private static final String REPORT_DIRECTORY = "html";

    private static final Comparator<ITestResult> RESULT_COMPARATOR = Comparator.comparing(ITestResult::getName);

    private static final Comparator<IClass> CLASS_COMPARATOR = Comparator.comparing(IClass::getName);

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectoryName) {
        try {
            super.generateReport(xmlSuites, suites, outputDirectoryName);
            boolean onlyFailures = "true".equals(System.getProperty(ONLY_FAILURES_PROPERTY, "false"));
            File outputDirectory = new File(outputDirectoryName, REPORT_DIRECTORY);
            createCustomResults(suites, outputDirectory, onlyFailures);
        } catch (Exception ex) {
            throw new ReportNGException("Failed generating HTML report.", ex);
        }

    }

    private void createCustomResults(List<ISuite> suites, File outputDirectory,
            boolean onlyShowFailures) {
        AtomicInteger suiteIndex = new AtomicInteger(1);
        suites.stream().forEach(suite -> {
            AtomicInteger testIndex = new AtomicInteger(1);
            Consumer<ISuiteResult> resultConsumer = getSuiteResultConsumer(outputDirectory, onlyShowFailures,
                    suiteIndex, testIndex);
            suite.getResults().values().stream().forEach(resultConsumer);
            suiteIndex.incrementAndGet();
        });
    }

    private Consumer<ISuiteResult> getSuiteResultConsumer(File outputDirectory, boolean onlyShowFailures,
            AtomicInteger suiteIndex, AtomicInteger testIndex) {
        return result -> {
            generateResultFile(outputDirectory, onlyShowFailures,
                    suiteIndex.get(), testIndex.get(), result);
            testIndex.incrementAndGet();
        };
    }

    private void generateResultFile(File outputDirectory, boolean onlyShowFailures,
            int suiteIndex, int testIndex, ISuiteResult result) {
        try {
            boolean failuresExist = result.getTestContext().getFailedTests().size() > 0
                    || result.getTestContext().getFailedConfigurations().size() > 0;
            if (!onlyShowFailures || failuresExist) {
                VelocityContext context = createContext();
                context.put(RESULT_KEY, result);
                context.put(FAILED_CONFIG_KEY, sortByTestClass(result.getTestContext().getFailedConfigurations()));
                context.put(SKIPPED_CONFIG_KEY, sortByTestClass(result.getTestContext().getSkippedConfigurations()));
                context.put(FAILED_TESTS_KEY, sortByTestClass(result.getTestContext().getFailedTests()));
                context.put(SKIPPED_TESTS_KEY, sortByTestClass(result.getTestContext().getSkippedTests()));
                context.put(PASSED_TESTS_KEY, sortByTestClass(result.getTestContext().getPassedTests()));
                Measure measures = Util.collectMeasurements(result.getTestContext());
                Util.writeToFile(measures, outputDirectory.getPath() + System.getProperty("file.separator") + "measures.csv");
                KeyPerformanceIndicator keyIndicators = Util.getKeyPerformance(measures);
                if (keyIndicators != null) {
                    context.put(Util.KEY_PERFORMANCE_INDICATOR, keyIndicators);
                }
                String fileName = String.format("suite%d_test%d_%s", suiteIndex, testIndex, RESULTS_FILE);
                File file = new File(outputDirectory, fileName);
                FileUtils.deleteQuietly(file);
                generateFile(file, "custom." + RESULTS_FILE + TEMPLATE_EXTENSION, context);
            }
        } catch (Exception e) {
            LOGGER.error("Error during result report generation: ", e);
        }
    }

    private SortedMap<IClass, List<ITestResult>> sortByTestClass(IResultMap results) {
        SortedMap<IClass, List<ITestResult>> sortedResults = new TreeMap<IClass, List<ITestResult>>(CLASS_COMPARATOR);
        for (ITestResult result : results.getAllResults()) {
            List<ITestResult> resultsForClass = sortedResults.get(result.getTestClass());
            if (resultsForClass == null) {
                resultsForClass = new ArrayList<>();
                sortedResults.put(result.getTestClass(), resultsForClass);
            }
            int index = Collections.binarySearch(resultsForClass, result, RESULT_COMPARATOR);
            if (index < 0) {
                index = Math.abs(index + 1);
            }
            resultsForClass.add(index, result);
        }
        return sortedResults;
    }

}
