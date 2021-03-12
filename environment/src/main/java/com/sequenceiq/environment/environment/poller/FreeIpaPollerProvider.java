package com.sequenceiq.environment.environment.poller;

import java.util.EnumSet;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;

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
            if (freeIpaResponse.isEmpty() || freeipaStopped(freeIpaResponse.get())) {
                return AttemptResults.finishWith(null);
            } else {
                return checkStopStatus(freeIpaResponse.get());
            }
        };
    }

    public AttemptMaker<Void> syncUsersPoller(Long envId, String envCrn, String operationId) {
        return () -> {
            //TODO [AF]: I would prefer EnvironmentInMemoryStateStore.get(envId))).isCancelled()
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
        // TODO: is the Status.isAvailable() wrong instead of this?
        return Status.AVAILABLE.equals(freeipa.getStatus());
    }

    private boolean freeIpaUsersSynchronized(SyncOperationStatus freeIpaResponse) {
        return SynchronizationStatus.COMPLETED.equals(freeIpaResponse.getStatus());
    }
}
