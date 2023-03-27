package com.sequenceiq.cloudbreak.quartz.configuration;

public class SchedulerRuntimeException extends RuntimeException {

    public SchedulerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
