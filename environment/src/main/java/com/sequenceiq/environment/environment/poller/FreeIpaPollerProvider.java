package com.sequenceiq.environment.environment.poller;

import java.util.EnumSet;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Component
public class FreeIpaPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPollerProvider.class);

    private static final EnumSet<SynchronizationStatus> FAILED_SYNCHRONIZATION_STATUSES =
            EnumSet.of(SynchronizationStatus.FAILED, SynchronizationStatus.REJECTED, SynchronizationStatus.TIMEDOUT);

    private final FreeIpaService freeIpaService;

    public FreeIpaPollerProvider(FreeIpaService freeIpaService) {
        this.freeIpaService = freeIpaService;
    }

    public AttemptMaker<Void> startPoller(Long envId, String envCrn) {
        return () -> {
            if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
                LOGGER.info("FreeIpa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("FreeIpa polling cancelled in inmemory store, id: " + envId);
            }
            Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(envCrn);
            if (freeIpaResponse.isPresent()) {
                LOGGER.debug("Next FreeIpa start polling's step. FreeIpa status: {}", freeIpaResponse.get().getStatus());
            } else {
                LOGGER.debug("FreeIpa isn't found");
            }
            if (freeIpaResponse.isEmpty() || freeipaAvailable(freeIpaResponse.get())) {
                return AttemptResults.finishWith(null);
            } else {
                return checkStartStatus(freeIpaResponse.get());
            }
        };
    }

    public AttemptMaker<Void> stopPoller(Long envId, String envCrn) {
        return () -> {
            if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
                LOGGER.info("FreeIpa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("FreeIpa polling cancelled in inmemory store, id: " + envId);
            }
            Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(envCrn);
            if (freeIpaResponse.isPresent()) {
                LOGGER.debug("Next FreeIpa stop polling's step. FreeIpa status: {}", freeIpaResponse.get().getStatus());
            } else {
                LOGGER.debug("FreeIpa isn't found");
            }
            if (freeIpaResponse.isEmpty() || freeipaStopped(freeIpaResponse.get())) {
                return AttemptResults.finishWith(null);
            } else {
                return checkStopStatus(freeIpaResponse.get());
            }
        };
    }

    public AttemptMaker<Void> syncUsersPoller(Long envId, String envCrn, String operationId) {
        return () -> {
            if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
                LOGGER.info("FreeIpa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("FreeIpa polling cancelled in inmemory store, id: " + envId);
            }
            SyncOperationStatus freeIpaResponse = freeIpaService.getSyncOperationStatus(envCrn, operationId);
            if (freeIpaUsersSynchronized(freeIpaResponse)) {
                return AttemptResults.finishWith(null);
            } else {
                return checkSyncStatus(freeIpaResponse);
            }
        };
    }

    private AttemptResult<Void> checkStopStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (Status.STOP_FAILED.equals(freeIpaResponse.getStatus())) {
            LOGGER.error("FreeIpa stop failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(),
                    freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("FreeIpa stop failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkStartStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (Status.START_FAILED.equals(freeIpaResponse.getStatus())) {
            LOGGER.error("FreeIpa start failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(),
                    freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("FreeIpa start failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkSyncStatus(SyncOperationStatus freeIpaResponse) {
        if (FAILED_SYNCHRONIZATION_STATUSES.contains(freeIpaResponse.getStatus())) {
            LOGGER.error("FreeIpa user synchronization failed for '{}' with status {}, reason: {}",
                    freeIpaResponse.getOperationId(), freeIpaResponse.getStatus(), freeIpaResponse.getFailure());
            return AttemptResults.breakFor("FreeIpa user synchronization failed '" + freeIpaResponse.getOperationId() + "', " + freeIpaResponse.getFailure());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean freeipaStopped(DescribeFreeIpaResponse freeipa) {
        return Status.STOPPED.equals(freeipa.getStatus());
    }

    private boolean freeipaAvailable(DescribeFreeIpaResponse freeipa) {
        return Status.AVAILABLE.equals(freeipa.getStatus());
    }

    private boolean freeIpaUsersSynchronized(SyncOperationStatus freeIpaResponse) {
        return SynchronizationStatus.COMPLETED.equals(freeIpaResponse.getStatus());
    }

    public AttemptResult<Void> upgradeCcmPoller(Long envId, String envCrn, String operationId) {
        return operationStatusPoller(envId, envCrn, operationId, "FreeIpa Upgrade CCM");
    }

    public AttemptResult<Void> modifyProxyConfigPoller(Long envId, String envCrn, String operationId) {
        return operationStatusPoller(envId, envCrn, operationId, "FreeIpa modify proxy config");
    }

    private AttemptResult<Void> operationStatusPoller(Long envId, String envCrn, String operationId, String operationName) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            LOGGER.info("FreeIpa polling cancelled in inmemory store, id: " + envId);
            return AttemptResults.breakFor("FreeIpa polling cancelled in inmemory store, id: " + envId);
        }
        OperationStatus operationStatus = freeIpaService.getOperationStatus(operationId);
        LOGGER.debug("Operation status: {}", operationStatus);
        return switch (operationStatus.getStatus()) {
            case REQUESTED, RUNNING -> AttemptResults.justContinue();
            case COMPLETED -> AttemptResults.justFinish();
            case TIMEDOUT -> AttemptResults.breakFor(operationName + " failed: timeout.");
            case REJECTED -> AttemptResults.breakFor(operationName + " operation request was rejected.");
            case FAILED -> AttemptResults.breakFor(operationName + " failed: " + operationStatus.getError());
        };
    }

    public AttemptResult<Void> verticalScalePoller(Long envId, String envCrn, FlowIdentifier flowIdentifier) {
        return flowPoller(envId, envCrn, flowIdentifier, "FreeIPA vertical scale");
    }

    public AttemptResult<Void> crossRealmTrustPoller(Long envId, String envCrn, FlowIdentifier flowIdentifier, String operation) {
        return flowPoller(envId, envCrn, flowIdentifier, "FreeIPA cross realm trust " + operation);
    }

    private AttemptResult<Void> flowPoller(Long envId, String envCrn, FlowIdentifier flowIdentifier, String flowName) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            LOGGER.info("FreeIpa polling cancelled in inmemory store, id: " + envId);
            return AttemptResults.breakFor("FreeIpa polling cancelled in inmemory store, id: " + envId);
        }
        FlowCheckResponse flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(() -> freeIpaService.checkFlow(flowIdentifier));
        LOGGER.debug("[----------FREEIPA - CHECK----------]Flow status: {}", flowCheckResponse);
        if (flowCheckResponse.getHasActiveFlow()) {
            return AttemptResults.justContinue();
        } else if (flowCheckResponse.getLatestFlowFinalizedAndFailed()) {
            return AttemptResults.breakFor(flowName + " failed.");
        } else {
            return AttemptResults.justFinish();
        }
    }

}
