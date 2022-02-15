package com.sequenceiq.cloudbreak.structuredevent.job;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

import javax.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;

@Service
public class StructuredSynchronizerJobService {

    private static final String JOB_GROUP = "structured-synchronizer-jobs";

    private static final String TRIGGER_GROUP = "structured-synchronizer-triggers";

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJobService.class);

    private static final Random RANDOM = new SecureRandom();

    private static final int SECONDS_IN_HOUR = 3600;

    @Inject
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Inject
    private Scheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public <T> void schedule(Long id, Class<? extends JobResourceAdapter<T>> resource) {
        try {
            Constructor<? extends JobResourceAdapter<T>> c = resource.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter<T> resourceAdapter = c.newInstance(id, applicationContext);
            JobDetail jobDetail = buildJobDetail(resourceAdapter);
            Trigger trigger = buildJobTrigger(jobDetail);
            schedule(resourceAdapter.getLocalId(), jobDetail, trigger);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
        }
    }

    public <T> void scheduleWithDelay(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTriggerWithDelay(jobDetail);
        schedule(resource.getLocalId(), jobDetail, trigger);
    }

    public <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (structuredSynchronizerConfig.isStructuredSyncEnabled()) {
            try {
                JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling structured event sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                    unschedule(id);
                }
                LOGGER.info("Scheduling structured event sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (SchedulerException e) {
                LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
            }
        }
    }

    public void unschedule(String id) {
        try {
            JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
            LOGGER.info("Unscheduling instance metadata archiver job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", id), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resourceAdapter) {
        return JobBuilder.newJob(resourceAdapter.getJobClassForResource())
                .withIdentity(resourceAdapter.getLocalId(), JOB_GROUP)
                .withDescription("Creating Structured Synchronization Event")
                .usingJobData(resourceAdapter.toJobDataMap())
                .storeDurably()
                .build();
    }

    protected Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Checking datalake status Trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(structuredSynchronizerConfig.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    protected Trigger buildJobTriggerWithDelay(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Checking datalake status Trigger")
                .startAt(delayedStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(structuredSynchronizerConfig.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedStart() {
        return Date.from(ZonedDateTime.now().toInstant().plus(
                Duration.ofSeconds(RANDOM.nextInt(structuredSynchronizerConfig.getIntervalInHours() * SECONDS_IN_HOUR))));
    }
}
