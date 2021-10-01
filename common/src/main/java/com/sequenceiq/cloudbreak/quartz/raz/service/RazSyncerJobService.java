package com.sequenceiq.cloudbreak.quartz.raz.service;

import javax.inject.Inject;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.quartz.model.JobResourceAdapter;
import com.sequenceiq.cloudbreak.quartz.raz.RazSyncerConfig;

@Service
public class RazSyncerJobService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RazSyncerJobService.class);

    private static final String LOCAL_ID = "localId";

    private static final String REMOTE_RESOURCE_CRN = "remoteResourceCrn";

    private static final String JOB_GROUP = "raz-syncer-job-group";

    private static final String JOB_NAME = "raz-syncer-job";

    private static final String TRIGGER_GROUP = "raz-syncer-triggers";

    @Inject
    private RazSyncerConfig properties;

    @Inject
    private Scheduler scheduler;

    public <T> void schedule(JobResourceAdapter<T> resource) {
        JobDetail jobDetail = buildJobDetail(resource.getLocalId(), resource.getRemoteResourceId(), resource.getJobClassForResource());
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            unschedule();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", jobDetail), e);
        }
    }

    private <T> JobDetail buildJobDetail(String id, String crn, Class<? extends Job> clazz) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(LOCAL_ID, id);
        jobDataMap.put(REMOTE_RESOURCE_CRN, crn);
        return JobBuilder.newJob(clazz)
                .withIdentity(JOB_NAME, JOB_GROUP)
                .withDescription("Determining if Raz is manually installed on cluster")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Trigger for determining if Raz is manually installed")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(properties.getIntervalInSeconds())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }

    public void unschedule() {
        JobKey jobKey = new JobKey(JOB_NAME, JOB_GROUP);
        try {
            if (scheduler.getJobDetail(jobKey) != null) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", jobKey), e);
        }
    }
}
