package com.sequenceiq.environment.environment.poller;

import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class FreeipaPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaPollerProvider.class);

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    public FreeipaPollerProvider(FreeIpaV1Endpoint freeIpaV1Endpoint) {
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
    }

    public AttemptMaker<Void> startPoller(Long envId, String envCrn) {
        return () -> {
            if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
                LOGGER.info("Freeipa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("Freeipa polling cancelled in inmemory store, id: " + envId);
            }
            DescribeFreeIpaResponse freeIpaResponse = describe(envCrn);
            if (freeIpaResponse == null || freeipaAvailable(freeIpaResponse)) {
                return AttemptResults.finishWith(null);
            } else {
                return checkStartStatus(freeIpaResponse);
            }
        };
    }

    public AttemptMaker<Void> stopPoller(Long envId, String envCrn) {
        return () -> {
            if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
                LOGGER.info("Freeipa polling cancelled in inmemory store, id: " + envId);
                return AttemptResults.breakFor("Freeipa polling cancelled in inmemory store, id: " + envId);
            }
            DescribeFreeIpaResponse freeIpaResponse = describe(envCrn);
            if (freeIpaResponse == null || freeipaStopped(freeIpaResponse)) {
                return AttemptResults.finishWith(null);
            } else {
                return checkStopStatus(freeIpaResponse);
            }
        };
    }

    public DescribeFreeIpaResponse describe(String envCrn) {
        try {
            return freeIpaV1Endpoint.describe(envCrn);
        } catch (NotFoundException e) {
            LOGGER.info("Could not find freeipa with envCrn: " + envCrn);
        }
        return null;
    }

    private AttemptResult<Void> checkStopStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (freeIpaResponse.getStatus() == Status.STOP_FAILED) {
            LOGGER.info("Freeipa stop failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(),
                    freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("Freeipa stop failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private AttemptResult<Void> checkStartStatus(DescribeFreeIpaResponse freeIpaResponse) {
        if (freeIpaResponse.getStatus() == Status.START_FAILED) {
            LOGGER.info("Freeipa start failed for '{}' with status {}, reason: {}", freeIpaResponse.getName(), freeIpaResponse.getStatus(),
                    freeIpaResponse.getStatusReason());
            return AttemptResults.breakFor("Freeipa start failed '" + freeIpaResponse.getName() + "', " + freeIpaResponse.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean freeipaStopped(DescribeFreeIpaResponse freeipa) {
        return freeipa.getStatus() == Status.STOPPED;
    }

    private boolean freeipaAvailable(DescribeFreeIpaResponse freeipa) {
        return freeipa.getStatus() == Status.AVAILABLE;
    }
}
