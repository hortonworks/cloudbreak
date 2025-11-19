package com.sequenceiq.cloudbreak.quartz.statuschecker.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.statuschecker.StatusCheckerConfig;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class StatusCheckerJobService implements JobSchedulerService {

    public static final String SYNC_JOB_TYPE = "syncJobType";

    public static final String LONG_SYNC_JOB_TYPE = "longSyncJobType";

    private static final String JOB_GROUP = "status-checker-jobs";

    private static final String TRIGGER_GROUP = "status-checker-triggers";

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCheckerJobService.class);

    @Inject
    private StatusCheckerConfig statusCheckerConfig;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public <T> void schedule(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        int intervalInSeconds = statusCheckerConfig.getIntervalInSeconds();
        int randomizedDelay = statusCheckerConfig.getSnoozeSeconds() + getUniformlyDistributedDelay(intervalInSeconds);
        Trigger trigger = buildJobTrigger(jobDetail, resource.getJobResource(), randomizedDelay, intervalInSeconds);
        schedule(jobDetail, trigger, resource.getJobResource());
    }

    public <T> void schedule(JobResourceAdapter<T> resource, int delayInSeconds) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail, resource.getJobResource(), delayInSeconds, statusCheckerConfig.getIntervalInSeconds());
        schedule(jobDetail, trigger, resource.getJobResource());
    }

    public void schedule(Long id, Class<? extends JobResourceAdapter<?>> resourceAdapterClass) {
        try {
            Constructor<? extends JobResourceAdapter> c = resourceAdapterClass.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter resourceAdapter = c.newInstance(id, applicationContext);
            schedule(resourceAdapter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Error during scheduling quartz job: {}", id, e);
        }
    }

    public <T> void scheduleLongIntervalCheck(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource, Map.of(SYNC_JOB_TYPE, LONG_SYNC_JOB_TYPE));
        int longIntervalInSeconds = statusCheckerConfig.getLongIntervalInSeconds();
        int randomizedDelay = getUniformlyDistributedDelay(longIntervalInSeconds);
        Trigger trigger = buildJobTrigger(jobDetail, resource.getJobResource(), randomizedDelay, longIntervalInSeconds);
        schedule(jobDetail, trigger, resource.getJobResource());
    }

    public <T> void scheduleLongIntervalCheck(Long id, Class<? extends JobResourceAdapter<?>> resourceAdapterClass) {
        try {
            Constructor<? extends JobResourceAdapter> c = resourceAdapterClass.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter resourceAdapter = c.newInstance(id, applicationContext);
            scheduleLongIntervalCheck(resourceAdapter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Error during scheduling long quartz job: {}", id, e);
        }
    }

    public void unschedule(String id) {
        try {
            JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
            LOGGER.info("Unscheduling status checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.deleteJob(jobKey);
            LOGGER.info("Status checker job unscheduled with id: {}", id);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", id), e);
        }
    }

    public boolean isLongSyncJob(JobExecutionContext context) {
        return StatusCheckerJobService.LONG_SYNC_JOB_TYPE.equals(context.getMergedJobDataMap().get(StatusCheckerJobService.SYNC_JOB_TYPE));
    }

    private void schedule(JobDetail jobDetail, Trigger trigger, JobResource jobResource) {
        try {
            JobKey jobKey = JobKey.jobKey(jobResource.getLocalId(), JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling status checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule(jobResource.getLocalId());
            }
            LOGGER.info("Scheduling status checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
            LOGGER.info("Status checker job scheduled with id: {}, name: {}, crn: {}, type: {}", jobResource.getLocalId(), jobResource.getName(),
                    jobResource.getRemoteResourceId(), jobDetail.getJobDataMap().get(SYNC_JOB_TYPE));
        } catch (Exception e) {
            LOGGER.error("Error during scheduling quartz job. id: {}, name: {}, crn: {}", jobResource.getLocalId(), jobResource.getName(),
                    jobResource.getRemoteResourceId(), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource) {
        return buildJobDetail(resource, Map.of());
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource, Map<String, String> dataMap) {
        JobDataMap jobDataMap = resource.toJobDataMap();
        jobDataMap.putAll(dataMap);
        String resourceType = getResourceTypeFromCrnIfAvailable(resource.getJobResource());
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription(String.format("Checking %s status job", resourceType))
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private String getResourceTypeFromCrnIfAvailable(JobResource jobResource) {
        String remoteResourceId = jobResource.getRemoteResourceId();
        String resourceType = "unknown";
        if (Crn.isCrn(remoteResourceId)) {
            resourceType = Crn.safeFromString(remoteResourceId).getResource();
        }
        return resourceType;
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, JobResource jobResource, int delayInSeconds, int intervalInSeconds) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription(String.format("Checking %s status trigger", getResourceTypeFromCrnIfAvailable(jobResource)))
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

    private int getUniformlyDistributedDelay(int intervalInSeconds) {
        return RandomUtil.getQuickRandomInt(intervalInSeconds);
    }

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return scheduler;
    }
}
