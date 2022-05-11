package com.sequenceiq.datalake.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class SdxRollForwardJob extends StatusCheckerJob {

    public SdxRollForwardJob(Tracer tracer, String jobName) {
        super(tracer, jobName);
    }

    @Override
    protected Object getMdcContextObject() {
        return null;
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {

    }
}
