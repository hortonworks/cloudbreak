package com.sequenceiq.consumption.job.storage;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.TracedQuartzJob;

import io.opentracing.Tracer;

@Component
public class StorageConsumptionJob extends TracedQuartzJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionJob.class);

    public StorageConsumptionJob(Tracer tracer) {
        super(tracer, "Storage Consumption Job");
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
    }

    @Override
    protected Object getMdcContextObject() {
        return null;
    }
}
