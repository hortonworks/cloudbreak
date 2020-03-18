package com.sequenceiq.environment.environment.sync;

import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.statuschecker.job.StatusCheckerJob;

@Component
public class EnvironmentStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentStatusCheckerJob.class);

    private final EnvironmentService environmentService;

    private final FlowLogService flowLogService;

    private final EnvironmentSyncService environmentSyncService;

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final EnvironmentJobService environmentJobService;

    private final AutoSyncConfig autoSyncConfig;

    public EnvironmentStatusCheckerJob(EnvironmentService environmentService, FlowLogService flowLogService,
            EnvironmentSyncService environmentSyncService, EnvironmentStatusUpdateService environmentStatusUpdateService,
            EnvironmentJobService environmentJobService, AutoSyncConfig autoSyncConfig) {
        this.environmentService = environmentService;
        this.flowLogService = flowLogService;
        this.environmentSyncService = environmentSyncService;
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.environmentJobService = environmentJobService;
        this.autoSyncConfig = autoSyncConfig;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Long envId = getEnvId();
        Optional<Environment> environmentOpt = environmentService.findEnvironmentById(envId);
        if (environmentOpt.isPresent()) {
            Environment environment = environmentOpt.get();
            prepareMdcContext(environment);
            if (flowLogService.isOtherFlowRunning(envId)) {
                LOGGER.info("EnvironmentStatusCheckerJob cannot run, because flow is running for environment: {}", environment.getName());
            } else {
                syncAnEnv(environment);
            }
            MDCBuilder.cleanupMdc();
        } else {
            environmentJobService.unschedule(envId);
            LOGGER.warn("EnvironmentStatusCheckerJob cannot run, because environment is not found with id: {}. This env is unscheduled now", envId);
        }
    }

    @VisibleForTesting
    void syncAnEnv(Environment environment) {
        try {
            ThreadBasedUserCrnProvider.doAs(environment.getCreator(), () -> {
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

    private void prepareMdcContext(Environment environment) {
        MdcContext.builder()
                .resourceCrn(environment.getResourceCrn())
                .resourceName(environment.getName())
                .resourceType("ENVIRONMENT")
                .buildMdc();
    }

    private Long getEnvId() {
        return Long.valueOf(getLocalId());
    }
}
