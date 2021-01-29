package com.sequenceiq.it.cloudbreak.log;

import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

public class Log<T extends CloudbreakTestDto> {

    public static final String TEST_CONTEXT_REPORTER = "testContextReporter";

    public static final String ALTERNATE_LOG = "alternateLog";

    private static final Logger LOGGER = LoggerFactory.getLogger(Log.class);

    private Log() {
    }

    public static void whenJson(Logger logger, String message, Object jsonObject) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, jsonObject);
        log(logger, "When", message, writer.toString());
    }

    public static void whenJson(String message, Object jsonObject) throws IOException {
        whenJson(null, message, jsonObject);
    }

    public static void log(String message, Object... args) {
        log(null, message, args);
    }

    public static void log(Logger logger, String message, Object... args) {
        String format = String.format(message, args);
        log(format);
        if (logger != null) {
            logger.info(format);
        }
    }

    public static void error(Logger logger, String message, Object... args) {
        String format = String.format(message, args);
        log(format);
        if (logger != null) {
            logger.error(format);
        }
    }

    private static void log(Logger logger, String step, String message) {
        log(logger, step, message, null);
    }

    private static void log(Logger logger, String step, String message, String json) {
        TestContextReporter testContextReporter = getReporter();
        if (testContextReporter == null) {
            log(logger, step + " " + message);
        } else {
            getReporter().addStep(step, message, json);
        }
        publishReport(getReporter());
    }

    public static void given(Logger logger, String message) {
        log(logger, "Given", message);
    }

    public static void as(Logger logger, String message) {
        log(logger, "As", message);
    }

    public static void when(Logger logger, String message) {
        log(logger, "When", message);
    }

    public static void whenException(Logger logger, String message) {
        log(logger, "WhenException", message);
    }

    public static void then(Logger logger, String message) {
        log(logger, "Then", message);
    }

    public static void expect(Logger logger, String message) {
        log(logger, "Expect", message);
    }

    public static void await(Logger logger, String message) {
        log(logger, "Await", message);
    }

    public static void validateError(Logger logger, String message) {
        log(logger, "Validate", message);
    }

    public static void log(String message) {
        Reporter.log(message);
    }

    public static void log(ITestResult testResult) {
        if (testResult != null) {
            Throwable testResultException = testResult.getThrowable();
            String methodName = testResult.getName();
            int status = testResult.getStatus();

            if (testResultException != null) {
                try {
                    String message = testResultException.getCause() != null
                            ? testResultException.getCause().getMessage()
                            : testResultException.getMessage();
                    String testFailureType = testResultException.getCause() != null
                            ? testResultException.getCause().getClass().getName()
                            : testResultException.getClass().getName();

                    if (message == null || message.isEmpty()) {
                        log(format(" Test Case: %s have been failed with empty test result! ", methodName));
                    } else {
                        LOGGER.info("Failed test results are: Test Case: {} | Status: {} | Failure Type: {} | Message: {}", methodName, status,
                                testFailureType, message);
                        log(message);
                    }
                } catch (Exception e) {
                    log(format(" Test Case: %s got Unexpected Exception: %s ", methodName, e.getMessage()));
                }
            }
        } else {
            LOGGER.error("Test result is NULL!");
        }
    }

    private static TestContextReporter getReporter() {
        ITestResult res = Reporter.getCurrentTestResult();
        if (res == null) {
            return null;
        }
        TestContextReporter reporter = (TestContextReporter) res.getAttribute(TEST_CONTEXT_REPORTER);
        if (reporter == null) {
            reporter = new TestContextReporter();
            res.setAttribute(TEST_CONTEXT_REPORTER, reporter);
        }

        return reporter;
    }

    private static void publishReport(TestContextReporter testContextReporter) {
        ITestResult res = Reporter.getCurrentTestResult();
        if (res != null) {
            res.setAttribute(ALTERNATE_LOG, testContextReporter.print());
        }
    }
}
