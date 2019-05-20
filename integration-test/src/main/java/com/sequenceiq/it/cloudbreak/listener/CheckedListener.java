package com.sequenceiq.it.cloudbreak.listener;

import java.util.Objects;

import org.testng.ITestResult;

public abstract class CheckedListener {
    protected boolean isThisSuiteListener(ITestResult testResult) {
        return testResult.getTestContext().getSuite().getXmlSuite().getListeners().stream().anyMatch(listener ->
                Objects.equals(listener, getClass().getName()));
    }
}
