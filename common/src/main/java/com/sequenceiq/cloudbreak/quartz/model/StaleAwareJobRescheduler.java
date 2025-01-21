package com.sequenceiq.cloudbreak.quartz.model;

public interface StaleAwareJobRescheduler {

    void rescheduleForStaleCluster(Long id);
}
