package com.sequenceiq.remoteenvironment.scheduled;

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
public class PrivateEnvironmentBaseClusterRegistrarJobService implements JobSchedulerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrivateEnvironmentBaseClusterRegistrarJobService.class);

    private static final String JOB_NAME = "private-environment-basecluster-registrar-job";

    private static final String JOB_GROUP = "private-environment-basecluster-registrar-job-group";

    private static final String TRIGGER_GROUP = "private-environment-basecluster-registrar-triggers";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private PrivateEnvironmentBaseClusterRegistrarJobConfig jobConfig;

    public void schedule() {
        JobDetail jobDetail = buildJobDetail();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            JobKey jobKey = JobKey.jobKey(JOB_NAME, JOB_GROUP);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling private environment base cluster registrar job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule();
            }
            LOGGER.info("Scheduling private environment base cluster registrar job for key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    private JobDetail buildJobDetail() {
        JobDataMap jobDataMap = new JobDataMap();
        return JobBuilder.newJob(PrivateEnvironmentBaseClusterRegistrarJob.class)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Register base cluster of all available private environments of every private control plane")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for private environment base cluster registrar job.")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(jobConfig.getIntervalInMintues())
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
                        .plus(Duration.ofMinutes(jobConfig.getDelayedFirstStartInMinutes())));
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
