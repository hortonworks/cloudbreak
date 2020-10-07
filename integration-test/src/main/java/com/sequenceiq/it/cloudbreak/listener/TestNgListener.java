package com.sequenceiq.it.cloudbreak.listener;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.internal.IResultListener;

public class TestNgListener implements IResultListener, ISuiteListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestNgListener.class);

    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("Finished testing: {}", context.getName());
    }

    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("Start testing: {}", context.getName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        LOGGER.warn("Test Failed (but within success percentage): {}", result.getName(), result.getThrowable());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        Throwable err = result.getThrowable();
        LOGGER.error("Test failed: {}", result.getName(), err);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        if (result.getThrowable() != null) {
            LOGGER.warn("Skipping test {}: {}", result.getName(), result.getThrowable().getMessage());
        } else {
            LOGGER.info("Skipping test {}: Unsatisfied dependency", result.getName());
        }
    }

    @Override
    public  void onTestStart(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        LOGGER.info(String.format("Starting test: %s%s", result.getName(), getParameters(result)));
    }

    @Override
    public  void onTestSuccess(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            LOGGER.info("Expected exception of {} '{}' was thrown.", throwable.getClass().getName(), throwable.getMessage());
        }
        LOGGER.info(String.format("Test Passed: %s%s", result.getName(), getParameters(result)));
    }

    @Override
    public void onConfigurationFailure(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        LOGGER.error("Configuration Failed: {}", result.getName(), result.getThrowable());
    }

    @Override
    public void onConfigurationSkip(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        if (result.getThrowable() != null) {
            LOGGER.warn("Skipping configuration {} : {}", result.getName(), result.getThrowable().getMessage());
        } else {
            LOGGER.info("Skipping configuration {} Unsatisfied dependency", result.getName());
        }
    }

    @Override
    public void onConfigurationSuccess(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        LOGGER.info("Configuration completed: {}.{}", result.getTestClass().getName(), result.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        LOGGER.info("Finishing test suite: {}", suite.getName());
    }

    @Override
    public void onStart(ISuite suite) {
        LOGGER.info("Starting test suite: {}", suite.getName());
    }

    public String getParameters(ITestResult result) {
        String params = "";
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            params = "(" + Arrays.deepToString(parameters) + ")";
        }
        return params;
    }
}