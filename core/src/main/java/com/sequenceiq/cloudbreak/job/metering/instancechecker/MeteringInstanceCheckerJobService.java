package com.sequenceiq.cloudbreak.job.metering.instancechecker;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.inject.Inject;

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

import com.sequenceiq.cloudbreak.job.metering.MeteringTransactionalScheduler;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.util.RandomUtil;

@Service
public class MeteringInstanceCheckerJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringInstanceCheckerJobService.class);

    private static final String JOB_GROUP = "metering-instance-checker-jobs";

    private static final String TRIGGER_GROUP = "metering-instance-checker-triggers";

    @Inject
    private MeteringConfig meteringConfig;

    @Inject
    private MeteringTransactionalScheduler scheduler;

    @Inject
    private ApplicationContext applicationContext;

    public void schedule(Long id, Class<? extends JobResourceAdapter<?>> resourceAdapterClass) {
        try {
            Constructor<? extends JobResourceAdapter> c = resourceAdapterClass.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter resourceAdapter = c.newInstance(id, applicationContext);
            schedule(resourceAdapter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Error during scheduling metering instance checker job: {}", id, e);
        }
    }

    public <T> void schedule(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail);
        schedule(resource.getJobResource().getLocalId(), jobDetail, trigger);
    }

    private <T> void schedule(String id, JobDetail jobDetail, Trigger trigger) {
        if (meteringConfig.isEnabled() && meteringConfig.isInstanceCheckerEnabled()) {
            JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
            try {
                if (scheduler.getJobDetail(jobKey) != null) {
                    unschedule(jobKey);
                }
                LOGGER.info("Scheduling metering instance checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                scheduler.scheduleJob(jobDetail, trigger);
            } catch (Exception e) {
                LOGGER.error("Error during scheduling metering instance checker job for stack with key: '{}' and group: '{}'",
                        jobKey.getName(), jobKey.getGroup(), e);
            }
        }
    }

    public void unschedule(String id) {
        unschedule(JobKey.jobKey(id, JOB_GROUP));
    }

    private void unschedule(JobKey jobKey) {
        try {
            LOGGER.info("Unscheduling metering instance checker job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (Exception e) {
            LOGGER.error("Error during unscheduling metering instance checker job for stack with key: '{}' and group: '{}'",
                    jobKey.getName(), jobKey.getGroup(), e);
        }
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource) {
        return JobBuilder.newJob(resource.getJobClassForResource())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Metering instance checker job")
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Metering instance checker trigger")
                .startAt(delayedStart(meteringConfig.getInstanceCheckerDelayInSeconds()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInHours(meteringConfig.getInstanceCheckerIntervalInHours())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .build();
    }

    private Date delayedStart(int delayInSeconds) {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(RandomUtil.getInt(delayInSeconds))));
    }

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }

}
