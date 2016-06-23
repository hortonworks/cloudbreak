package com.sequenceiq.it

import org.testng.ITestContext
import org.testng.ITestResult
import org.testng.internal.IResultListener2

class ThreadLocalTestListener : IResultListener2 {
    private val logReporter = object : ThreadLocal<VerboseLogReporter>() {
        override fun initialValue(): VerboseLogReporter {
            return VerboseLogReporter()
        }
    }

    override fun beforeConfiguration(tr: ITestResult) {
        getLogReporter().beforeConfiguration(tr)
    }

    override fun onConfigurationSuccess(itr: ITestResult) {
        getLogReporter().onConfigurationSuccess(itr)
    }

    override fun onConfigurationFailure(itr: ITestResult) {
        getLogReporter().onConfigurationFailure(itr)
    }

    override fun onConfigurationSkip(itr: ITestResult) {
        getLogReporter().onConfigurationSkip(itr)
    }

    override fun onTestStart(result: ITestResult) {
        getLogReporter().onTestStart(result)
    }

    override fun onTestSuccess(result: ITestResult) {
        getLogReporter().onTestSuccess(result)
    }

    override fun onTestFailure(result: ITestResult) {
        getLogReporter().onTestFailure(result)
    }

    override fun onTestSkipped(result: ITestResult) {
        getLogReporter().onTestSkipped(result)
    }

    override fun onTestFailedButWithinSuccessPercentage(result: ITestResult) {
        getLogReporter().onTestFailedButWithinSuccessPercentage(result)
    }

    override fun onStart(context: ITestContext) {
        getLogReporter().onStart(context)
    }

    override fun onFinish(context: ITestContext) {
        getLogReporter().onFinish(context)
    }

    private fun getLogReporter(): VerboseLogReporter {
        return logReporter.get()
    }
}
