package com.sequenceiq.cloudbreak.quartz;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class MdcQuartzJobTestClass extends MdcQuartzJob {

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {

    }

    public void fillMdcContext(JobExecutionContext context) {
        super.fillMdcContext(context);
    }
}
