package com.sequenceiq.cloudbreak.job.metering.sync;

import java.time.Duration;
import java.time.ZonedDateTime;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class MeteringSyncJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringSyncJobService.class);

    private static final String JOB_GROUP = "metering-sync-jobs";

    private static final String TRIGGER_GROUP = "metering-sync-triggers";

    @Inject
    private MeteringConfig meteringConfig;

    @Qualifier("MeteringSyncTransactionalScheduler")
    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public void scheduleIfNotScheduled(Long id) {
        JobKey jobKey = JobKey.jobKey(String.valueOf(id), JOB_GROUP);
        try {
            if (scheduler.getJobDetail(jobKey) == null) {
                schedule(id);
            } else {
                LOGGER.info("Metering sync job already scheduled with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            }
        } catch (Exception e) {
            LOGGER.error("Checking metering sync job details failed for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup(), e);
        }
    }

    public void schedule(Long id) {
        MeteringSyncJobAdapter resourceAdapter = new MeteringSyncJobAdapter(id, applicationContext);
        schedule(resourceAdapter);
    }

    public <T> void schedule(MeteringSyncJobAdapter resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resource.getJobResource().getLocalId(), jobDetail, trigger);
    }

    private <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (meteringConfig.isEnabled()) {
            JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
            try {
                if (scheduler.getJobDetail(jobKey) != null) {
                    unschedule(jobKey);
                }
                LOGGER.info("Scheduling metering sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling metering sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup(), e);
            }
        }
    }

    public void unschedule(String id) {
        unschedule(JobKey.jobKey(id, JOB_GROUP));
    }

    private void unschedule(JobKey jobKey) {
        try {
            LOGGER.info("Unscheduling metering sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling metering sync job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup(), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Metering sync job")
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Metering sync trigger")
                .startAt(delayedStart(meteringConfig.getSyncInitialDelayInSeconds()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(meteringConfig.getSyncIntervalInSeconds())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();
    }

    private Date delayedStart(int delayInSeconds) {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(RandomUtil.getInt(delayInSeconds))));
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
