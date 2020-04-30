package com.sequenceiq.it.cloudbreak.util.spot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class SpotRetryOnceTestListener implements ITestListener {

    /**
     * Add {@link SpotRetryOnce} retry analyzer to test methods that should be retried on failure.
     */
    @Override
    public void onTestStart(ITestResult result) {
        ITestNGMethod testNGMethod = result.getMethod();
        if (SpotUtil.shouldUseSpotInstances(testNGMethod.getConstructorOrMethod().getMethod())) {
            testNGMethod.setRetryAnalyzerClass(SpotRetryOnce.class);
        }
    }

    public static class SpotRetryOnce implements IRetryAnalyzer {

        private static final Logger LOGGER = LoggerFactory.getLogger(SpotRetryOnce.class);

        @Override
        public boolean retry(ITestResult result) {
            if (SpotRetryUtil.willRetry(result.getMethod())) {
                LOGGER.info("Retrying test");
                return true;
            }
            return false;
        }
    }
}
