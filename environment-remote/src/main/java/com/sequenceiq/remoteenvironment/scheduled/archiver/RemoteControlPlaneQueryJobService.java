package com.sequenceiq.remoteenvironment.scheduled.archiver;

import java.time.Duration;
import java.time.ZonedDateTime;
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

@Service
public class RemoteControlPlaneQueryJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteControlPlaneQueryJobService.class);

    private static final String JOB_NAME = "query-job";

    private static final String JOB_GROUP = "query-job-group";

    private static final String TRIGGER_GROUP = "query-triggers";

    private static final int ONE_HOUR_IN_MINUTES = 60;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private RemoteControlPlaneQueryConfig remoteControlPlaneQueryConfig;

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling classic cluster query job key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling classic cluster query for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(RemoteControlPlaneQueryJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Query Classic clusters for new and removed private control plane configs")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for classic clusters query.")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(remoteControlPlaneQueryConfig.getIntervalInMintues())
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();
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
                .plus(Duration.ofMinutes(1)));
    }

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return this.scheduler;
    }
}
