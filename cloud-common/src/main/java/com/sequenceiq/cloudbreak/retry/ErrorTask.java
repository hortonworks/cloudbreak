package com.sequenceiq.cloudbreak.retry;

public interface ErrorTask {
    void run(Exception e);
}
