package com.sequenceiq.cloudbreak.job.diskusage;

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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.RandomUtil;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class DiskUsageSyncJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskUsageSyncJobService.class);

    private static final String JOB_GROUP = "diskusage-sync-jobs";

    private static final String TRIGGER_GROUP = "diskusage-sync-triggers";

    private static final String UNKNOWN_PROVIDER_NAME = "UNKNOWN";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private DiskUsageSyncConfig properties;

    @Inject
    private Clock clock;

    @Inject
    private StackDtoService stackService;

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }

    @Override
    public TransactionalScheduler getScheduler() {
        return scheduler;
    }

    public void schedule(StackView stack) {
        if (properties.isDiskUsageSyncEnabled()) {
            scheduleJob(new DiskUsageSyncJobAdapter(stack.getId(), applicationContext));
        }
    }

    public void schedule(DiskUsageSyncJobAdapter resource) {
        if (properties.isDiskUsageSyncEnabled()) {
            scheduleJob(resource);
        }
    }

    private void scheduleJob(DiskUsageSyncJobAdapter resource) {
        String stackId = resource.getJobResource().getLocalId();
        StackDto stack = stackService.getById(Long.valueOf(stackId));

        if (stack.getType() == StackType.DATALAKE) {
            LOGGER.debug("Skip scheduling database disk usage sync job, stack is a Datalake: {}", stack.getResourceCrn());
        } else if (!stack.getDatabase().getExternalDatabaseAvailabilityType().isEmbedded()) {
            LOGGER.debug("Skip scheduling database disk usage sync job, external database is used for stack: {}", stack.getResourceCrn());
        } else {
            JobDetail jobDetail = buildJobDetail(resource);
            Trigger trigger = buildJobTrigger(jobDetail);
            try {
                JobKey jobKey = jobDetail.getKey();
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling disk usage sync checker job for stack with key: '{}' and group: '{}'",
                            jobKey.getName(), jobKey.getGroup());
                    deregister(jobKey);
                }
                LOGGER.debug("Scheduling disk usage sync job for stack {}", resource.getJobResource().getRemoteResourceId());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling disk usage sync job: {}", jobDetail, e);
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

    private JobDetail buildJobDetail(DiskUsageSyncJobAdapter resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Disk Usage sync: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger disk usage sync.")
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