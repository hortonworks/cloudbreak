package com.sequenceiq.cloudbreak.quartz.statuschecker.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Random;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;

@Service
public class StatusCheckerJobService {

    public static final String SYNC_JOB_TYPE = "syncJobType";

    public static final String LONG_SYNC_JOB_TYPE = "longSyncJobType";

    private static final String JOB_GROUP = "status-checker-jobs";

    private static final String TRIGGER_GROUP = "status-checker-triggers";

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCheckerJobService.class);

    private static final Random RANDOM = new SecureRandom();

    @Inject
    private StatusCheckerConfig statusCheckerConfig;

    @Inject
    private Scheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public <T> void schedule(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail, RANDOM.nextInt(statusCheckerConfig.getIntervalInSeconds()), statusCheckerConfig.getIntervalInSeconds());
        schedule(jobDetail, trigger, resource.getLocalId());
    }

    public <T> void schedule(JobResourceAdapter<T> resource, int delayInSeconds) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail, delayInSeconds, statusCheckerConfig.getIntervalInSeconds());
        schedule(jobDetail, trigger, resource.getLocalId());
    }

    public void schedule(Long id, Class<? extends JobResourceAdapter<?>> resource) {
        try {
            Constructor<? extends JobResourceAdapter<?>> c = resource.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter<?> resourceAdapter = c.newInstance(id, applicationContext);
            schedule(resourceAdapter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
        }
    }

    public <T> void scheduleLongIntervalCheck(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource, Map.of(SYNC_JOB_TYPE, LONG_SYNC_JOB_TYPE));
        Trigger trigger = buildJobTrigger(jobDetail, statusCheckerConfig.getIntervalInSeconds(), statusCheckerConfig.getLongIntervalInSeconds());
        schedule(jobDetail, trigger, resource.getLocalId());
    }

    public void unschedule(String id) {
        try {
            scheduler.deleteJob(JobKey.jobKey(id, JOB_GROUP));
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", id), e);
        }
    }

    public void deleteAll() {
        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            LOGGER.error("Error during clearing quartz jobs", e);
        }
    }

    private void schedule(JobDetail jobDetail, Trigger trigger, String localId) {
        try {
            if (scheduler.getJobDetail(JobKey.jobKey(localId, JOB_GROUP)) != null) {
                unschedule(localId);
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", localId), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource) {
        return buildJobDetail(resource, Map.of());
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource, Map<String, String> dataMap) {
        JobDataMap jobDataMap = resource.toJobDataMap();
        jobDataMap.putAll(dataMap);

        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getLocalId(), JOB_GROUP)
                .withDescription("Checking datalake status Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, int delayInSeconds, int intervalInSeconds) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Checking datalake status Trigger")
                .startAt(delayedStart(delayInSeconds))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(intervalInSeconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedStart(int delayInSeconds) {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(delayInSeconds)));
    }
}
