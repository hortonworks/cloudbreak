package com.sequenceiq.it.cloudbreak.listener;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IResultMap;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.SkipException;

public class FirstLastTestExecutionBehaviour extends CheckedListener implements IInvokedMethodListener {
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (!isThisSuiteListener(testResult)) {
            return;
        }
        if (method.isTestMethod()) {
            ITestNGMethod thisMethod = method.getTestMethod();
            ITestNGMethod[] allTestMethods = testResult.getTestContext().getAllTestMethods();
            ITestNGMethod firstMethod = allTestMethods[0];
            ITestNGMethod lastMethod = allTestMethods[allTestMethods.length - 1];

            if (!thisMethod.equals(firstMethod) && !thisMethod.equals(lastMethod)) {
                IResultMap success = testResult.getTestContext().getPassedTests();
                if (!success.getAllMethods().contains(firstMethod)) {
                    throw new SkipException("Skipped because first method was not succcessfull");
                }
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

    }
}
