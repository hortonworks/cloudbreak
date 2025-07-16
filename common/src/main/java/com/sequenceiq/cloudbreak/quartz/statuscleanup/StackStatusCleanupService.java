package com.sequenceiq.cloudbreak.quartz.statuscleanup;

public interface StackStatusCleanupService {

    void cleanupByTimestamp(int limit, long timestampBefore);
}
