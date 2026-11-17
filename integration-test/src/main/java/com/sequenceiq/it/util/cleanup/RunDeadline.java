package com.sequenceiq.it.util.cleanup;

/**
 * The deadline of a whole cleanup run (epoch millis): every wait of the run stops at the latest at this point, so the
 * sequential per-resource waits cannot add up beyond the CI stage timeout.
 * <p>
 * It also remembers whether the deadline actually ended a wait early ({@link #markCutShort()}, called by a wait only when
 * it returned TIMEOUT because of this deadline), so the end-of-run summary reports what happened instead of comparing
 * the clock with the deadline when it is printed.
 * One instance belongs to one run, see {@link CleanupReport}.
 */
class RunDeadline {

    private final long epochMillis;

    private boolean cutShort;

    RunDeadline(long epochMillis) {
        this.epochMillis = epochMillis;
    }

    /**
     * A deadline that never passes. A new instance on every call, because the cut short flag is mutable.
     *
     * @return  A run deadline at {@link Long#MAX_VALUE} epoch millis
     */
    static RunDeadline none() {
        return new RunDeadline(Long.MAX_VALUE);
    }

    long getEpochMillis() {
        return epochMillis;
    }

    void markCutShort() {
        cutShort = true;
    }

    boolean wasCutShort() {
        return cutShort;
    }
}
