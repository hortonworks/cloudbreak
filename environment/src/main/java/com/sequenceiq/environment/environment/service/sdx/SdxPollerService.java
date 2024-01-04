package com.sequenceiq.environment.environment.service.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DATALAKE_PROXY_CONFIG_MODIFICATION_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.RUNNING;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.START_IN_PROGRESS;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOPPED;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.STOP_IN_PROGRESS;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.exception.PollerException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Service
public class SdxPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxPollerService.class);

    private static final Set<SdxClusterStatusResponse> SKIP_STOP_OPERATION = Set.of(STOPPED, STOP_IN_PROGRESS);

    private static final Set<SdxClusterStatusResponse> SKIP_START_OPERATION = Set.of(RUNNING, START_IN_PROGRESS);

    private static final Set<SdxClusterStatusResponse> SKIP_MODIFY_PROXY_OPERATION = Set.of(DATALAKE_PROXY_CONFIG_MODIFICATION_IN_PROGRESS);

    @Value("${env.stop.polling.attempt:360}")
    private Integer attempt;

    @Value("${env.stop.polling.sleep.time:5}")
    private Integer sleeptime;

    private final SdxService sdxService;

    private final SdxPollerProvider sdxPollerProvider;

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    private final DatalakeMultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    public SdxPollerService(SdxService sdxService, SdxPollerProvider sdxPollerProvider,
            WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor,
            DatalakeMultipleFlowsResultEvaluator multipleFlowsResultEvaluator) {
        this.sdxService = sdxService;
        this.sdxPollerProvider = sdxPollerProvider;
        this.webApplicationExceptionMessageExtractor = webApplicationExceptionMessageExtractor;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
    }

    public void startAttachedDatalake(Long envId, String environmentName) {
        executeSdxOperationAndStartPolling(envId, environmentName, SKIP_START_OPERATION, sdxService::startByCrn);
    }

    public void stopAttachedDatalakeClusters(Long envId, String environmentName) {
        executeSdxOperationAndStartPolling(envId, environmentName, SKIP_STOP_OPERATION, sdxService::stopByCrn);
    }

    public void modifyProxyConfigOnAttachedDatalakeClusters(Long envId, String environmentName, String previousProxyCrn) {
        executeSdxOperationAndStartPolling(envId, environmentName, SKIP_MODIFY_PROXY_OPERATION,
                sdxCrn -> sdxService.modifyProxy(sdxCrn, previousProxyCrn));
    }

    private void executeSdxOperationAndStartPolling(Long envId, String environmentName, Set<SdxClusterStatusResponse> skipStatuses,
            Function<String, FlowIdentifier> sdxOperation) {
        try {
            List<FlowIdentifier> flowIdentifiers = getExecuteSdxOperationsAndGetCrns(environmentName, sdxOperation, skipStatuses);
            if (CollectionUtils.isNotEmpty(flowIdentifiers)) {
                Polling.stopAfterAttempt(attempt)
                        .stopIfException(false)
                        .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                        .run(sdxPollerProvider.flowListPoller(envId, flowIdentifiers));
            }
            if (multipleFlowsResultEvaluator.anyFailed(flowIdentifiers)) {
                throw new EnvironmentServiceException(String.format("Sdx start/stop operation failed. FlowIds: %s", flowIdentifiers));
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

    List<FlowIdentifier> getExecuteSdxOperationsAndGetCrns(String environmentName, Function<String, FlowIdentifier> sdxOperation,
            Set<SdxClusterStatusResponse> skipStatuses) {
        Collection<SdxClusterResponse> attachedSdxClusters = getAttachedDatalakeClusters(environmentName);
        return attachedSdxClusters.stream()
                .map(response -> {
                    String crn = response.getCrn();
                    FlowIdentifier flowIdentifier = FlowIdentifier.notTriggered();
                    if (skipStatuses.contains(response.getStatus())) {
                        LOGGER.info("The env operation is skipped because of the status of sdx in the proper state: {}", skipStatuses);
                    } else {
                        LOGGER.info("The env operation is executed, the status of sdx is {} but the skip status is {}", response.getStatus(), skipStatuses);
                        flowIdentifier = sdxOperation.apply(crn);
                    }
                    return flowIdentifier;
                })
                .collect(Collectors.toList());
    }

    private Collection<SdxClusterResponse> getAttachedDatalakeClusters(String environmentName) {
        LOGGER.debug("Getting SDX clusters for environment: '{}'", environmentName);
        return sdxService.list(environmentName);
    }
}
