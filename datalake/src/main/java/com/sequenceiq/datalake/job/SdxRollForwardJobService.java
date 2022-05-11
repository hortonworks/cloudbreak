package com.sequenceiq.datalake.job;

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
public class SdxRollForwardJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRollForwardJobService.class);

    private static final String JOB_NAME = "sdx-roll-forward-job";

    private static final String JOB_GROUP = "sdx-roll-forward-job-group";

    private static final String TRIGGER_GROUP = "sdx-roll-forward-triggers";

    private static final Random RANDOM = new SecureRandom();

    @Inject
    private Scheduler scheduler;

    @Inject
    private RollForwardConfig rollForwardConfig;

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling roll-forward job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling roll forward job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling roll-forward quartz job: %s", jobDetail), e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(SdxRollForwardJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Running roll-forward to keep CM and parcel info up-to-date in the DB")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for executing roll-forward")
                .startAt(delayedFirstStart())
//                .withSchedule(CronScheduleBuilder.cronSchedule(""))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(rollForwardConfig.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling automatic roll-forward quartz job: %s", jobKey), e);
        }
    }

    private Date delayedFirstStart() {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofHours(RANDOM.nextInt(rollForwardConfig.getIntervalInMinutes()))));
    }

}
