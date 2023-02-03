package com.sequenceiq.consumption.job.storage;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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

import com.sequenceiq.cloudbreak.common.mappable.StorageType;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ConsumptionType;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.service.ConsumptionService;

@Service
public class StorageConsumptionJobService implements JobSchedulerService {

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

    @Inject
    private ConsumptionService consumptionService;

    public void schedule(Long id) {
        StorageConsumptionJobAdapter resourceAdapter = new StorageConsumptionJobAdapter(id, applicationContext);
        scheduleIfNotScheduled(resourceAdapter);
    }

    public void schedule(Consumption consumption) {
        if (consumptionService.isAggregationRequired(consumption)) {
            scheduleWithAggregation(consumption);
        } else {
            schedule(consumption.getId());
        }
    }

    private void scheduleIfNotScheduled(StorageConsumptionJobAdapter resource) {
        if (storageConsumptionConfig.isStorageConsumptionEnabled()) {
            JobDetail jobDetail = buildJobDetail(resource);
            Trigger trigger = buildJobTrigger(jobDetail);
            try {
                JobKey jobKey = JobKey.jobKey(resource.getJobResource().getLocalId(), JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Skipping scheduling, storage consumption job with key: '{}' and group: '{}' is already scheduled",
                            jobKey.getName(), jobKey.getGroup());
                } else {
                    LOGGER.info("Scheduling storage consumption job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                    scheduler.scheduleJob(jobDetail, trigger);
                }
            } catch (SchedulerException e) {
                LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
            }
        }
    }

    private void scheduleWithAggregation(Consumption consumption) {
        String storageLocation = consumption.getStorageLocation();
        String environmentCrn = consumption.getEnvironmentCrn();
        StorageType storageType = consumption.getConsumptionType().getStorageService();
        ConsumptionType consumptionType = consumption.getConsumptionType();
        try {
            List<Consumption> consumptionsForSameEnvAndBucket = consumptionService
                    .findAllStorageConsumptionForEnvCrnAndBucketName(environmentCrn, storageLocation, storageType, consumptionType);
            if (isJobRunningForConsumptions(consumptionsForSameEnvAndBucket)) {
                LOGGER.info("Skipping scheduling as aggregated storage consumption job is already running for " +
                                "environment CRN '{}' and bucket name of location '{}'.", environmentCrn, storageLocation);
            } else {
                LOGGER.info("Scheduling aggregated storage consumption job for environment CRN '{}' and bucket name of location '{}'. " +
                                "Using consumption with CRN '{}' and ID '{}'.",
                        environmentCrn, storageLocation, consumption.getResourceCrn(), consumption.getId());
                schedule(consumption.getId());
            }
        } catch (SchedulerException e) {
            LOGGER.error("Error during getting aggregated storage consumption job details " +
                            "for environment CRN '{}' and bucket name of location '{}'. Reason: {}",
                    environmentCrn, storageLocation, e.getMessage(), e);
        }
    }

    public void unschedule(Consumption consumption) {
        if (consumptionService.isAggregationRequired(consumption)) {
            unscheduleWithAggregation(consumption);
        } else {
            unschedule(consumption.getId());
        }
    }

    private void unschedule(Long id) {
        JobKey jobKey = JobKey.jobKey(id.toString(), JOB_GROUP);
        try {
            LOGGER.info("Unscheduling storage consumption job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private void unscheduleWithAggregation(Consumption consumption) {
        String storageLocation = consumption.getStorageLocation();
        String environmentCrn = consumption.getEnvironmentCrn();
        StorageType storageType = consumption.getConsumptionType().getStorageService();
        ConsumptionType consumptionType = consumption.getConsumptionType();
        try {
            if (isJobRunningForConsumption(consumption)) {
                LOGGER.info("Unscheduling aggregated storage consumption job for environment CRN '{}' and bucket name of location '{}'. " +
                                "Using consumption with CRN '{}' and ID '{}'.",
                        environmentCrn, storageLocation, consumption.getResourceCrn(), consumption.getId());
                unschedule(consumption.getId());
                Optional<Consumption> nextConsumptionForGroupOptional = consumptionService
                        .findAllStorageConsumptionForEnvCrnAndBucketName(environmentCrn, storageLocation, storageType, consumptionType)
                        .stream()
                        .filter(cons -> !cons.getId().equals(consumption.getId()))
                        .findFirst();
                if (nextConsumptionForGroupOptional.isPresent()) {
                    Consumption nextConsumptionForGroup = nextConsumptionForGroupOptional.get();
                    LOGGER.info("Consumptions are still present for environment CRN '{}' and bucket name of location '{}'." +
                                    "Rescheduling aggregated storage consumption job. " +
                                    "Using consumption with CRN '{}' and ID '{}'.",
                            environmentCrn, storageLocation, nextConsumptionForGroup.getResourceCrn(), nextConsumptionForGroup.getId());
                    schedule(nextConsumptionForGroup.getId());
                }
            } else {
                LOGGER.info("Skipping unscheduling as no job is running for consumption with CRN '{}' and ID '{}'.",
                        consumption.getResourceCrn(), consumption.getId());
            }
        } catch (SchedulerException e) {
            LOGGER.error("Error during getting aggregated storage consumption job details " +
                            "for environment CRN '{}' and bucket name of location '{}'. Reason: {}",
                    environmentCrn, storageLocation, e.getMessage(), e);
        }
    }

    private boolean isJobRunningForConsumption(Consumption consumption) throws SchedulerException {
        return scheduler.getJobDetail(JobKey.jobKey(consumption.getId().toString(), JOB_GROUP)) != null;
    }

    private boolean isJobRunningForConsumptions(List<Consumption> consumptions) throws SchedulerException {
        for (Consumption consumption : consumptions) {
            if (isJobRunningForConsumption(consumption)) {
                return true;
            }
        }
        return false;
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

    private Date delayedFirstStart() {
        int delayInSeconds = RANDOM.nextInt((int) TimeUnit.MINUTES.toSeconds(storageConsumptionConfig.getIntervalInMinutes()));
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(delayInSeconds)));
    }

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }
}