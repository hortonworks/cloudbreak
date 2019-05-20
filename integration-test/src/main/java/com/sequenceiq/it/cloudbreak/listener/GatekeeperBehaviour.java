package com.sequenceiq.it.cloudbreak.listener;

import java.util.Map;
import java.util.Objects;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IResultMap;
import org.testng.ISuiteResult;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

public class GatekeeperBehaviour extends CheckedListener implements IInvokedMethodListener {

    private static final String IS_GATEKEEPER = "isGatekeeper";

    private static final String TRUE = "true";

    private static final String SKIP_MESSAGE = "Skipped because gatekeeper test in this suite failed";

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!isThisSuiteListener(testResult)) {
            return;
        }
        if (method.isTestMethod()) {
            ITestNGMethod thisMethod = method.getTestMethod();
            ITestNGMethod[] allTestMethods = testResult.getTestContext().getAllTestMethods();
            ITestNGMethod firstMethod = allTestMethods[0];

            if (thisMethod.equals(firstMethod)) {
                Map<String, ISuiteResult> results = testResult.getTestContext().getSuite().getResults();
                if (hasFailedGatekeeperTest(results)) {
                    throw new GatekeeperException(SKIP_MESSAGE);
                }
            } else {
                IResultMap skippedTests = testResult.getTestContext().getSkippedTests();
                if (anyTestsSkippedBecauseOfGatekeeper(skippedTests)) {
                    throw new GatekeeperException(SKIP_MESSAGE);
                }
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

    }

    private boolean anyTestsSkippedBecauseOfGatekeeper(IResultMap skippedTests) {
        return skippedTests.getAllResults().stream().anyMatch(result -> {
            return result.getThrowable() instanceof GatekeeperException;
        });
    }

    private boolean hasFailedGatekeeperTest(Map<String, ISuiteResult> results) {
        return results.values().stream().anyMatch(iSuiteResult -> {
            return iSuiteResult.getTestContext().getFailedTests().size() != 0
                    && Objects.equals(iSuiteResult.getTestContext().getCurrentXmlTest().getParameter(IS_GATEKEEPER), TRUE);
        });
    }
}
