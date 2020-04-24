package com.sequenceiq.it.cloudbreak.util.wait.service;

public class TimeoutChecker {

    private final int maxAttempts;

    private int attempt;

    public TimeoutChecker(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean checkTimeout() {
        attempt++;
        if (maxAttempts < 0) {
            return false;
        }
        return attempt >= maxAttempts;
    }
}
