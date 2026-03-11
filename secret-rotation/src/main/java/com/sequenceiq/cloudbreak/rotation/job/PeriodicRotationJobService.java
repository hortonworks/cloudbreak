package com.sequenceiq.cloudbreak.rotation.job;

import java.time.Duration;
import java.util.Date;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class PeriodicRotationJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicRotationJobService.class);

    private static final String JOB_GROUP = "periodic-secret-rotation-job-group";

    private static final String TRIGGER_GROUP = "periodic-secret-rotation-triggers";

    private static final int MAX_INITIAL_SPREAD_MINUTES = 60;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private PeriodicRotationProperties periodicRotationProperties;

    @Inject
    private Clock clock;

    public void schedule(PeriodicRotationJobAdapter adapter) {
        try {
            JobDetail jobDetail = buildJobDetail(adapter);
            JobKey jobKey = jobDetail.getKey();
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.debug("Unscheduling existing periodic rotation job: {}:{}", jobKey.getGroup(), jobKey.getName());
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, buildJobTrigger(adapter, jobDetail));
            LOGGER.debug("Scheduled periodic rotation job for resource: {} ({}:{})",
                    adapter.getJobResource().getRemoteResourceId(), jobKey.getGroup(), jobKey.getName());
        } catch (Exception e) {
            LOGGER.error("Error scheduling periodic secret rotation job for resource {}",
                    adapter.getJobResource().getRemoteResourceId(), e);
        }
    }

    private JobDetail buildJobDetail(PeriodicRotationJobAdapter adapter) {
        return JobBuilder.newJob(adapter.getJobClassForResource())
                .withIdentity(adapter.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Periodic secret rotation job for resource " + adapter.getJobResource().getRemoteResourceId())
                .usingJobData(adapter.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(PeriodicRotationJobAdapter adapter, JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(TriggerKey.triggerKey(jobDetail.getKey().getName(), TRIGGER_GROUP))
                .withDescription("Trigger for periodic secret rotation for resource "
                    + jobDetail.getJobDataMap().getString(adapter.getJobResource().getRemoteResourceId()))
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(periodicRotationProperties.getScheduleIntervalMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedFirstStart() {
        int interval = Math.max(1, periodicRotationProperties.getScheduleIntervalMinutes());
        int spreadMinutes = Math.min(interval, MAX_INITIAL_SPREAD_MINUTES);
        long delayMinutes = spreadMinutes > 0 ? RandomUtil.getInt(spreadMinutes) : 0;
        return Date.from(clock.getCurrentInstant().plus(Duration.ofMinutes(delayMinutes)));
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

