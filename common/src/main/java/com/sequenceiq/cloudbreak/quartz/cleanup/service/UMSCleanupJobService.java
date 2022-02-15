package com.sequenceiq.cloudbreak.quartz.cleanup.service;

import javax.inject.Inject;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.quartz.cleanup.job.UMSCleanupJob;

public abstract class UMSCleanupJobService<T extends UMSCleanupJob> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UMSCleanupJobService.class);

    private static final String JOB_NAME = "ums-cleanup-job";

    private static final String JOB_GROUP = "ums-cleanup-job-group";

    private static final String TRIGGER_GROUP = "ums-cleanup-triggers";

    @Inject
    private Scheduler scheduler;

    @Inject
    private UMSCleanupConfig umsCleanupConfig;

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            LOGGER.info("Unscheduling UMS cleanup job with name: '{}' and group: '{}'", JOB_NAME, JOB_GROUP);
            unschedule();
            LOGGER.info("Scheduling UMS cleanup job with name: '{}' and group: '{}'", JOB_NAME, JOB_GROUP);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    public abstract Class<T> getJobClass();

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(getJobClass())
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Removing unused UMS resources (machine users)")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for removing unused UMS resources (machine users)")
                .withSchedule(CronScheduleBuilder.cronSchedule(umsCleanupConfig.getCronExpression()))
                .build();
    }

    public void unschedule() {
        JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }
}
