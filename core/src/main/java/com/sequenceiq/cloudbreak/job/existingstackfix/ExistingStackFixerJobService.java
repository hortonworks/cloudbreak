package com.sequenceiq.cloudbreak.job.existingstackfix;

import javax.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExistingStackFixerJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackFixerJobService.class);

    private static final String JOB_GROUP = "existing-stack-fixer-jobs";

    private static final String TRIGGER_GROUP = "existing-stack-fixer-triggers";

    private static final String LOCAL_ID = "localId";

    private static final String REMOTE_RESOURCE_CRN = "remoteResourceCrn";

    @Inject
    private ExistingStackFixerConfig properties;

    @Inject
    private Scheduler scheduler;

    public void schedule(ExistingStackFixerJobAdapter resource) {
        String localId = resource.getLocalId();
        JobDetail jobDetail = buildJobDetail(localId, resource.getRemoteResourceId());
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            if (scheduler.getJobDetail(JobKey.jobKey(localId, JOB_GROUP)) != null) {
                unschedule(localId);
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    public void unschedule(String id) {
        JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
            if (scheduler.getJobKeys(GroupMatcher.groupEquals(JOB_GROUP)).isEmpty()) {
                LOGGER.info("All existing stacks have been fixed, hooray!");
            }
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(String stackId, String crn) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(LOCAL_ID, stackId);
        jobDataMap.put(REMOTE_RESOURCE_CRN, crn);
        return JobBuilder.newJob(ExistingStackFixerJob.class)
                .withIdentity(stackId, JOB_GROUP)
                .withDescription("Fixing existing stack: " + crn)
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for fixing existing stack")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(properties.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }
}
