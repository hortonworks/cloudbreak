package com.sequenceiq.cloudbreak.cloud.retry;

public interface ExceptionCheckTask {
    boolean check(Exception e);
}
