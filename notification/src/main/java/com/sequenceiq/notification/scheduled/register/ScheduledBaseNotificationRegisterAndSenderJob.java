package com.sequenceiq.notification.scheduled.register;

import static com.sequenceiq.cloudbreak.util.TimeUtil.ONE_HOUR_IN_MINUTES;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

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
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.service.NotificationSendingService;

public abstract class ScheduledBaseNotificationRegisterAndSenderJob extends MdcQuartzJob implements JobInitializer, JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledBaseNotificationRegisterAndSenderJob.class);

    private final TransactionalScheduler scheduler;

    private final NotificationSendingService notificationSendingService;

    public ScheduledBaseNotificationRegisterAndSenderJob(
            TransactionalScheduler scheduler,
            NotificationSendingService notificationSendingService) {
        this.scheduler = scheduler;
        this.notificationSendingService = notificationSendingService;
    }

    protected abstract String getName();

    protected abstract boolean enabled();

    protected abstract Collection<NotificationGeneratorDto> data();

    protected abstract NotificationType notificationType();

    protected Optional<Integer> intervalInHours() {
        return Optional.empty();
    }

    protected Optional<String> cron() {
        return Optional.of("0 0 0 ? * 1");
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.empty();
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) {
        NotificationGeneratorDtos notificationData = NotificationGeneratorDtos.builder()
                .notification(data())
                .notificationType(notificationType())
                .build();
        LOGGER.debug("Sending notifications: {}", notificationData);
        notificationSendingService.processAndImmediatelySend(notificationData);
    }

    @Override
    public void initJobs() {
        if (enabled()) {
            schedule();
        }
    }

    @Override
    public String getJobGroup() {
        return String.format("%s-notification-register-job-group", getName());
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return scheduler;
    }

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(getJobName(), getJobGroup());
            if (getScheduler().getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling notification job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling notification job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            getScheduler().scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(getClass())
                .withIdentity(getJobName(), getJobGroup())
                .withDescription("Register notifiable environments")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), getTriggerGroupName())
                .withDescription("Trigger for notifiable environments")
                .startAt(delayedFirstStart());
        if (cron().isPresent()) {
            triggerBuilder.withSchedule(cronSchedule(cron().get())
                    .withMisfireHandlingInstructionDoNothing());
        } else if (intervalInHours().isPresent()) {
            triggerBuilder.withSchedule(simpleSchedule()
                    .withIntervalInMinutes(intervalInHours().get() * ONE_HOUR_IN_MINUTES)
                    .repeatForever()
                    .withMisfireHandlingInstructionIgnoreMisfires());
        }
        return triggerBuilder.build();
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(getJobName(), getJobGroup());
        try {
            getScheduler().deleteJob(jobKey);
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling quartz job: {}", jobKey, e);
        }
    }

    private Date delayedFirstStart() {
        return Date.from(Instant.now().plus(Duration.ofMinutes(ONE_HOUR_IN_MINUTES)));
    }

    private String getJobName() {
        return String.format("%s-notification-register-job", getName());
    }

    private String getTriggerGroupName() {
        return String.format("%s-notification-register-triggers", getName());
    }
}
