package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;
import com.sequenceiq.environment.environment.domain.LbUpdateFlowLog;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.UpdateFailedException;
import com.sequenceiq.environment.network.service.LbUpdateFlowLogService;
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

    private final LbUpdateFlowLogService lbUpdateFlowLogService;

    @Value("${env.loadbalancer.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.loadbalancer.update.polling.sleep.time.seconds:30}")
    private Integer sleepTime;

    public LoadBalancerPollerService(
            DatahubService datahubService,
            SdxService sdxService,
            StackService stackService,
            FlowEndpoint flowEndpoint,
            LbUpdateFlowLogService lbUpdateFlowLogService) {
        this.datahubService = datahubService;
        this.sdxService = sdxService;
        this.stackService = stackService;
        this.flowEndpoint = flowEndpoint;
        this.lbUpdateFlowLogService = lbUpdateFlowLogService;
    }

    public void updateStackWithLoadBalancer(Long environmentId, String environmentCrn, String environmentName,
            PublicEndpointAccessGateway endpointAccessGateway, String flowId) {
        Map<String, String> stackNames;
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
                Map<String, FlowIdentifier> failedFlows = waitStackLoadBalancerUpdate(getPollingConfig(), stackNames, environmentName,
                    flowId, environmentCrn);
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

    private Map<String, FlowIdentifier> waitStackLoadBalancerUpdate(PollingConfig pollingConfig, Map<String, String> stackNameCrnMap,
            String environmentName, String parentFlowId, String environmentCrn) {

        LOGGER.debug("Attempting to initiate load balancer update for {} clusters for environment id {}",
            stackNameCrnMap.size(), environmentName);
        Map<String, FlowIdentifier> flowIdentifiers = stackService.updateLoadBalancer(stackNameCrnMap.keySet());

        LOGGER.debug("Creating database entries to track update status.");
        List<LbUpdateFlowLog> lbUpdateFlowLogs = new ArrayList<>();
        flowIdentifiers.forEach((name, flowId) -> {
            LbUpdateFlowLog lbUpdateFlowLog = new LbUpdateFlowLog();
            lbUpdateFlowLog.setParentFlowId(parentFlowId);
            lbUpdateFlowLog.setChildResourceName(name);
            lbUpdateFlowLog.setChildResourceCrn(stackNameCrnMap.get(name));
            lbUpdateFlowLog.setChildFlowId(flowId == null ? null : flowId.getPollableId());
            lbUpdateFlowLog.setEnvironmentCrn(environmentCrn);
            lbUpdateFlowLog.setStatus(flowId == null ? LoadBalancerUpdateStatus.COULD_NOT_START : LoadBalancerUpdateStatus.IN_PROGRESS);
            lbUpdateFlowLogs.add(lbUpdateFlowLog);
        });
        lbUpdateFlowLogService.saveAll(lbUpdateFlowLogs);

        LOGGER.debug("Checking to see if all flows were started correctly.");
        Set<String> failedClusters = flowIdentifiers.entrySet().stream()
            .filter(entry -> entry.getValue() == null)
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (!failedClusters.isEmpty()) {
            LOGGER.error("Flows for clusters [{}] failed to start.", failedClusters);
        }

        LOGGER.debug("Monitoring load balancer update flows: {}", flowIdentifiers);
        LOGGER.debug("Starting poller to check all data lake and data hub stacks for environment {} are updated", environmentName);
        return Polling.stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
            .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
            .waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
            .run(() -> periodicCheckForCompletion(flowIdentifiers, parentFlowId));
    }

    private AttemptResult<Map<String, FlowIdentifier>> periodicCheckForCompletion(Map<String, FlowIdentifier> flowIdentifiers, String parentFlowID) {
        try {
            boolean anyFlowsActive = false;
            Set<LbUpdateFlowLog> updateLogs = lbUpdateFlowLogService.findByParentFlowId(parentFlowID);
            for (Map.Entry<String, FlowIdentifier> entry : flowIdentifiers.entrySet()) {
                String resourceName = entry.getKey();
                FlowIdentifier flowId = entry.getValue();



                if (flowId != null) {
                    LOGGER.debug("Updating current state of flow {} in database", flowId.getPollableId());
                    FlowLogResponse lastFlowLog = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                        flowEndpoint.getLastFlowById(flowId.getPollableId()));
                    updateLogs.stream()
                        .filter(flowLog -> resourceName.equals(flowLog.getChildResourceCrn()))
                        .forEach(flowLog -> flowLog.setCurrentState(lastFlowLog.getCurrentState()));

                    // TODO

                    Boolean hasActiveFlow = ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                        flowEndpoint.hasFlowRunningByFlowId(flowId.getPollableId()).getHasActiveFlow());
                    if (hasActiveFlow) {
                        LOGGER.debug("Flow {} is still running", flowId.getPollableId());
                    } else {
                        updateLogs.stream()
                            .filter(flowLog -> resourceName.equals(flowLog.getChildResourceCrn()))
                            .forEach(flowLog -> flowLog.setStatus(LoadBalancerUpdateStatus.FINISHED));
                        LOGGER.debug("Flow {} is complete", flowId.getPollableId());
                    }
                    anyFlowsActive = anyFlowsActive || hasActiveFlow;
                }
            }
            if (anyFlowsActive) {
                lbUpdateFlowLogService.saveAll(updateLogs);
                return AttemptResults.justContinue();
            } else {
                Map<String, FlowIdentifier> failedFlows = new HashMap<>();
                for (Map.Entry<String, FlowIdentifier> entry : flowIdentifiers.entrySet()) {
                    String resourceName = entry.getKey();
                    FlowIdentifier flowId = entry.getValue();
                    if (flowId == null || hasFlowFailed(ThreadBasedUserCrnProvider.doAsInternalActor(() ->
                        flowEndpoint.getFlowLogsByFlowId(flowId.getPollableId())))) {
                        failedFlows.put(entry.getKey(), flowId);
                        if (flowId != null) {
                            updateLogs.stream()
                                .filter(flowLog -> resourceName.equals(flowLog.getChildResourceCrn()))
                                .forEach(flowLog -> flowLog.setStatus(LoadBalancerUpdateStatus.FAILED));
                            lbUpdateFlowLogService.saveAll(updateLogs);
                        }
                    }
                }
                lbUpdateFlowLogService.saveAll(updateLogs);
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

    private Map<String, String> getDataLakeAndDataHubNames(String environmentCrn, String environmentName) {
        Map<String, String> dataHubs = getAttachedDatahubClusters(environmentCrn);
        Map<String, String> dataLakes = getAttachedDatalakeClusters(environmentName);
        return Stream.concat(dataHubs.entrySet().stream(), dataLakes.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, String> getAttachedDatahubClusters(String environmentCrn) {
        LOGGER.debug("Getting Datahub clusters for environment: '{}'", environmentCrn);
        Collection<StackViewV4Response> responses = datahubService.list(environmentCrn).getResponses();
        return responses.stream()
            .collect(Collectors.toMap(StackViewV4Response::getName, StackViewV4Response::getCrn));
    }

    private Map<String, String> getAttachedDatalakeClusters(String environmentName) {
        LOGGER.debug("Getting SDX clusters for environment: '{}'", environmentName);
        Collection<SdxClusterResponse> responses = sdxService.list(environmentName);
        return responses.stream()
            .collect(Collectors.toMap(SdxClusterResponse::getName, SdxClusterResponse::getCrn));
    }

    private boolean hasFlowFailed(List<FlowLogResponse> flowLogs) {
        return flowLogs.stream().map(FlowLogResponse::getCurrentState).anyMatch(LOAD_BALANCER_UPDATE_FAILED_STATE::equals);
    }
}
