package com.sequenceiq.it.cloudbreak.newway.log;

import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import java.util.Arrays;

public class ReportListener extends TestListenerAdapter {
    @Override
    public void onTestFailure(ITestResult tr) {
        Log.log(tr.getThrowable().getMessage());
        Log.log(Arrays.toString(tr.getThrowable().getStackTrace()));
    }
}
