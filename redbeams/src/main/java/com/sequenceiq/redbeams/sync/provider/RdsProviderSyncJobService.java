package com.sequenceiq.redbeams.sync.provider;

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
public class RdsProviderSyncJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdsProviderSyncJobService.class);

    private static final String JOB_GROUP = "rds-provider-sync-jobs";

    private static final String TRIGGER_GROUP = "rds-provider-sync-triggers";

    private static final String UNKNOWN_PROVIDER_NAME = "UNKNOWN";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private RdsProviderSyncConfig config;

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
        schedule(new RdsProviderSyncJobAdapter(id, applicationContext));
    }

    public void schedule(RdsProviderSyncJobAdapter resource) {
        String provider = resource.getJobResource().getProvider().orElse(UNKNOWN_PROVIDER_NAME);
        if (shouldSync(provider)) {
            scheduleJob(resource);
        } else {
            LOGGER.debug("Skipping RDS provider sync scheduling for {}, provider {} is not enabled", resource.getJobResource().getLocalId(), provider);
        }
    }

    private boolean shouldSync(String provider) {
        return config.isEnabled() && config.getEnabledProviders().contains(provider);
    }

    private void scheduleJob(RdsProviderSyncJobAdapter resource) {
        try {
            JobDetail jobDetail = buildJobDetail(resource);
            JobKey jobKey = jobDetail.getKey();
            if (scheduler.getJobDetail(jobKey) != null) {
                deregister(jobKey);
            }
            scheduler.scheduleJob(jobDetail, buildJobTrigger(jobDetail));
            LOGGER.debug("Scheduled RDS provider sync job for DB stack {}", resource.getJobResource().getRemoteResourceId());
        } catch (Exception e) {
            LOGGER.error("Error scheduling RDS provider sync job", e);
        }
    }

    public void deregister(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
                LOGGER.info("Unscheduled RDS provider sync job: {}", jobKey);
            }
        } catch (Exception e) {
            LOGGER.error("Error unscheduling RDS provider sync job: {}", jobKey, e);
        }
    }

    public void unschedule(Long id) {
        deregister(JobKey.jobKey(String.valueOf(id), JOB_GROUP));
    }

    private JobDetail buildJobDetail(RdsProviderSyncJobAdapter resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("RDS provider sync: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger RDS provider sync.")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(config.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedFirstStart() {
        return Date.from(clock.getCurrentInstant()
                .plus(Duration.ofMinutes(RandomUtil.getInt(config.getIntervalInMinutes()))));
    }
}
