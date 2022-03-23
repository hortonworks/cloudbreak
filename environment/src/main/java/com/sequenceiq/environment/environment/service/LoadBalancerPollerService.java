package com.sequenceiq.environment.environment.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.UpdateFailedException;
import com.sequenceiq.environment.util.PollingConfig;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

@Service
public class LoadBalancerPollerService {

    static final String LOAD_BALANCER_UPDATE_FAILED_STATE = "LOAD_BALANCER_UPDATE_FAILED_STATE";

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerPollerService.class);

    private final DatahubService datahubService;

    private final SdxService sdxService;

    private final StackService stackService;

    private final FlowEndpoint flowEndpoint;

    @Value("${env.loadbalancer.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.loadbalancer.update.polling.sleep.time.seconds:30}")
    private Integer sleepTime;

    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public LoadBalancerPollerService(
            DatahubService datahubService,
            SdxService sdxService,
            StackService stackService,
            FlowEndpoint flowEndpoint,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.datahubService = datahubService;
        this.sdxService = sdxService;
        this.stackService = stackService;
        this.flowEndpoint = flowEndpoint;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public void updateStackWithLoadBalancer(Long environmentId, String environmentCrn, String environmentName,
            PublicEndpointAccessGateway endpointAccessGateway) {
        Set<String> stackNames;
        if (PublicEndpointAccessGateway.ENABLED.equals(endpointAccessGateway)) {
            LOGGER.debug("Updating load balancers for endpoint gateway on Data Lake and Data Hubs.");
            stackNames = getDataLakeAndDataHubNames(environmentCrn, environmentName);
            LOGGER.debug("Found {} Data Lake and Data Hub clusters to update for environment {}.", stackNames.size(), environmentName);
        } else {
            LOGGER.debug("Updating load balancer for Data Lake cluster.");
            stackNames = getAttachedDatalakeClusters(environmentName);
        }

        if (stackNames.isEmpty()) {
            LOGGER.debug("No Data Lake or Data Hub clusters found for environment.");
        } else {
            try {
                List<FlowIdentifier> failedFlows = waitStackLoadBalancerUpdate(getPollingConfig(), stackNames, environmentName);
                LOGGER.debug("Data Lake and Data Hub load balancer update finished.");
                if (!failedFlows.isEmpty()) {
                    LOGGER.error("Found failed flows for Data Lake or Data Hub load balancer updates: " + failedFlows);
                    throw new UpdateFailedException("Data Lake or Data Hub update flows failed: " + failedFlows);
                }
            } catch (PollerStoppedException e) {
                throw new UpdateFailedException("Stack update poller reached timeout.", e);
            }
        }
    }

    private List<FlowIdentifier> waitStackLoadBalancerUpdate(PollingConfig pollingConfig, Set<String> stackNames, String environmentName) {
        LOGGER.debug("Attempting to initiate load balancer update for {} clusters for environment id {}",
            stackNames.size(), environmentName);
        List<FlowIdentifier> flowIdentifiers = stackService.updateLoadBalancer(stackNames);
        LOGGER.debug("Monitoring load balancer update flows: {}", flowIdentifiers);

        LOGGER.debug("Starting poller to check all data lake and data hub stacks for environment {} are updated", environmentName);
        return Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
            .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
            .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
            .run(() -> periodicCheckForCompletion(flowIdentifiers));
    }

    private AttemptResult<List<FlowIdentifier>> periodicCheckForCompletion(List<FlowIdentifier> flowIdentifiers) {
        try {
            boolean anyFlowsActive = false;
            for (FlowIdentifier flowIdentifier: flowIdentifiers) {
                Boolean hasActiveFlow = ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () ->
                    flowEndpoint.hasFlowRunningByFlowId(flowIdentifier.getPollableId()).getHasActiveFlow());
                if (hasActiveFlow) {
                    LOGGER.debug("Flow {} is still running", flowIdentifier.getPollableId());
                } else {
                    LOGGER.debug("Flow {} is complete", flowIdentifier.getPollableId());
                }
                anyFlowsActive = anyFlowsActive || hasActiveFlow;
            }
            if (anyFlowsActive) {
                return AttemptResults.justContinue();
            } else {
                List<FlowIdentifier> failedFlows = flowIdentifiers.stream()
                    .filter(flowId -> hasFlowFailed(ThreadBasedUserCrnProvider.doAsInternalActor(
                            regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                            () ->
                        flowEndpoint.getFlowLogsByFlowId(flowId.getPollableId()))))
                    .collect(Collectors.toList());
                return AttemptResults.finishWith(failedFlows);
            }
        } catch (Exception e) {
            LOGGER.warn("Failure checking status of flows {}, error is: {}", flowIdentifiers,
                e.getMessage());
            return AttemptResults.breakFor(e);
        }
    }

    private PollingConfig getPollingConfig() {
        return PollingConfig.builder()
            .withStopPollingIfExceptionOccured(true)
            .withSleepTime(sleepTime)
            .withSleepTimeUnit(TimeUnit.SECONDS)
            .withTimeout(maxTime)
            .withTimeoutTimeUnit(TimeUnit.SECONDS)
            .build();
    }

    private Set<String> getDataLakeAndDataHubNames(String environmentCrn, String environmentName) {
        return Stream.of(getAttachedDatahubClusters(environmentCrn), getAttachedDatalakeClusters(environmentName))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private Set<String> getAttachedDatahubClusters(String environmentCrn) {
        LOGGER.debug("Getting Datahub clusters for environment: '{}'", environmentCrn);
        Collection<StackViewV4Response> responses = datahubService.list(environmentCrn).getResponses();
        return responses.stream().map(StackViewV4Response::getName).collect(Collectors.toSet());
    }

    private Set<String> getAttachedDatalakeClusters(String environmentName) {
        LOGGER.debug("Getting SDX clusters for environment: '{}'", environmentName);
        Collection<SdxClusterResponse> responses = sdxService.list(environmentName);
        return responses.stream().map(SdxClusterResponse::getName).collect(Collectors.toSet());
    }

    private boolean hasFlowFailed(List<FlowLogResponse> flowLogs) {
        return flowLogs.stream().map(FlowLogResponse::getCurrentState).anyMatch(LOAD_BALANCER_UPDATE_FAILED_STATE::equals);
    }
}
