package com.sequenceiq.cdp.databus.quartz;

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
import org.springframework.stereotype.Component;

@Component
public class DatabusCredentialCleanupJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusCredentialCleanupJobService.class);

    private static final String JOB_NAME = "databus-credential-cleanup-job";

    private static final String JOB_GROUP = "databus-credential-cleanup-job-group";

    private static final String TRIGGER_GROUP = "databus-credential-cleanup-triggers";

    private final Scheduler scheduler;

    private final DatabusCredentialCleanuplJobConfig databusCredentialCleanuplJobConfig;

    public DatabusCredentialCleanupJobService(Scheduler scheduler, DatabusCredentialCleanuplJobConfig databusCredentialCleanuplJobConfig) {
        this.scheduler = scheduler;
        this.databusCredentialCleanuplJobConfig = databusCredentialCleanuplJobConfig;
    }

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            unschedule();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
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

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(DatabusCredentialCleanupJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Removing unused account databus credential resources (access keys)")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for removing unused account databus credential resources (access keys)")
                .withSchedule(CronScheduleBuilder.cronSchedule(databusCredentialCleanuplJobConfig.getCronExpression()))
                .build();
    }
}
