package com.sequenceiq.it.cloudbreak.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.internal.invokers.InvokedMethod;

public class TestCaseTimeoutListener implements ITestListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCaseTimeoutListener.class);

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        long testRunInMs = result.getEndMillis() - result.getStartMillis();
        LOGGER.error("Test timed out: '{}' it took: '{}' ms", result.getName(), testRunInMs);
        LOGGER.info("Invoking TestInvocationListener to persist created resources in a JSON output file for clean up job.");
        TestInvocationListener testInvocationListener = new TestInvocationListener();
        IInvokedMethod invokedMethod = new InvokedMethod(System.currentTimeMillis(), result);
        testInvocationListener.afterInvocation(invokedMethod, result);
    }
}