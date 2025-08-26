package com.sequenceiq.cloudbreak.quartz.statuscleanup;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;

@Service
public class StackStatusCleanupJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackStatusCleanupJobService.class);

    private static final String JOB_NAME = "cleanup-stack-status-job";

    private static final String JOB_GROUP = "cleanup-stack-status-job-group";

    private static final String TRIGGER_GROUP = "cleanup-stack-status-triggers";

    private static final int TEN_MINUTES = 10;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private StackStatusCleanupConfig stackStatusCleanUpConfig;

    public boolean isCleanupEnabled() {
        return stackStatusCleanUpConfig.isEnabled();
    }

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        JobKey jobKey = jobDetail.getKey();
        Trigger trigger = buildJobTrigger(jobKey, true);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling stack status cleanup job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling stack status cleanup job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    public void reschedule() {
        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
        Trigger trigger = buildJobTrigger(jobKey, false);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Reschedule stack status cleanup job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                Date date = scheduler.rescheduleJob(trigger.getKey(), trigger);
                LOGGER.info("Stack status cleanup job next fire time: {}", date);
            } else {
                LOGGER.warn("Stack status cleanup job detail does not exists for job key: '{}' and group: '{}', schedule job.",
                        jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(buildJobDetail(), trigger);
            }
        } catch (Exception e) {
            LOGGER.error("Error during rescheduling stack status cleanup job for job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup(), e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(StackStatusCleanupJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Removing old stack statuses")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobKey jobKey, boolean delayedStart) {
        TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder.newTrigger()
                .forJob(jobKey)
                .withIdentity(jobKey.getName(), TRIGGER_GROUP)
                .withDescription("Trigger for removing old stack statuses")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule());
        if (delayedStart) {
            triggerBuilder.startAt(delayedFirstStart());
        } else {
            triggerBuilder.startAt(Date.from(ZonedDateTime.now().toInstant().plusSeconds(stackStatusCleanUpConfig.getIntervalInSeconds())));
        }
        return triggerBuilder.build();
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
        try {
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private Date delayedFirstStart() {
        return Date.from(
                ZonedDateTime.now()
                        .toInstant()
                        .plus(Duration.ofMinutes(TEN_MINUTES)));
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