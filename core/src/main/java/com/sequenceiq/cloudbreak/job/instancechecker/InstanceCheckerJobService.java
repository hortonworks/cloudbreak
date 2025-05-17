package com.sequenceiq.cloudbreak.job.instancechecker;

import java.time.Duration;
import java.util.Date;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.conf.InstanceCheckerConfig;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class InstanceCheckerJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceCheckerJobService.class);

    private static final String JOB_GROUP = "instance-checker-jobs";

    private static final String TRIGGER_GROUP = "instance-checker-triggers";

    @Inject
    private InstanceCheckerConfig instanceCheckerConfig;

    @Qualifier("InstanceCheckerTransactionalScheduler")
    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private Clock clock;

    public void scheduleIfNotScheduled(Long id) {
        JobKey jobKey = JobKey.jobKey(String.valueOf(id), JOB_GROUP);
        try {
            if (scheduler.getJobDetail(jobKey) == null) {
                schedule(id);
            } else {
                LOGGER.info("Instance checker job already scheduled with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            }
        } catch (Exception e) {
            LOGGER.error("Checking instance checker job details failed for stack with key: '{}' and group: '{}'",
                    jobKey.getName(), jobKey.getGroup(), e);
        }
    }

    public void schedule(Long id) {
        InstanceCheckerJobAdapter resourceAdapter = new InstanceCheckerJobAdapter(id, applicationContext);
        schedule(resourceAdapter);
    }

    public <T> void schedule(InstanceCheckerJobAdapter resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resource.getJobResource().getLocalId(), jobDetail, trigger);
    }

    private <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (instanceCheckerConfig.isEnabled()) {
            JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
            try {
                if (scheduler.getJobDetail(jobKey) != null) {
                    unschedule(jobKey);
                }
                LOGGER.info("Scheduling instance checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling instance checker job for stack with key: '{}' and group: '{}'",
                        jobKey.getName(), jobKey.getGroup(), e);
            }
        }
    }

    public void unschedule(String id) {
        unschedule(JobKey.jobKey(id, JOB_GROUP));
    }

    private void unschedule(JobKey jobKey) {
        try {
            LOGGER.info("Unscheduling instance checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling instance checker job for stack with key: '{}' and group: '{}'",
                    jobKey.getName(), jobKey.getGroup(), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Instance checker job")
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Instance checker trigger")
                .startAt(delayedStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(instanceCheckerConfig.getInstanceCheckerIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedStart() {
        long randomDelay = RandomUtil.getLong(Duration.ofSeconds(instanceCheckerConfig.getInstanceCheckerDelayInSeconds()).toSeconds());
        return clock.getDateForDelayedStart(Duration.ofSeconds(randomDelay));
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
