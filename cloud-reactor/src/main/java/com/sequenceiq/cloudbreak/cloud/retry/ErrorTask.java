package com.sequenceiq.cloudbreak.cloud.retry;

public interface ErrorTask {
    void run(Exception e);
}
