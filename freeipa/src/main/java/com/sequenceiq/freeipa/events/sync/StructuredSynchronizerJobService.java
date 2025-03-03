package com.sequenceiq.freeipa.events.sync;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Date;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class StructuredSynchronizerJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJobService.class);

    private static final String JOB_GROUP = "structured-synchronizer-jobs";

    private static final String TRIGGER_GROUP = "structured-synchronizer-triggers";

    @Inject
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private Clock clock;

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return scheduler;
    }

    public <T> void schedule(JobResourceAdapter<T> resource, boolean withDelay) {
        if (structuredSynchronizerConfig.isStructuredSyncEnabled()) {
            JobDetail jobDetail = buildJobDetail(resource);
            Trigger trigger = withDelay ? buildJobTriggerWithDelay(jobDetail) : buildJobTrigger(jobDetail);
            String id = resource.getJobResource().getLocalId();
            try {
                JobKey jobKey = jobDetail.getKey();
                if (scheduler.getJobDetail(jobKey) != null) {
                    unschedule(id);
                }
                LOGGER.info("Scheduling structured event sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
            }
        } else {
            LOGGER.info("Structured sync is disabled, not scheduling job for stack with key: '{}'", resource.getJobResource().getLocalId());
        }
    }

    public <T> void schedule(Long id, Class<? extends JobResourceAdapter<T>> resource, boolean withDelay) {
        try {
            Constructor<? extends JobResourceAdapter<T>> c = resource.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter<T> resourceAdapter = c.newInstance(id, applicationContext);
            schedule(resourceAdapter, withDelay);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
        }
    }

    public void unschedule(String id) {
        try {
            JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
            LOGGER.info("Unscheduling structured event sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", id), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resourceAdapter) {
        return JobBuilder.newJob(resourceAdapter.getJobClassForResource())
                .withIdentity(resourceAdapter.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Creating Freeipa Structured Synchronization Event")
                .usingJobData(resourceAdapter.toJobDataMap())
                .storeDurably()
                .build();
    }

    protected Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Freeipa Structured Synchronization Event Trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(structuredSynchronizerConfig.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    protected Trigger buildJobTriggerWithDelay(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Freeipa Structured Synchronization Event Trigger")
                .startAt(delayedStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(structuredSynchronizerConfig.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedStart() {
        long randomDelay = RandomUtil.getLong(Duration.ofHours(structuredSynchronizerConfig.getIntervalInHours()).toSeconds());
        return clock.getDateForDelayedStart(Duration.ofSeconds(randomDelay));
    }
}
