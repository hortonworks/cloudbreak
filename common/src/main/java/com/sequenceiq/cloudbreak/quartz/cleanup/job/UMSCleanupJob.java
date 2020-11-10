package com.sequenceiq.cloudbreak.quartz.cleanup.job;

import com.sequenceiq.cloudbreak.quartz.TracedQuartzJob;

import io.opentracing.Tracer;

public abstract class UMSCleanupJob extends TracedQuartzJob {

    protected UMSCleanupJob(Tracer tracer, String jobName) {
        super(tracer, jobName);
    }
}
