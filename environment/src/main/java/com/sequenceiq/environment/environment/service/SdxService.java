package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class SdxService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Value("${env.stop.polling.attempt:90}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final SdxEndpoint sdxEndpoint;

    private final SdxPollerProvider sdxPollerProvider;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public SdxService(SdxEndpoint sdxEndpoint, SdxPollerProvider sdxPollerProvider,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.sdxEndpoint = sdxEndpoint;
        this.sdxPollerProvider = sdxPollerProvider;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void startAttachedDatalake(Long envId, String environmentName) {
        executeSdxOperationAndStartPolling(envId, environmentName, sdxEndpoint::startByCrn, sdxPollerProvider::startSdxClustersPoller);

    }

    public void stopAttachedDatalakeClusters(Long envId, String environmentName) {
        executeSdxOperationAndStartPolling(envId, environmentName, sdxEndpoint::stopByCrn, sdxPollerProvider::stopSdxClustersPoller);
    }

    private void executeSdxOperationAndStartPolling(Long envId, String environmentName, Consumer<String> sdxOperation,
            BiFunction<Long, List<String>, AttemptMaker<Void>> attemptMakerFactory) {
        try {
            ArrayList<String> sdxCrns = getExecuteSdxOperationsAndGetCrns(environmentName, sdxOperation);
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(attemptMakerFactory.apply(envId, sdxCrns));
        } catch (PollerException e) {
            if (e.getCause() != null && e.getCause() instanceof WebApplicationException) {
                WebApplicationException wae = (WebApplicationException) e.getCause();
                String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(wae);
                throw new EnvironmentServiceException(errorMessage, e);
            }
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            throw new EnvironmentServiceException(errorMessage, e);
        }
    }

    private ArrayList<String> getExecuteSdxOperationsAndGetCrns(String environmentName, Consumer<String> sdxOperation) {
        Collection<SdxClusterResponse> attachedSdxClusters = getAttachedDatalakeClusters(environmentName);
        return attachedSdxClusters.stream()
                .map(response -> {
                    String crn = response.getCrn();
                    sdxOperation.accept(crn);
                    return crn;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Collection<SdxClusterResponse> getAttachedDatalakeClusters(String environmentName) {
        LOGGER.debug("Getting SDX clusters for environment: '{}'", environmentName);
        try {
            return sdxEndpoint.list(environmentName);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get SDX clusters for environment '{}' due to: '{}'", environmentName, e.getMessage(), e);
            throw new EnvironmentServiceException(errorMessage, e);
        }
    }
}
