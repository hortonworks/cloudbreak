package com.sequenceiq.environment.environment.sync;

import java.util.Optional;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.FlowLogService;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class EnvironmentStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStatusCheckerJob.class);

    private final EnvironmentService environmentService;

    private final FlowLogService flowLogService;

    private final EnvironmentSyncService environmentSyncService;

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentJobService environmentJobService;

    private final AutoSyncConfig autoSyncConfig;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public EnvironmentStatusCheckerJob(EnvironmentService environmentService, FlowLogService flowLogService,
            EnvironmentSyncService environmentSyncService, EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentJobService environmentJobService, AutoSyncConfig autoSyncConfig, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        super(tracer, "Environment Status Checker Job");
        this.environmentService = environmentService;
        this.flowLogService = flowLogService;
        this.environmentSyncService = environmentSyncService;
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.environmentJobService = environmentJobService;
        this.autoSyncConfig = autoSyncConfig;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    @Override
    protected Object getMdcContextObject() {
        return environmentService.findEnvironmentById(getLocalIdAsLong()).orElse(null);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Long envId = getLocalIdAsLong();
        Optional<Environment> environmentOpt = environmentService.findEnvironmentById(envId);
        if (environmentOpt.isPresent()) {
            Environment environment = environmentOpt.get();
            if (flowLogService.isOtherFlowRunning(envId)) {
                LOGGER.info("EnvironmentStatusCheckerJob cannot run, because flow is running for environment: {}", environment.getName());
            } else {
                syncAnEnv(environment);
            }
        } else {
            environmentJobService.unschedule(envId);
            LOGGER.warn("EnvironmentStatusCheckerJob cannot run, because environment is not found with id: {}. This env is unscheduled now", envId);
        }
    }

    @VisibleForTesting
    void syncAnEnv(Environment environment) {
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> {
                EnvironmentStatus status = environmentSyncService.getStatusByFreeipa(environment);
                if (environment.getStatus() != status) {
                    if (!flowLogService.isOtherFlowRunning(environment.getId())) {
                        updateIfEnabled(environment, status);
                    } else {
                        LOGGER.info("EnvironmentStatusCheckerJob wants to update the status but it's ignored because a flow started on: {}",
                                environment.getName());
                    }
                } else {
                    LOGGER.info("Environment status is the same ({}), the update is skipped", status);
                }
            });
        } catch (Exception e) {
            LOGGER.info("Environment sync is failed for {}, error: {}", environment.getName(), e.getMessage(), e);
        }
    }

    private void updateIfEnabled(Environment environment, EnvironmentStatus status) {
        if (autoSyncConfig.isUpdateStatus()) {
            environmentStatusUpdateService.updateEnvironmentStatusAndNotify(environment, status, ResourceEvent.ENVIRONMENT_SYNC_FINISHED);
        } else {
            LOGGER.info("The environment status would be had to update from {} to {}", environment.getStatus(), status);
        }
    }
}
