package com.sequenceiq.cloudbreak.quartz.statuschecker;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResourceCheckerJobListener extends JobListenerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCheckerJobListener.class);

    private static final String REMOTE_RESOURCE_CRN = "remoteResourceCrn";

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        if (jobDataMap.containsKey(REMOTE_RESOURCE_CRN)) {
            if (jobException == null) {
                LOGGER.debug("Job finished successfully in {} ms for resourceCrn: {}",
                        context.getJobRunTime(), jobDataMap.getString(REMOTE_RESOURCE_CRN));
            } else {
                LOGGER.warn("Job finished with error: {} in {} ms for resourceCrn: {}",
                        jobException.getMessage(), context.getJobRunTime(), jobDataMap.getString(REMOTE_RESOURCE_CRN));
            }
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
