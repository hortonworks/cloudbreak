package com.sequenceiq.cloudbreak.structuredevent.job;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import org.quartz.Job;
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

@Service
public class StructuredSynchronizerJobService {

    private static final String JOB_GROUP = "structured-synchronizer-jobs";

    private static final String TRIGGER_GROUP = "structured-synchronizer-triggers";

    private static final String LOCAL_ID = "localId";

    private static final String REMOTE_RESOURCE_CRN = "remoteResourceCrn";

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredSynchronizerJobService.class);

    @Inject
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Inject
    private Scheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public <T> void schedule(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource.getLocalId(), resource.getRemoteResourceId(), resource.getJobClassForResource());
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            if (scheduler.getJobDetail(JobKey.jobKey(resource.getLocalId(), JOB_GROUP)) != null) {
                unschedule(resource.getLocalId());
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", resource.getLocalId()), e);
        }
    }

    public void schedule(Long id, Class<? extends JobResourceAdapter> resource) {
        try {
            Constructor<? extends JobResourceAdapter> c = resource.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter resourceAdapter = c.newInstance(id, applicationContext);
            schedule(resourceAdapter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
        }
    }

    public void unschedule(String id) {
        try {
            scheduler.deleteJob(JobKey.jobKey(id, JOB_GROUP));
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", id), e);
        }
    }

    private <T> JobDetail buildJobDetail(String sdxId, String crn, Class<? extends Job> clazz) {
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put(LOCAL_ID, sdxId);
        jobDataMap.put(REMOTE_RESOURCE_CRN, crn);

        return JobBuilder.newJob(clazz)
                .withIdentity(sdxId, JOB_GROUP)
                .withDescription("Creating Structured Synchronization Event")
                .usingJobData(jobDataMap)
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
}
