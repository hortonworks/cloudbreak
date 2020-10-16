package com.sequenceiq.it.cloudbreak.search;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.testng.IClass;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.xml.XmlSuite;
import org.uncommons.reportng.JUnitXMLReporter;
import org.uncommons.reportng.ReportNGException;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.performance.KeyPerformanceIndicator;
import com.sequenceiq.it.cloudbreak.performance.Measure;
import com.sequenceiq.it.cloudbreak.performance.Util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit XML reporter for TestNG that uses Velocity templates to generate its
 * output.
 * @author Daniel Dyer
 */
@Component
@EnableConfigurationProperties(CommonCloudProperties.class)
public class CustomJUnitXMLReporter extends JUnitXMLReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomJUnitXMLReporter.class);

    private static final String RESULTS_KEY = "results";

    private static final String RESULTS_FILE = "results.xml";

    private static final String REPORT_DIRECTORY = "xml";

    private static CommonCloudProperties commonCloudProperties;

    private KeyPerformanceIndicator<?> keyPerformanceIndicator;

    @Inject
    @SuppressFBWarnings("ST")
    public void setCommonCloudProperties(CommonCloudProperties commonCloudProperties) {
        this.commonCloudProperties = commonCloudProperties;
    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectoryName) {
        removeEmptyDirectories(outputDirectoryName);

        File outputDirectory = new File(outputDirectoryName, REPORT_DIRECTORY);
        createCustomXmlDirectory(outputDirectory);
        Collection<TestClassResults> flattenedResults = flattenResults(suites, outputDirectory);

        for (TestClassResults results : flattenedResults) {
            VelocityContext context = createContext();
            context.put(RESULTS_KEY, results);
            context.put(Util.KEY_PERFORMANCE_INDICATOR, getPerformanceIndicator());

            try {
                generateFile(new File(outputDirectory, String.join("_", commonCloudProperties.getCloudProvider().toLowerCase(),
                        results.getTestClassName(), RESULTS_FILE)), "custom." + RESULTS_FILE + TEMPLATE_EXTENSION, context);
            } catch (Exception ex) {
                throw new ReportNGException("Failed generating JUnit XML report.", ex);
            }
        }
    }

    /**
     * Deletes any empty directories under the output directory.  These
     * directories are created by TestNG for its own reports regardless
     * of whether those reports are generated.  If you are using the
     * default TestNG reports as well as ReportNG, these directories will
     * not be empty and will be retained.  Otherwise they will be removed.
     * @param outputDirectoryName   The directory to search for empty directories.
     */
    private void removeEmptyDirectories(String outputDirectoryName) {
        File folder = new File(outputDirectoryName);
        Path folderPath = Path.of(folder.getPath()).normalize().toAbsolutePath();

        File[] foundEmptyDirectories = Optional.ofNullable(folder.listFiles((file, path) ->
                file.isDirectory() && Objects.requireNonNull(file.listFiles()).length == 0)).orElse(new File[0]);
        if (foundEmptyDirectories.length > 0) {
            for (File file : foundEmptyDirectories) {
                try {
                    if (file.delete()) {
                        LOGGER.info("Empty folder: {} have been found and deleted at: {}.", file.getName(), folderPath);
                    } else {
                        LOGGER.info("Empty folder: {} have NOT been deleted at: {}.", file.getName(), folderPath);
                    }
                } catch (Exception e) {
                    LOGGER.info("Empty folder: {} cleanup has been failed, because of: {}", file.getName(), e.getMessage(), e);
                }
            }
        }
    }

    private void createCustomXmlDirectory(File outputDirectory) {
        Path folderPath = Path.of(outputDirectory.getPath()).normalize().toAbsolutePath();
        try {
            Files.createDirectories(folderPath);
        } catch (Exception e) {
            LOGGER.info("Creating folder {} throws exception: {}", folderPath, e.getMessage(), e);
        }
    }

    /**
     * Flatten a list of test suite results into a collection of results grouped by test class.
     * This method basically strips away the TestNG way of organising tests and arranges
     * the results by test class.
     */
    private Collection<TestClassResults> flattenResults(List<ISuite> suites, File outputDirectory) {
        Map<IClass, TestClassResults> flattenedResults = new HashMap<IClass, TestClassResults>();
        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                organiseByClass(suiteResult.getTestContext().getFailedTests().getAllResults(), flattenedResults);
                organiseByClass(suiteResult.getTestContext().getSkippedTests().getAllResults(), flattenedResults);
                organiseByClass(suiteResult.getTestContext().getPassedTests().getAllResults(), flattenedResults);
                Measure measures = Util.collectMeasurements(suiteResult.getTestContext());
                Util.writeToFile(measures, outputDirectory.getPath() + System.getProperty("file.separator") + "measures.csv");
                KeyPerformanceIndicator keyIndicators = Util.getKeyPerformance(measures);
                if (keyIndicators != null) {
                    setPerformanceIndicator(keyIndicators);
                }
            }
        }
        return flattenedResults.values();
    }

    private void setPerformanceIndicator(KeyPerformanceIndicator keyIndicators) {
        keyPerformanceIndicator = keyIndicators;
    }

    private KeyPerformanceIndicator getPerformanceIndicator() {
        return keyPerformanceIndicator;
    }

    private void organiseByClass(Set<ITestResult> testResults, Map<IClass, TestClassResults> flattenedResults) {
        for (ITestResult testResult : testResults) {
            if (testResult != null) {
                Throwable testResultException = testResult.getThrowable();
                String methodName = testResult.getName();
                int status = testResult.getStatus();
                String testName = String.join("_", commonCloudProperties.getCloudProvider().toLowerCase(), methodName);

                if (testResultException != null) {
                    try {
                        String message = testResultException.getCause().getMessage() != null
                                ? testResultException.getCause().getMessage()
                                : testResultException.getMessage();
                        String testFailureType = testResultException.getCause().getClass().getName();

                        if (message == null || message.isEmpty()) {
                            LOGGER.warn("Test Case: {} have been failed with empty test result!", methodName);
                        } else {
                            LOGGER.info("Failed test results are: Test Case: {} | Test Name: {} | Status: {} | Failure Type: {} | Message: {}",
                                    methodName, testName, status, testFailureType, message);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Test case: {} got Unexpected Exception: {}", methodName, e.getMessage());
                    }
                } else {
                    getTestErrorFromTestOutput(testResult, methodName, status);
                }

                testResult.setTestName(testName);
                getResultsForClass(flattenedResults, testResult).addResult(testResult);
            } else {
                LOGGER.error("Test result is NULL!");
            }
        }
    }

    private void getTestErrorFromTestOutput(ITestResult testResult, String methodName, int status) {
        List<String> reporterOutputs = Reporter.getOutput(testResult);
        List<String> reporterOutputException = new ArrayList<>();
        AtomicReference<List<String>> reporterOutputLines = new AtomicReference<>();
        String reporterOutputCause = null;
        int start = 0;
        int finish = 0;

        reporterOutputs.forEach(reporterOutput -> reporterOutputLines.set(Arrays.asList(reporterOutputs.toString().split("\\r?\\n"))));

        if (reporterOutputLines.get() != null && reporterOutputLines.get().size() != 0) {
            for (int i = 0; i < reporterOutputLines.get().size(); i++) {
                if (reporterOutputLines.get().get(i)
                        .equalsIgnoreCase("All Exceptions that occurred during the test are logged after this message")) {
                    start = i + 2;
                }
                if (reporterOutputLines.get().get(i)
                        .contains("Caused by:")) {
                    finish = i;
                    reporterOutputCause = reporterOutputLines.get().get(i);
                }
            }
            for (int i = start; i < finish; i++) {
                reporterOutputException.add(reporterOutputLines.get().get(i));
            }
            LOGGER.info("Failed test result exception is NULL! Test case: {} | Test Status: {} | Test Output Error Cause: {} " +
                    "| Test Output Exception: {}", methodName, status, reporterOutputCause, reporterOutputException);
        } else {
            LOGGER.info("Failed test result exception is NULL and Test Output is also NULL or empty");
        }
    }

    /**
     * Look-up the results data for a particular test class.
     */
    private TestClassResults getResultsForClass(Map<IClass, TestClassResults> flattenedResults, ITestResult testResult) {
        TestClassResults resultsForClass = flattenedResults.get(testResult.getTestClass());
        if (resultsForClass == null) {
            resultsForClass = new TestClassResults(testResult.getTestClass());
            flattenedResults.put(testResult.getTestClass(), resultsForClass);
        }
        return resultsForClass;
    }

    /**
     * Groups together all of the data about the tests results from the methods
     * of a single test class.
     */
    public static final class TestClassResults {

        private final IClass testClass;

        private final Collection<ITestResult> failedTests = new LinkedList<ITestResult>();

        private final Collection<ITestResult> skippedTests = new LinkedList<ITestResult>();

        private final Collection<ITestResult> passedTests = new LinkedList<ITestResult>();

        private long duration;

        private TestClassResults(IClass testClass) {
            this.testClass = testClass;
        }

        public IClass getTestClass() {
            return testClass;
        }

        public String getTestClassName() {
            return testClass.getRealClass().getSimpleName();
        }

        /**
         * Adds a test result for this class.  Organises results by outcome.
         */
        void addResult(ITestResult result) {
            switch (result.getStatus()) {
                case ITestResult.SKIP:
                    if (META.allowSkippedTestsInXML()) {
                        skippedTests.add(result);
                    }
                    break;
                case ITestResult.FAILURE:
                case ITestResult.SUCCESS_PERCENTAGE_FAILURE:
                    failedTests.add(result);
                    break;
                case ITestResult.SUCCESS:
                    passedTests.add(result);
                    break;
                default:
                    break;
            }
            duration += result.getEndMillis() - result.getStartMillis();
        }

        public Collection<ITestResult> getFailedTests() {
            return failedTests;
        }

        public Collection<ITestResult> getSkippedTests() {
            return skippedTests;
        }

        public Collection<ITestResult> getPassedTests() {
            return passedTests;
        }

        public long getDuration() {
            return duration;
        }
    }
}
