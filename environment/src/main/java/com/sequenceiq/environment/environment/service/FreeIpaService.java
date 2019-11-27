package com.sequenceiq.environment.environment.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.poller.FreeIpaPollerProvider;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Service
public class FreeIpaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final FreeIpaV1Endpoint freeIpaV1Endpoint;

    private final FreeIpaPollerProvider freeipaPollerProvider;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public FreeIpaService(FreeIpaV1Endpoint freeIpaV1Endpoint, FreeIpaPollerProvider freeipaPollerProvider,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.freeIpaV1Endpoint = freeIpaV1Endpoint;
        this.freeipaPollerProvider = freeipaPollerProvider;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void startAttachedFreeipaInstances(Long envId, String envCrn) {
        executeFreeIpaOperationAndStartPolling(envCrn, this::startFreeIpa, freeipaPollerProvider.startPoller(envId, envCrn));
    }

    public void stopAttachedFreeipaInstances(Long envId, String envCrn) {
        executeFreeIpaOperationAndStartPolling(envCrn, this::stopFreeIpa, freeipaPollerProvider.stopPoller(envId, envCrn));
    }

    private void startFreeIpa(String environmentCrn) {
        try {
            freeIpaV1Endpoint.start(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to start FreeIpa(s) for environment '{}' due to: '{}'", environmentCrn, errorMessage, e);
            throw new EnvironmentServiceException(errorMessage, e);
        }
    }

    private void stopFreeIpa(String environmentCrn) {
        try {
            freeIpaV1Endpoint.stop(environmentCrn);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to stop FreeIpa(s) for environment '{}' due to: '{}'", environmentCrn, errorMessage, e);
            throw new EnvironmentServiceException(errorMessage, e);
        }
    }

    private void executeFreeIpaOperationAndStartPolling(String envCrn, Consumer<String> freeIpaOperation, AttemptMaker<Void> attemptMaker) {
        try {
            DescribeFreeIpaResponse freeIpaResponse = freeipaPollerProvider.describe(envCrn);
            if (freeIpaResponse != null && !freeIpaResponse.getStatus().isAvailable()) {
                freeIpaOperation.accept(envCrn);
                Polling.stopAfterAttempt(attempt)
                        .stopIfException(true)
                        .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            }
        } catch (PollerException e) {
            if (e.getCause() != null && e.getCause() instanceof WebApplicationException) {
                WebApplicationException wae = (WebApplicationException) e.getCause();
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(wae);
                throw new EnvironmentServiceException(errorMessage, e);
            }
            throw e;
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            throw new EnvironmentServiceException(errorMessage, e);
        }
    }
}
