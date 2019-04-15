package com.sequenceiq.it.cloudbreak.newway;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

public class CustomInvocationHandler implements IInvokedMethodListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomInvocationHandler.class);

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        LOGGER.info("%%%%%%%-" + method.getTestMethod().getMethodName()
                + "-%%-" + Arrays.toString(testResult.getParameters()));
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

    }
}
