package com.sequenceiq.it.cloudbreak.performance;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IResultMap;
import org.testng.ITestContext;
import org.testng.ITestResult;

import com.sequenceiq.it.cloudbreak.context.MeasuredTestContext;

public class Util {

    public static final String KEY_PERFORMANCE_INDICATOR = "keyPerformanceIndicator";

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {
    }

    public static KeyPerformanceIndicator getKeyPerformance(Measure allMeasurement) {
        return BasicStatistic.build(allMeasurement);
    }

    public static KeyPerformanceIndicator getKeyPerformance(ITestContext iTestContext) {
        return getKeyPerformance(collectMeasurements(iTestContext));
    }

    public static Measure collectMeasurements(ITestContext iTestContext) {
        if (iTestContext == null) {
            throw new IllegalArgumentException("No testng testcontext is given.");
        }
        IResultMap failed = iTestContext.getFailedTests();
        IResultMap success = iTestContext.getPassedTests();
        MeasureAll allMeasurement = new MeasureAll();
        failed.getAllResults().stream().forEach(
                getiTestResultConsumer(allMeasurement)
        );
        success.getAllResults().stream().forEach(
                getiTestResultConsumer(allMeasurement)
        );

        return allMeasurement;
    }

    public static void writeToFile(Measure measure, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (PerformanceIndicator pi : measure.getAll()) {
                writer.write(pi + "\n");
            }
        } catch (IOException e) {
            LOGGER.error("Performance csv writing is failed", e);
        }
    }

    private static Consumer<ITestResult> getiTestResultConsumer(MeasureAll allMeasurement) {
        return result -> {
            Object[] param = result.getParameters();
            if (param.length > 0) {
                if (param[0] instanceof MeasuredTestContext) {
                    List<PerformanceIndicator> all = ((MeasuredTestContext) param[0]).getMeasure().getAll();
                    String testFullName = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
                    all.stream().forEach(pi -> pi.setTestName(testFullName));
                    allMeasurement.addAll(all);
                }
            }
        };
    }
}
