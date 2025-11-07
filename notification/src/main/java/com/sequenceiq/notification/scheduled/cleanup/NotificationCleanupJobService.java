package com.sequenceiq.notification.scheduled.cleanup;

import static com.sequenceiq.cloudbreak.util.TimeUtil.ONE_HOUR_IN_MINUTES;
import static java.time.Duration.ofMinutes;
import static java.time.ZonedDateTime.now;
import static org.quartz.JobKey.jobKey;

import java.util.Date;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class NotificationCleanupJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationCleanupJobService.class);

    private static final String JOB_NAME = "notification-cleanup-job";

    private static final String JOB_GROUP = "notification-cleanup-job-group";

    private static final String TRIGGER_GROUP = "notification-cleanup-triggers";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private NotificationCleanupConfig notificationCleanupConfig;

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = jobKey(JOB_NAME, JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling notification cleanup job key: '{}' and group: '{}'",
                        jobKey.getName(),
                        jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling notification cleanup job for key: '{}' and group: '{}'",
                    jobKey.getName(),
                    jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error("Error during scheduling quartz job: {}", jobDetail, e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(NotificationCleanupJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Removing sent notifications")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for removing sent notifications")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(notificationCleanupConfig.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();
    }

    public boolean enabled() {
        return notificationCleanupConfig.enabled();
    }

    public void unschedule() {
        JobKey jobKey = jobKey(JOB_NAME, JOB_GROUP);
        try {
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling quartz job: {}", jobKey, e);
        }
    }

    private Date delayedFirstStart() {
        return Date.from(
                now().toInstant()
                        .plus(ofMinutes(RandomUtil.getInt(notificationCleanupConfig.getMaxDelayToStartInHours() * ONE_HOUR_IN_MINUTES))));
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
