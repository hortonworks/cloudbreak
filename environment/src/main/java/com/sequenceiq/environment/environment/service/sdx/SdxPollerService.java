package com.sequenceiq.environment.environment.service.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.RUNNING;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.START_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOPPED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOP_IN_PROGRESS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
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
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class SdxPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxPollerService.class);

    private static final Set<SdxClusterStatusResponse> SKIP_STOP_OPERATION = Set.of(STOPPED, STOP_IN_PROGRESS);

    private static final Set<SdxClusterStatusResponse> SKIP_START_OPERATION = Set.of(RUNNING, START_IN_PROGRESS);

    @Value("${env.stop.polling.attempt:360}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final SdxService sdxService;

    private final SdxPollerProvider sdxPollerProvider;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public SdxPollerService(SdxService sdxService, SdxPollerProvider sdxPollerProvider,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor) {
        this.sdxService = sdxService;
        this.sdxPollerProvider = sdxPollerProvider;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
    }

    public void startAttachedDatalake(Long envId, String environmentName) {
        executeSdxOperationAndStartPolling(envId, environmentName, SKIP_START_OPERATION, sdxService::startByCrn, sdxPollerProvider::startSdxClustersPoller);
    }

    public void stopAttachedDatalakeClusters(Long envId, String environmentName) {
        executeSdxOperationAndStartPolling(envId, environmentName, SKIP_STOP_OPERATION, sdxService::stopByCrn, sdxPollerProvider::stopSdxClustersPoller);
    }

    private void executeSdxOperationAndStartPolling(Long envId, String environmentName, Set<SdxClusterStatusResponse> skipStatuses,
            Consumer<String> sdxOperation, BiFunction<Long, List<String>, AttemptMaker<Void>> attemptMakerFactory) {
        try {
            List<String> sdxCrns = getExecuteSdxOperationsAndGetCrns(environmentName, sdxOperation, skipStatuses);
            if (CollectionUtils.isNotEmpty(sdxCrns)) {
                Polling.stopAfterAttempt(attempt)
                        .stopIfException(true)
                        .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                        .run(attemptMakerFactory.apply(envId, sdxCrns));
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

    List<String> getExecuteSdxOperationsAndGetCrns(String environmentName, Consumer<String> sdxOperation, Set<SdxClusterStatusResponse> skipStatuses) {
        Collection<SdxClusterResponse> attachedSdxClusters = getAttachedDatalakeClusters(environmentName);
        return attachedSdxClusters.stream()
                .map(response -> {
                    String crn = response.getCrn();
                    if (skipStatuses.contains(response.getStatus())) {
                        LOGGER.info("The env operation is skipped because of the status of sdx in the proper state: {}", skipStatuses);
                    } else {
                        LOGGER.info("The env operation is executed, the status of sdx is {} but the skip status is {}", response.getStatus(), skipStatuses);
                        sdxOperation.accept(crn);
                    }
                    return crn;
                })
                .collect(Collectors.toList());
    }

    private Collection<SdxClusterResponse> getAttachedDatalakeClusters(String environmentName) {
        LOGGER.debug("Getting SDX clusters for environment: '{}'", environmentName);
        return sdxService.list(environmentName);
    }
}
