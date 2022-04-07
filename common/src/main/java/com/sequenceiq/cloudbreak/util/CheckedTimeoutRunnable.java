package com.sequenceiq.cloudbreak.util;

import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface CheckedTimeoutRunnable {

    void run() throws TimeoutException;
}
