package com.sequenceiq.cloudbreak.polling;

public class AttemptBasedTimeoutChecker implements TimeoutChecker {

    private final int maxAttempts;

    private int attempt;

    public AttemptBasedTimeoutChecker(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean checkTimeout() {
        attempt++;
        if (maxAttempts < 0) {
            return false;
        }
        return attempt >= maxAttempts;
    }

    @Override
    public String toString() {
        return "AttemptBasedTimeoutChecker{" +
                "maxAttempts=" + maxAttempts +
                ", attempt=" + attempt +
                '}';
    }
}
