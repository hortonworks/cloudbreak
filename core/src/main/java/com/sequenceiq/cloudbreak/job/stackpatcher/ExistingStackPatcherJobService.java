package com.sequenceiq.cloudbreak.job.stackpatcher;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.quartz.JobBuilder;
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
public class ExistingStackPatcherJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobService.class);

    private static final String JOB_GROUP = "existing-stack-patcher-jobs";

    private static final String TRIGGER_GROUP = "existing-stack-patcher-triggers";

    private static final Random RANDOM = new SecureRandom();

    @Inject
    private ExistingStackPatcherConfig properties;

    @Inject
    private Scheduler scheduler;

    public void schedule(ExistingStackPatcherJobAdapter resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        JobKey jobKey = jobDetail.getKey();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                unschedule(jobKey);
            }
            LOGGER.debug("Scheduling stack patcher job for stack {}", resource.getRemoteResourceId());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling stack patcher job: %s", jobDetail), e);
        }
    }

    public void unschedule(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
            if (scheduler.getJobKeys(GroupMatcher.groupEquals(JOB_GROUP)).isEmpty()) {
                LOGGER.info("All existing stacks have been patched, hooray!");
            }
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(ExistingStackPatcherJobAdapter resource) {
        return JobBuilder.newJob(ExistingStackPatcherJob.class)
                .withIdentity(resource.getLocalId(), JOB_GROUP)
                .withDescription("Patching existing stack: " + resource.getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for patching existing stack")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(properties.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();
    }

    private Date delayedFirstStart() {
        int delayInMinutes = RANDOM.nextInt((int) TimeUnit.HOURS.toMinutes(properties.getIntervalInHours()));
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofMinutes(delayInMinutes)));
    }
}
