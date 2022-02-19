package com.sequenceiq.flow.cleanup;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

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
import org.springframework.stereotype.Service;

@Service
public class FlowCleanupJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowCleanupJobService.class);

    private static final String JOB_NAME = "flow-cleanup-job";

    private static final String JOB_GROUP = "flow-cleanup-job-group";

    private static final String TRIGGER_GROUP = "flow-cleanup-triggers";

    private static final Random RANDOM = new SecureRandom();

    @Inject
    private Scheduler scheduler;

    @Inject
    private FlowCleanupConfig flowCleanupConfig;

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling flow cleanup job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling flow cleanup job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(FlowCleanupJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Removing finalized flows and flowchains")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for removing finalized flows and flowchains")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(flowCleanupConfig.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private Date delayedFirstStart() {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofHours(RANDOM.nextInt(flowCleanupConfig.getIntervalInHours()))));
    }
}
