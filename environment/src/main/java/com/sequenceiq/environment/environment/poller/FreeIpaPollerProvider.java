package com.sequenceiq.environment.environment.poller;

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

@Component
public class FreeIpaPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPollerProvider.class);

    private final FreeIpaService freeIpaService;

    public FreeIpaPollerProvider(FreeIpaService freeIpaService) {
        this.freeIpaService = freeIpaService;
    }

    public AttemptMaker<Void> startPoller(Long envId, String envCrn) {
        return () -> {
            if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
                LOGGER.info("Freeipa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("Freeipa polling cancelled in inmemory store, id: " + envId);
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
                LOGGER.info("Freeipa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("Freeipa polling cancelled in inmemory store, id: " + envId);
            }
            Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(envCrn);
            if (freeIpaResponse.isEmpty() || freeipaStopped(freeIpaResponse.get())) {
                return AttemptResults.finishWith(null);
            } else {
                return checkStopStatus(freeIpaResponse.get());
            }
        };
    }

    private AttemptResult<Void> checkStopStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (Status.STOP_FAILED.equals(freeIpaResponse.getStatus())) {
            LOGGER.error("Freeipa stop failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(),
                    freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("Freeipa stop failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkStartStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (Status.START_FAILED.equals(freeIpaResponse.getStatus())) {
            LOGGER.error("Freeipa start failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(),
                    freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("Freeipa start failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
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
}
