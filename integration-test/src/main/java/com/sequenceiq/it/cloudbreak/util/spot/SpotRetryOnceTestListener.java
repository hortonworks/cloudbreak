package com.sequenceiq.it.cloudbreak.util.spot;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testng.IRetryAnalyzer;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.Reporter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD SpotBugs warning is suppressed because spring beans are needed when this class is initiated by TestNG
@Component
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
public class SpotRetryOnceTestListener implements ITestListener {

    private static SpotUtil spotUtil;

    private static SpotRetryUtil spotRetryUtil;

    @Inject
    public void setSpotUtil(SpotUtil spotUtil) {
        SpotRetryOnceTestListener.spotUtil = spotUtil;
    }

    @Inject
    public void setSpotRetryUtil(SpotRetryUtil spotRetryUtil) {
        SpotRetryOnceTestListener.spotRetryUtil = spotRetryUtil;
    }

    /**
     * Add {@link SpotRetryOnce} retry analyzer to test methods that should be retried on failure.
     */
    @Override
    public void onTestStart(ITestResult result) {
        ITestNGMethod testNGMethod = result.getMethod();
        if (spotUtil.shouldUseSpotInstancesForTest(testNGMethod.getConstructorOrMethod().getMethod())) {
            testNGMethod.setRetryAnalyzerClass(SpotRetryOnce.class);
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (result.getThrowable() instanceof org.testng.internal.thread.ThreadTimeoutException) {
            addToReport(result);
        }
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        addToReport(result);
    }

    private void addToReport(ITestResult result) {
        Reporter.log(String.format("%s test timed out with method: %s ", result.getName(), result.getMethod()));
    }

    public static class SpotRetryOnce implements IRetryAnalyzer {

        private static final Logger LOGGER = LoggerFactory.getLogger(SpotRetryOnce.class);

        @Override
        public boolean retry(ITestResult result) {
            if (spotRetryUtil.willRetry(result.getMethod())) {
                LOGGER.info("Retrying test");
                return true;
            }
            return false;
        }
    }
}
