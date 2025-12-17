package com.sequenceiq.environment.environment.service.datahub;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;

@Service
public class DatahubPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubPollerService.class);

    @Value("${env.stop.polling.attemptCount:360}")
    private Integer attemptCount;

    @Value("${env.stop.polling.sleep.time.seconds:5}")
    private Integer sleepTime;

    private final DatahubService datahubService;

    private final DatahubPollerProvider datahubPollerProvider;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public DatahubPollerService(
            DatahubService datahubService,
            DatahubPollerProvider datahubPollerProvider,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.datahubService = datahubService;
        this.datahubPollerProvider = datahubPollerProvider;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void startAttachedDatahubClusters(Long envId, String envCrn) {
        List<String> datahubCrns = getAttachedDatahubClusterCrns(envCrn);
        executeDatahubOperationAndStartPolling(envCrn, datahubCrns, datahubService::putStartByCrns,
                datahubPollerProvider.startDatahubClustersPoller(datahubCrns, envId));

    }

    public void stopAttachedDatahubClusters(Long envId, String envCrn) {
        List<String> datahubCrns = getAttachedDatahubClusterCrns(envCrn);
        executeDatahubOperationAndStartPolling(envCrn, datahubCrns, datahubService::putStopByCrns,
                datahubPollerProvider.stopDatahubClustersPoller(datahubCrns, envId));
    }

    private void executeDatahubOperationAndStartPolling(String envCrn, List<String> datahubCrns,
            BiConsumer<String, List<String>> datahubOperation, AttemptMaker<Void> attemptMaker) {
        if (CollectionUtils.isNotEmpty(datahubCrns)) {
            datahubOperation.accept(envCrn, datahubCrns);
            try {
                Polling.stopAfterAttempt(attemptCount)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                LOGGER.info("Datahub operation timed out");
                throw new DatahubOperationFailedException("Datahub operation timed out");
            }
        }
    }

    private List<String> getAttachedDatahubClusterCrns(String environmentCrn) {
        LOGGER.debug("Getting Datahub clusters for environment: '{}'", environmentCrn);
        Collection<StackViewV4Response> responses = datahubService.list(environmentCrn).getResponses();
        return mapStacksToCrns(responses);
    }

    private List<String> mapStacksToCrns(Collection<StackViewV4Response> attachedDistroXClusters) {
        return attachedDistroXClusters.stream().map(StackViewV4Response::getCrn).collect(Collectors.toList());
    }
}
