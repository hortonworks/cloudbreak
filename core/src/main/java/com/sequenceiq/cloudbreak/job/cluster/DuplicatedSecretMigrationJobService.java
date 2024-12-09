package com.sequenceiq.cloudbreak.job.cluster;

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
public class DuplicatedSecretMigrationJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicatedSecretMigrationJobService.class);

    private static final String JOB_GROUP = "cluster-duplicated-secrets-migration-jobs";

    private static final String TRIGGER_GROUP = "cluster-duplicated-secrets-migration-triggers";

    @Inject
    private TransactionalScheduler scheduler;

    @Inject
    private DuplicatedSecretMigrationJobConfig properties;

    @Inject
    private ApplicationContext applicationContext;

    public void schedule(Long id) {
        DuplicatedSecretMigrationJobAdapter resourceAdapter = new DuplicatedSecretMigrationJobAdapter(id, applicationContext);
        JobDetail jobDetail = buildJobDetail(resourceAdapter);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resourceAdapter);
    }

    public void schedule(DuplicatedSecretMigrationJobAdapter resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        JobKey jobKey = jobDetail.getKey();
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                unschedule(jobKey);
            }
            LOGGER.debug("DuplicatedSecretMigrationJob for cluster {}", resource.getJobResource().getRemoteResourceId());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (Exception e) {
            LOGGER.error(String.format("Error during scheduling DuplicatedSecretMigrationJob: %s", jobDetail), e);
        }
    }

    public void unschedule(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
            if (scheduler.getJobKeys(GroupMatcher.groupEquals(JOB_GROUP)).isEmpty()) {
                LOGGER.info("All secret is migrated, hooray!");
            }
        } catch (Exception e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(DuplicatedSecretMigrationJobAdapter resource) {
        return JobBuilder.newJob(DuplicatedSecretMigrationJob.class)
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(properties.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();
    }

    private Date delayedFirstStart() {
        int delayInMinutes = RandomUtil.getInt((int) TimeUnit.MINUTES.toMinutes(properties.getIntervalInMinutes()));
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
