package com.sequenceiq.datalake.service.sdx;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

import javax.inject.Inject;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.job.DatalakeStatusCheckerJob;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Service
public class SdxJobService {

    public static final String JOB_GROUP = "datalake-jobs";

    public static final String TRIGGER_GROUP = "datalake-triggers";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxJobService.class);

    private static final int RANDOM_DELAY = 100;

    private static final Random RANDOM = new SecureRandom();

    @Value("${datalake.autosync.intervalsec:60}")
    private int intervalInSeconds;

    @Inject
    private Scheduler scheduler;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public void schedule(SdxCluster cluster) {
        JobDetail jobDetail = buildJobDetail(cluster.getId().toString(), cluster.getStackCrn());
        Trigger trigger = buildJobTrigger(jobDetail);
        try {
            if (scheduler.getJobDetail(JobKey.jobKey(cluster.getId().toString(), JOB_GROUP)) != null) {
                unschedule(cluster.getId());
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during scheduling quartz job: %s", cluster.getId()), e);
        }
    }

    public void schedule(Long sdxId) {
        sdxClusterRepository
                .findById(sdxId)
                .ifPresent(cluster -> schedule(cluster));
    }

    public void unschedule(Long sdxId) {
        try {
            scheduler.deleteJob(JobKey.jobKey(sdxId.toString(), JOB_GROUP));
        } catch (SchedulerException e) {
            LOGGER.error(String.format("Error during unscheduling quartz job: %s", sdxId), e);
        }
    }

    public void deleteAll() {
        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            LOGGER.error("Error during clearing quartz jobs", e);
        }
    }

    private JobDetail buildJobDetail(String sdxId, String crn) {
        JobDataMap jobDataMap = new JobDataMap();

        jobDataMap.put("sdxId", sdxId);
        jobDataMap.put("stackCrn", crn);

        return JobBuilder.newJob(DatalakeStatusCheckerJob.class)
                .withIdentity(sdxId, JOB_GROUP)
                .withDescription("Checking datalake status Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), TRIGGER_GROUP)
                .withDescription("Checking datalake status Trigger")
                .startAt(delayedFirstStart())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(intervalInSeconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();
    }

    private Date delayedFirstStart() {
        return Date.from(ZonedDateTime.now().toInstant().plus(Duration.ofSeconds(RANDOM.nextInt(RANDOM_DELAY))));
    }
}
