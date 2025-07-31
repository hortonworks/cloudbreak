package com.sequenceiq.cloudbreak.job.cm;

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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class ClouderaManagerSyncJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerSyncJobService.class);

    private static final String JOB_GROUP = "cm-sync-jobs";

    private static final String TRIGGER_GROUP = "cm-sync-triggers";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ClouderaManagerSyncConfig properties;

    @Inject
    private Clock clock;

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return scheduler;
    }

    public void schedule(Long id) {
        ClouderaManagerSyncJobAdapter resourceAdapter = new ClouderaManagerSyncJobAdapter(id, applicationContext);
        JobDetail jobDetail = buildJobDetail(resourceAdapter);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resourceAdapter.getJobResource().getLocalId(), jobDetail, trigger);
    }

    private <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (properties.isClouderaManagerSyncEnabled()) {
            try {
                JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling cm sync checker job for stack with key: '{}' and group: '{}'",
                            jobKey.getName(), jobKey.getGroup());
                    deregister(jobKey);
                }
                LOGGER.info("Scheduling cm sync checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling quartz job: {}", id, e);
            }
        }
    }

    public void schedule(ClouderaManagerSyncJobAdapter resource) {
        if (properties.isClouderaManagerSyncEnabled()) {
            JobDetail jobDetail = buildJobDetail(resource);
            JobKey jobKey = jobDetail.getKey();
            Trigger trigger = buildJobTrigger(jobDetail);
            try {
                if (scheduler.getJobDetail(jobKey) != null) {
                    deregister(jobKey);
                }
                LOGGER.debug("Scheduling cm sync job for stack {}", resource.getJobResource().getRemoteResourceId());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling cm sync job: {}", jobDetail, e);
            }
        }
    }

    public void deregister(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling quartz job: {}", jobKey, e);
        }
    }

    private JobDetail buildJobDetail(ClouderaManagerSyncJobAdapter resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("cm sync: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger cm sync.")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(properties.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedFirstStart() {
        return Date.from(clock.getCurrentInstant()
                .plus(Duration.ofMinutes(RandomUtil.getInt(properties.getIntervalInMinutes()))));
    }
}