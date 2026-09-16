package com.sequenceiq.maintenance.dispatcher.scheduled;

import java.time.Duration;
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

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class MaintenanceWindowDispatcherJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowDispatcherJobService.class);

    private static final String JOB_NAME = "maintenance-window-dispatcher-job";

    private static final String JOB_GROUP = "maintenance-window-dispatcher-job-group";

    private static final String TRIGGER_GROUP = "maintenance-window-dispatcher-triggers";

    private static final int MAX_INITIAL_SPREAD_MINUTES = 15;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private MaintenanceWindowDispatcherConfig dispatcherConfig;

    @Inject
    private Clock clock;

    public void schedule() {
        if (!dispatcherConfig.isEnabled()) {
            LOGGER.info("Maintenance window dispatcher job is disabled; skipping schedule");
            return;
        }
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling maintenance window dispatcher job key: '{}' and group: '{}'",
                        jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling maintenance window dispatcher job for key: '{}' and group: '{}'",
                    jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error("Error during scheduling quartz job: {}", jobDetail, e);
        }
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
        try {
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling quartz job: {}", jobKey, e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(MaintenanceWindowDispatcherJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Maintenance window dispatcher tick")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for maintenance window dispatcher tick")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(effectiveIntervalMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private int effectiveIntervalMinutes() {
        return Math.max(1, dispatcherConfig.getIntervalInMinutes());
    }

    private Date delayedFirstStart() {
        int spreadMinutes = Math.min(effectiveIntervalMinutes(), MAX_INITIAL_SPREAD_MINUTES);
        long delayMinutes = RandomUtil.getInt(spreadMinutes);
        return Date.from(clock.getCurrentInstant().plus(Duration.ofMinutes(delayMinutes)));
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
