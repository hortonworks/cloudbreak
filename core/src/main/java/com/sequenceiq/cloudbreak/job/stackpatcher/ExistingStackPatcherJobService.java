package com.sequenceiq.cloudbreak.job.stackpatcher;

import javax.inject.Inject;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;

@Service
public class ExistingStackPatcherJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExistingStackPatcherJobService.class);

    private static final String JOB_GROUP = "existing-stack-patcher-jobs";

    private static final String TRIGGER_GROUP = "existing-stack-patcher-triggers";

    @Inject
    private Scheduler scheduler;

    @Inject
    private StackService stackService;

    @Inject
    private ExistingStackPatcherServiceProvider existingStackPatcherServiceProvider;

    public void schedule(Long stackId, StackPatchType stackPatchType) {
        JobResource jobResource = stackService.getJobResource(stackId);
        schedule(new ExistingStackPatcherJobAdapter(jobResource, stackPatchType));
    }

    public void schedule(ExistingStackPatcherJobAdapter resource) {
        JobDetail jobDetail = buildJobDetail(resource);
        JobKey jobKey = jobDetail.getKey();
        StackPatchType stackPatchType = resource.getStackPatchType();
        try {
            ExistingStackPatchService existingStackPatchService = existingStackPatcherServiceProvider.provide(stackPatchType);
            Trigger trigger = buildJobTrigger(jobDetail, existingStackPatchService);
            if (scheduler.getJobDetail(jobKey) != null) {
                LOGGER.info("Unscheduling stack patcher job for stack with key: '{}' and group: '{}'", jobKey.getName(), jobKey.getGroup());
                unschedule(jobKey);
            }
            LOGGER.info("Scheduling stack patcher {} job for stack with key: '{}' and group: '{}'",
                    stackPatchType, jobKey.getName(), jobKey.getGroup());
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (UnknownStackPatchTypeException e) {
            LOGGER.error("Failed to get stack patcher for type {}", stackPatchType, e);
        } catch (SchedulerException e) {
            LOGGER.error("Error during scheduling stack patcher job: {}", jobDetail, e);
        }
    }

    public void unschedule(JobKey jobKey) {
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
            if (scheduler.getJobKeys(GroupMatcher.groupEquals(JOB_GROUP)).isEmpty()) {
                LOGGER.info("All existing stacks have been patched, hooray!");
            }
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }

    private JobDetail buildJobDetail(ExistingStackPatcherJobAdapter resource) {
        return JobBuilder.newJob(ExistingStackPatcherJob.class)
                .withIdentity(resource.getJobResource().getLocalId(), JOB_GROUP)
                .withDescription("Patching existing stack: " + resource.getJobResource().getRemoteResourceId())
                .usingJobData(resource.toJobDataMap())
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, ExistingStackPatchService existingStackPatchService) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for existing stack patch " + existingStackPatchService.getStackPatchType().name())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(existingStackPatchService.getIntervalInMinutes())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithExistingCount())
                .startAt(existingStackPatchService.getFirstStart())
                .build();
    }
}
