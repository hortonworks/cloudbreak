package com.sequenceiq.it.util;

import org.springframework.stereotype.Component;

@Component
public class TestNameExtractorUtil {

    private static final String TESTCASE_COMMON_PACKAGE_NAME = "testcase";

    public String getExecutingTestName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int i = 0;
        while (i < stackTrace.length && !stackTrace[i].getClassName().contains(TESTCASE_COMMON_PACKAGE_NAME)) {
            i++;
        }
        if (i < stackTrace.length) {
            return stackTrace[i].getMethodName();
        }
        return "";
    }
}
