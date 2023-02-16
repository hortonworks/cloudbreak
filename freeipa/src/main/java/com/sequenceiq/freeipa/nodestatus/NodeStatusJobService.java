package com.sequenceiq.freeipa.nodestatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.quartz.JobSchedulerService;
import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.util.RandomUtil;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class NodeStatusJobService implements JobSchedulerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusJobService.class);

    private static final String JOB_NAME = "node-status-checker-job";

    private static final String JOB_GROUP = "node-status-checker-group";

    private static final String TRIGGER_GROUP = "node-status-check-triggers";

    @Inject
    private Scheduler scheduler;

    @Inject
    private NodeStatusJobConfig nodeStatusJobConfig;

    @Inject
    private ApplicationContext applicationContext;

    public void schedule(Long id) {
        if (nodeStatusJobConfig.isEnabled()) {
            schedule(id, NodeStatusJobAdapter.class);
        }
    }

    public <T> void schedule(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        Trigger trigger = buildJobTrigger(jobDetail, RandomUtil.getInt(nodeStatusJobConfig.getIntervalInSeconds()));
        try {
            LOGGER.info("Unscheduling Node status job with name: '{}' and group: '{}'", JOB_NAME, JOB_GROUP);
            unschedule(resource.getJobResource().getLocalId());
            LOGGER.info("Scheduling Node status job with name: '{}' and group: '{}'", JOB_NAME, JOB_GROUP);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    public void schedule(Long id, Class<? extends JobResourceAdapter<?>> resourceAdapterClass) {
        try {
            Constructor<? extends JobResourceAdapter> c = resourceAdapterClass.getConstructor(Long.class, ApplicationContext.class);
            JobResourceAdapter resourceAdapter = c.newInstance(id, applicationContext);
            schedule(resourceAdapter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Error during scheduling quartz job: {}", id, e);
        }
    }

    public void unschedule(String id) {
        JobKey jobKey = JobKey.jobKey(id, JOB_GROUP);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    public void unschedule(Stack stack) {
        unschedule(String.valueOf(stack.getId()));
    }

    private <T> JobDetail buildJobDetail(JobResourceAdapter<T> resource) {
        return JobBuilder.newJob(getJobClass())
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Checking stack nodestatus Job")
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, int delayedInSeconds) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .usingJobData(jobDetail.getJobDataMap())
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Checking nodestatus Trigger")
                .startAt(delayedStart(delayedInSeconds))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(nodeStatusJobConfig.getIntervalInSeconds())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    private Date delayedStart(int delayInSeconds) {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(delayInSeconds)));
    }

    private Class<? extends Job> getJobClass() {
        return NodeStatusJob.class;
    }

    @Override
    public String getJobGroup() {
        return JOB_GROUP;
    }
}
