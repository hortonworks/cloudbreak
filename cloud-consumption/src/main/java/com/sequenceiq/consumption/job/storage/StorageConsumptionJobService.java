package com.sequenceiq.consumption.job.storage;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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

@Service
public class StorageConsumptionJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageConsumptionJobService.class);

    private static final String JOB_GROUP = "storage-consumption-job-group";

    private static final String TRIGGER_GROUP = "storage-consumption-job-triggers";

    private static final Random RANDOM = new SecureRandom();

    @Inject
    private  ApplicationContext applicationContext;

    @Inject
    private Scheduler scheduler;

    @Inject
    private StorageConsumptionConfig storageConsumptionConfig;

    public void schedule(StorageConsumptionJobAdapter resource) {
        if (storageConsumptionConfig.isStorageConsumptionEnabled()) {
            JobDetail jobDetail = buildJobDetail(resource);
            Trigger trigger = buildJobTrigger(jobDetail);
            try {
                JobKey jobKey = JobKey.jobKey(resource.getJobResource().getLocalId(), JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling storage consumption job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                    unschedule(jobKey.getName());
                }
                LOGGER.info("Scheduling storage consumption job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (SchedulerException e) {
                LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
            }
        }

    }

    public void schedule(Long id) {
        StorageConsumptionJobAdapter resourceAdapter = new StorageConsumptionJobAdapter(id, applicationContext);
        schedule(resourceAdapter);
    }

    private JobDetail buildJobDetail(StorageConsumptionJobAdapter resource) {
        JobDataMap jobDataMap = resource.toJobDataMap();
        return JobBuilder.newJob(StorageConsumptionJob.class)
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Getting storage usage")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();

    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for getting storage usage")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(storageConsumptionConfig.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();

    }

    public void unschedule(String id) {
        JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
        try {
            LOGGER.info("Unscheduling storage consumption job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }

    }

    private Date delayedFirstStart() {
        int delayInSeconds = RANDOM.nextInt((int) TimeUnit.MINUTES.toSeconds(storageConsumptionConfig.getIntervalInMinutes()));
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(delayInSeconds)));
    }
}