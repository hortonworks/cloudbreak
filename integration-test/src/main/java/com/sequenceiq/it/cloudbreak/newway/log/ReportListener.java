package com.sequenceiq.it.cloudbreak.newway.log;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

public class ReportListener extends TestListenerAdapter {
    @Override
    public void onTestFailure(ITestResult tr) {
        Throwable throwable = tr.getThrowable();
        Log.log(throwable.getMessage());
    }
}
