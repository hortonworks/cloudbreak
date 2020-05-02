package com.sequenceiq.it.cloudbreak.context;

import org.slf4j.Logger;
import org.testng.SkipException;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public enum TestErrorLog {
    IGNORE(false, null),
    FAIL(true, true),
    SKIP(true, false);

    private Boolean report;

    private Boolean fail;

    TestErrorLog(Boolean report, Boolean fail) {
        this.report = report;
        this.fail = fail;
    }

    void report(Logger logger, String message) {
        if (report) {
            Log.validateError(logger, message);
            if (fail) {
                Log.error(logger, message);
                throw new TestFailException(message);
            } else {
                Log.error(logger, message);
                throw new SkipException(message);
            }
        }
    }
}
