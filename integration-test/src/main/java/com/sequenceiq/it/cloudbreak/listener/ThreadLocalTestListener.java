package com.sequenceiq.it.cloudbreak.listener;

import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.internal.IResultListener;

public class ThreadLocalTestListener extends TestNgListener implements IResultListener, ISuiteListener {

    private static final ThreadLocal<VerboseLogReporter> LOG_REPORTER = ThreadLocal.withInitial(VerboseLogReporter::new);

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
        Reporter.setCurrentTestResult(result);
        getLogReporter().onTestSuccess(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Reporter.setCurrentTestResult(result);
        getLogReporter().onTestFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        Reporter.setCurrentTestResult(result);
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
        return LOG_REPORTER.get();
    }
}
