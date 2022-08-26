package com.sequenceiq.datalake.service.sdx.poller;

public class PollerRunnerResult {

    private final boolean success;

    private final String message;

    private final Exception exception;

    private PollerRunnerResult(boolean success, Exception exception, String message) {
        this.success = success;
        this.exception = exception;
        this.message = message;
    }

    public static PollerRunnerResult ofSuccess() {
        return new PollerRunnerResult(true, null, null);
    }

    public static PollerRunnerResult ofError(Exception t, String message) {
        return new PollerRunnerResult(false, t, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public Exception getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "PollerRunnerResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", exception=" + exception +
                '}';
    }
}
