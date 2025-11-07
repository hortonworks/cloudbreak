package com.sequenceiq.notification.scheduled.send.base;

import static com.sequenceiq.cloudbreak.util.TimeUtil.ONE_HOUR_IN_MINUTES;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.MdcQuartzJob;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobInitializer;
import com.sequenceiq.notification.domain.Notification;
import com.sequenceiq.notification.service.NotificationSendingService;

public abstract class ScheduledBaseNotificationSenderJobService extends MdcQuartzJob implements JobSchedulerService, JobInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledBaseNotificationSenderJobService.class);

    private final TransactionalScheduler scheduler;

    private final NotificationSendingService sendingService;

    public ScheduledBaseNotificationSenderJobService(TransactionalScheduler scheduler, NotificationSendingService sendingService) {
        this.scheduler = scheduler;
        this.sendingService = sendingService;
    }

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(getJobName(), getJobGroup());
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling scheduled notification job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling scheduled notification job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error("Error during scheduling quartz job: {}", jobDetail, e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(className())
                .withIdentity(getJobName(), getJobGroup())
                .withDescription("Register notifiable environments")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), getJobTriggers())
                .withDescription("Trigger for notifiable environments")
                .startAt(delayedFirstStart());
        if (cron().isPresent()) {
            triggerBuilder.withSchedule(cronSchedule(cron().get())
                    .withMisfireHandlingInstructionDoNothing());
        } else if (intervalInHours() > 0) {
            triggerBuilder.withSchedule(simpleSchedule()
                    .withIntervalInMinutes(intervalInHours() * ONE_HOUR_IN_MINUTES)
                    .repeatForever()
                    .withMisfireHandlingInstructionIgnoreMisfires());
        }
        return triggerBuilder.build();
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(getJobName(), getJobGroup());
        try {
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling quartz job: {}", jobKey, e);
        }
    }

    @Override
    public void initJobs() {
        if (enabled()) {
            schedule();
        }
    }

    private Date delayedFirstStart() {
        return Date.from(Instant.now().plus(Duration.ofMinutes(ONE_HOUR_IN_MINUTES)));
    }

    private String getJobName() {
        return String.format("notification-%s-sender-job", periodName());
    }

    private String getJobTriggers() {
        return String.format("notification-%s-sender-job-triggers", periodName());
    }

    @Override
    public String getJobGroup() {
        return String.format("notification-%s-sender-job-group", periodName());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        sendingService.processAndSend(data());
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return scheduler;
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    protected abstract String periodName();

    protected abstract Class<? extends Job> className();

    protected abstract int intervalInHours();

    protected abstract Optional<String> cron();

    protected abstract boolean enabled();

    protected abstract Collection<Notification> data();
}
