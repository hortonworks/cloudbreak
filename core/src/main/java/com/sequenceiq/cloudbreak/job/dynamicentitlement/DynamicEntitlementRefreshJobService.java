package com.sequenceiq.cloudbreak.job.dynamicentitlement;

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
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class DynamicEntitlementRefreshJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicEntitlementRefreshJobService.class);

    private static final String JOB_GROUP = "dynamic-entitlement-jobs";

    private static final String TRIGGER_GROUP = "dynamic-entitlement-triggers";

    private static final int MAX_ERROR_COUNT = 11;

    @Qualifier("DynamicEntitlementRefreshTransactionalScheduler")
    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private DynamicEntitlementRefreshConfig properties;

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
        DynamicEntitlementRefreshJobAdapter resourceAdapter = new DynamicEntitlementRefreshJobAdapter(id, applicationContext);
        JobDetail jobDetail = buildJobDetail(resourceAdapter);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resourceAdapter.getJobResource().getLocalId(), jobDetail, trigger);
    }

    public void reScheduleWithBackoff(Long id, JobDetail jobDetail, int errorCount) {
        Trigger trigger = buildJobTriggerWithBackoff(jobDetail, errorCount, id);
        schedule(id.toString(), jobDetail, trigger);
    }

    public void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (properties.isDynamicEntitlementEnabled()) {
            try {
                JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling dynamic entitlement checker job for stack with key: '{}' and group: '{}'",
                            jobKey.getName(), jobKey.getGroup());
                    unschedule(jobKey);
                }
                LOGGER.info("Scheduling dynamic entitlement checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
            }
        }
    }

    public void schedule(DynamicEntitlementRefreshJobAdapter resource) {
        if (properties.isDynamicEntitlementEnabled()) {
            JobDetail jobDetail = buildJobDetail(resource);
            JobKey jobKey = jobDetail.getKey();
            Trigger trigger = buildJobTrigger(jobDetail);
            try {
                if (scheduler.getJobDetail(jobKey) != null) {
                    unschedule(jobKey);
                }
                LOGGER.debug("Scheduling dynamic entitlement checker job for stack {}", resource.getJobResource().getRemoteResourceId());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error(String.format("Error during scheduling dynamic entitlement checker job: %s", jobDetail), e);
            }
        }
    }

    public void unschedule(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(DynamicEntitlementRefreshJobAdapter resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Checking dynamic entitlements: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger checking dynamic entitlements.")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(properties.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Trigger buildJobTriggerWithBackoff(JobDetail jobDetail, int errorCount, Long stackId) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger checking dynamic entitlements.")
                .startAt(backoffFirstStart(errorCount, stackId))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(properties.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date backoffFirstStart(int errorCount, Long stackId) {
        if (errorCount == 0) {
            LOGGER.debug("DynamicEntitlementRefreshJob will be rescheduled in {} minutes for stack {}.", properties.getIntervalInMinutes(), stackId);
            return Date.from(clock.getCurrentDateLowPrecision().toInstant().plus(Duration.ofMinutes(properties.getIntervalInMinutes())));
        } else {
            int count = Math.min(errorCount, MAX_ERROR_COUNT);
            // backoff strategy, delay = 2^(errorcount+1) + original interval
            int exponentialBackOffInMinutes = (2 << count) + properties.getIntervalInMinutes();
            LOGGER.debug("DynamicEntitlementRefreshJob will be rescheduled in {} minutes for stack {}.", exponentialBackOffInMinutes, stackId);
            return Date.from(clock.getCurrentDateLowPrecision().toInstant().plus(Duration.ofMinutes(exponentialBackOffInMinutes)));
        }
    }

    private Date delayedFirstStart() {
        return Date.from(clock.getCurrentDateLowPrecision().toInstant().plus(Duration.ofMinutes(RandomUtil.getInt(properties.getIntervalInMinutes()))));
    }
}
