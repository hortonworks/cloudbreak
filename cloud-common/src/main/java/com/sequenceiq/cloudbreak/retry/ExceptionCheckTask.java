package com.sequenceiq.cloudbreak.retry;

public interface ExceptionCheckTask {
    boolean check(Exception e);
}
