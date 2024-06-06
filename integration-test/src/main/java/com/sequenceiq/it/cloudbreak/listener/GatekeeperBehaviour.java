package com.sequenceiq.it.cloudbreak.listener;

import java.util.Objects;

import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;

public class GatekeeperBehaviour extends CheckedListener implements IInvokedMethodListener {

    public static final String IS_GATEKEEPER = "isGatekeeper";

    public static final String TRUE = "true";

    private static final String SKIP_MESSAGE = "Skipped because gatekeeper test in this suite failed";

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!isThisSuiteListener(testResult)) {
            return;
        }
        if (!AbstractTestNGSpringContextTests.class.equals(method.getTestMethod().getRealClass()) && hasFailedGatekeeperTest(testResult)) {
            throw new GatekeeperException(SKIP_MESSAGE);
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

    }

    private boolean hasFailedGatekeeperTest(ITestResult testResult) {
        return testResult.getTestContext().getSuite().getResults().values().stream().anyMatch(this::hasFailedGatekeeperTest);
    }

    private boolean hasFailedGatekeeperTest(ISuiteResult suiteResult) {
        ITestContext testContext = suiteResult.getTestContext();
        return !(testContext.getFailedTests().getAllResults().isEmpty() && testContext.getSkippedTests().getAllResults().isEmpty())
                && Objects.equals(testContext.getCurrentXmlTest().getParameter(IS_GATEKEEPER), TRUE);
    }
}
