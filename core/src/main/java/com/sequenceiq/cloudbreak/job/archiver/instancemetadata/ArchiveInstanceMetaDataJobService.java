package com.sequenceiq.cloudbreak.job.archiver.instancemetadata;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class ArchiveInstanceMetaDataJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveInstanceMetaDataJobService.class);

    private static final String JOB_GROUP = "archive-instancemetadata-jobs";

    private static final String TRIGGER_GROUP = "archive-instancemetadata-triggers";

    @Inject
    private ArchiveInstanceMetaDataConfig properties;

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public void schedule(Long id) {
        ArchiveInstanceMetaDataJobAdapter resourceAdapter = new ArchiveInstanceMetaDataJobAdapter(id, applicationContext);
        JobDetail jobDetail = buildJobDetail(resourceAdapter);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resourceAdapter.getJobResource().getLocalId(), jobDetail, trigger);
    }

    public <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (properties.isArchiveInstanceMetaDataEnabled()) {
            try {
                JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
                if (scheduler.getJobDetail(jobKey) != null) {
                    LOGGER.info("Unscheduling instance metadata archiver job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                    unschedule(jobKey);
                }
                LOGGER.info("Scheduling instance metadata archiver job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error(String.format("Error during scheduling quartz job: %s", id), e);
            }
        }
    }

    public void schedule(ArchiveInstanceMetaDataJobAdapter resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        JobKey jobKey = jobDetail.getKey();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                unschedule(jobKey);
            }
            LOGGER.debug("Scheduling archive InstanceMetaData job for stack {}", resource.getJobResource().getRemoteResourceId());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during scheduling archive InstanceMetaData job: %s", jobDetail), e);
        }
    }

    public void unschedule(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
            if (scheduler.getJobKeys(GroupMatcher.groupEquals(JOB_GROUP)).isEmpty()) {
                LOGGER.info("All terminated InstanceMetaData older than a week have been archived, hooray!");
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(ArchiveInstanceMetaDataJobAdapter resource) {
        return JobBuilder.newJob(ArchiveInstanceMetaDataJob.class)
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Archiving InstanceMetadata for stack: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger archiving InstanceMetaData for existing stack")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(properties.getIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();
    }

    private Date delayedFirstStart() {
        int delayInMinutes = RandomUtil.getInt((int) TimeUnit.HOURS.toMinutes(properties.getIntervalInHours()));
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofMinutes(delayInMinutes)));
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
