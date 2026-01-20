package com.sequenceiq.cloudbreak.quartz.saltstatuschecker;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.quartz.JobDataMapProvider;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.util.RandomUtil;

public abstract class SaltStatusCheckerJobService<T extends JobResourceAdapter<?>> implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltStatusCheckerJobService.class);

    private static final String JOB_GROUP = "stack-salt-status-checker-jobs";

    private static final String TRIGGER_GROUP = "stack-salt-status-checker-triggers";

    @Inject
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private JobDataMapProvider jobDataMapProvider;

    public void schedule(T resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        JobKey jobKey = jobDetail.getKey();
        try {
            Trigger trigger = buildJobTrigger(jobDetail);
            if (scheduler.getJobDetail(jobKey) != null) {
                unschedule(jobKey);
            }
            LOGGER.info("Scheduling stack salt status job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error("Error during scheduling stack salt status job: {}", jobDetail, e);
        }
    }

    public void unschedule(JobKey jobKey) {
        try {
            LOGGER.info("Unscheduling stack salt status job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(T resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Checking salt status of stack: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(jobDataMapProvider.provide(resource))
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for stack salt status checker job")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(saltStatusCheckerConfig.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .startAt(delayedStart())
                .build();
    }

    private Date delayedStart() {
        int intervalInSeconds = (int) TimeUnit.MINUTES.toSeconds(saltStatusCheckerConfig.getIntervalInMinutes());
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(RandomUtil.getInt(intervalInSeconds))));
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
