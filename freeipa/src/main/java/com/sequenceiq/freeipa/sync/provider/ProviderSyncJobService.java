package com.sequenceiq.freeipa.sync.provider;

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
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class ProviderSyncJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncJobService.class);

    private static final String JOB_GROUP = "provider-sync-jobs";

    private static final String TRIGGER_GROUP = "provider-sync-triggers";

    private static final String UNKNOWN_PROVIDER_NAME = "UNKNOWN";

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

    public void schedule(Stack stack) {
        if (shouldSync(stack.getCloudPlatform())) {
            scheduleJob(new ProviderSyncJobAdapter(stack.getId(), applicationContext));
        }
    }

    public void schedule(ProviderSyncJobAdapter resource) {
        String provider = resource.getJobResource().getProvider().orElse(UNKNOWN_PROVIDER_NAME);
        if (shouldSync(provider)) {
            scheduleJob(resource);
        }
    }

    private boolean shouldSync(String provider) {
        if (!properties.isProviderSyncEnabled()) {
            return false;
        }
        boolean shouldSync = properties.getEnabledProviders().contains(provider);
        LOGGER.debug("Should sync: {}, provider: {}, enabled providers: {}", shouldSync, provider, properties.getEnabledProviders());
        return shouldSync;
    }

    private void scheduleJob(ProviderSyncJobAdapter resource) {
        try {
            JobDetail jobDetail = buildJobDetail(resource);
            JobKey jobKey = jobDetail.getKey();
            if (scheduler.getJobDetail(jobKey) != null) {
                deregister(jobKey);
            }
            scheduler.scheduleJob(jobDetail, buildJobTrigger(jobDetail));
            LOGGER.debug("Scheduled FreeIPA provider sync job for stack {}", resource.getJobResource().getRemoteResourceId());
        } catch (Exception e) {
            LOGGER.error("Error scheduling FreeIPA provider sync job", e);
        }
    }

    public void deregister(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error("Error unscheduling quartz job: {}", jobKey, e);
        }
    }

    private JobDetail buildJobDetail(ProviderSyncJobAdapter resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("FreeIPA provider sync: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger FreeIPA provider sync.")
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
