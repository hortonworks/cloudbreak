package com.sequenceiq.cloudbreak.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryUtil implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryUtil.class);

    private static final int DEFAULT_NUMBER_OF_RETRIES = 3;

    private int retryCount;

    private RetryTask task;

    private ErrorTask errorHandler;

    private ExceptionCheckTask exceptionCheck;

    private CheckTask check;

    private RetryUtil(int count) {
        retryCount = count;
    }

    public static RetryUtil withDefaultRetries() {
        return new RetryUtil(DEFAULT_NUMBER_OF_RETRIES);
    }

    public static RetryUtil withRetries(int count) {
        return new RetryUtil(count);
    }

    public RetryUtil retry(RetryTask task) {
        this.task = task;
        return this;
    }

    public RetryUtil retryIfFalse(CheckTask check) {
        this.check = check;
        return this;
    }

    public RetryUtil checkIfRecoverable(ExceptionCheckTask check) {
        exceptionCheck = check;
        return this;
    }

    public RetryUtil ifNotRecoverable(ErrorTask errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    @Override
    public void run() {
        try {
            retryCount--;
            task.run();
            runChecker();
        } catch (RuntimeException e) {
            runExceptionChecker(e);
        }
    }

    private void runRetry() {
        if (retryCount > 0) {
            run();
        } else {
            runErrorHandler(new RetryException("too many retries"));
        }
    }

    private void runChecker() {
        if (check != null && !check.check()) {
            runRetry();
        }
    }

    private void runExceptionChecker(Exception e) {
        if (exceptionCheck == null || exceptionCheck.check(e)) {
            runRetry();
        } else {
            runErrorHandler(e);
        }
    }

    private void runErrorHandler(Exception e) {
        try {
            if (errorHandler != null) {
                errorHandler.run(e);
            }
        } catch (Exception ex) {
            LOGGER.debug("ErrorHandler failed during retries.", ex);
        }
    }

}
