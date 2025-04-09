package com.sequenceiq.cloudbreak.job.provider;

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
public class ProviderSyncJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncJobService.class);

    private static final String JOB_GROUP = "provider-sync-jobs";

    private static final String TRIGGER_GROUP = "provider-sync-triggers";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private ProviderSyncConfig properties;

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
        ProviderSyncJobAdapter resourceAdapter = new ProviderSyncJobAdapter(id, applicationContext);
        JobDetail jobDetail = buildJobDetail(resourceAdapter);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resourceAdapter.getJobResource().getLocalId(), jobDetail, trigger);
    }

    public <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (properties.isProviderSyncEnabled()) {
            try {
                JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling provider sync checker job for stack with key: '{}' and group: '{}'",
                            jobKey.getName(), jobKey.getGroup());
                    deregister(jobKey);
                }
                LOGGER.info("Scheduling provider sync checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling quartz job: {}", id, e);
            }
        }
    }

    public void schedule(ProviderSyncJobAdapter resource) {
        if (properties.isProviderSyncEnabled()) {
            JobDetail jobDetail = buildJobDetail(resource);
            JobKey jobKey = jobDetail.getKey();
            Trigger trigger = buildJobTrigger(jobDetail);
            try {
                if (scheduler.getJobDetail(jobKey) != null) {
                    deregister(jobKey);
                }
                LOGGER.debug("Scheduling provider sync job for stack {}", resource.getJobResource().getRemoteResourceId());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling provider sync job: {}", jobDetail, e);
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

    private JobDetail buildJobDetail(ProviderSyncJobAdapter resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Provider sync: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger provider sync.")
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