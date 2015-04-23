package com.sequenceiq.it;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.internal.IResultListener2;

public class ThreadLocalTestListener implements IResultListener2 {
    private ThreadLocal<VerboseLogReporter> logReporter = new ThreadLocal<VerboseLogReporter>() {
        @Override
        protected VerboseLogReporter initialValue() {
            return new VerboseLogReporter();
        }
    };

    @Override
    public void beforeConfiguration(ITestResult tr) {
        getLogReporter().beforeConfiguration(tr);
    }

    @Override
    public void onConfigurationSuccess(ITestResult itr) {
        getLogReporter().onConfigurationSuccess(itr);
    }

    @Override
    public void onConfigurationFailure(ITestResult itr) {
        getLogReporter().onConfigurationFailure(itr);
    }

    @Override
    public void onConfigurationSkip(ITestResult itr) {
        getLogReporter().onConfigurationSkip(itr);
    }

    @Override
    public void onTestStart(ITestResult result) {
        getLogReporter().onTestStart(result);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        getLogReporter().onTestSuccess(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        getLogReporter().onTestFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        getLogReporter().onTestSkipped(result);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        getLogReporter().onTestFailedButWithinSuccessPercentage(result);
    }

    @Override
    public void onStart(ITestContext context) {
        getLogReporter().onStart(context);
    }

    @Override public void onFinish(ITestContext context) {
        getLogReporter().onFinish(context);
    }

    private VerboseLogReporter getLogReporter() {
        return logReporter.get();
    }
}
