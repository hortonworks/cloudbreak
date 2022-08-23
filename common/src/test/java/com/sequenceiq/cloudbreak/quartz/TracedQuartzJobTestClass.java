package com.sequenceiq.cloudbreak.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.opentracing.Tracer;

public class TracedQuartzJobTestClass extends TracedQuartzJob {

    public TracedQuartzJobTestClass() {
        super(null, null);
    }

    public TracedQuartzJobTestClass(Tracer tracer, String jobName) {
        super(tracer, jobName);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {

    }

    public void fillMdcContext(JobExecutionContext context) {
        super.fillMdcContext(context);
    }
}
