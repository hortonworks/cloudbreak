package com.sequenceiq.cloudbreak.util;

@FunctionalInterface
public interface CheckedRunnable {

    void run() throws Exception;
}
