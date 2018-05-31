package com.sequenceiq.it.cloudbreak.newway.listener;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class ReportListener extends TestListenerAdapter {
    @Override
    public void onTestFailure(ITestResult tr) {
        Throwable throwable = tr.getThrowable();
        Log.log(throwable.getMessage());
    }
}
