package com.sequenceiq.environment.environment.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.exception.EnvironmentServiceException;

@Service
public class DatahubService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubService.class);

    @Value("${env.stop.polling.attemptCount:90}")
    private Integer attemptCount;

    @Value("${env.stop.polling.sleep.time.seconds:5}")
    private Integer sleepTime;

    private final DistroXV1Endpoint distroXV1Endpoint;

    private final DatahubPollerProvider datahubPollerProvider;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DatahubService(DistroXV1Endpoint distroXV1Endpoint, DatahubPollerProvider datahubPollerProvider,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.distroXV1Endpoint = distroXV1Endpoint;
        this.datahubPollerProvider = datahubPollerProvider;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void startAttachedDatahubClusters(Long envId, String envCrn) {
        List<String> datahubCrns = getAttachedDatahubClusterCrns(envCrn);
        executeDatahubOperationAndStartPolling(datahubCrns, distroXV1Endpoint::putStartByCrns,
                datahubPollerProvider.startDatahubClustersPoller(datahubCrns, envId));

    }

    public void stopAttachedDatahubClusters(Long envId, String envCrn) {
        List<String> datahubCrns = getAttachedDatahubClusterCrns(envCrn);
        executeDatahubOperationAndStartPolling(datahubCrns, distroXV1Endpoint::putStopByCrns,
                datahubPollerProvider.stopDatahubClustersPoller(datahubCrns, envId));
    }

    private void executeDatahubOperationAndStartPolling(List<String> datahubCrns, Consumer<List<String>> distroxOperation, AttemptMaker<Void> attemptMaker) {
        if (CollectionUtils.isNotEmpty(datahubCrns)) {
            try {
                distroxOperation.accept(datahubCrns);
                Polling.stopAfterAttempt(attemptCount)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
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

    private List<String> getAttachedDatahubClusterCrns(String environmentCrn) {
        LOGGER.debug("Getting Datahub clusters for environment: '{}'", environmentCrn);
        try {
            Collection<StackViewV4Response> responses = distroXV1Endpoint.list(null, environmentCrn).getResponses();
            return mapStacksToCrns(responses);
        } catch (WebApplicationException e) {
            String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get Datahub clusters for environment '{}' due to: '{}'", environmentCrn, errorMessage, e);
            throw new EnvironmentServiceException(errorMessage, e);
        }
    }

    private List<String> mapStacksToCrns(Collection<StackViewV4Response> attachedDistroXClusters) {
        return attachedDistroXClusters.stream().map(StackViewV4Response::getCrn).collect(Collectors.toList());
    }
}
