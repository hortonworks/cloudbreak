package com.sequenceiq.environment.environment.poller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.core.AttemptState;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Component
public class DatahubPollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatahubPollerProvider.class);

    private final DatahubService datahubService;

    private final ClusterPollerResultEvaluator clusterPollerResultEvaluator;

    private final FlowEndpoint flowEndpoint;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private final FlowResultPollerEvaluator flowResultPollerEvaluator;

    public DatahubPollerProvider(DatahubService datahubService, ClusterPollerResultEvaluator clusterPollerResultEvaluator, FlowEndpoint flowEndpoint,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory, FlowResultPollerEvaluator flowResultPollerEvaluator) {

        this.datahubService = datahubService;
        this.clusterPollerResultEvaluator = clusterPollerResultEvaluator;
        this.flowEndpoint = flowEndpoint;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
        this.flowResultPollerEvaluator = flowResultPollerEvaluator;
    }

    public AttemptMaker<Void> startDatahubClustersPoller(List<String> datahubCrns, Long envId) {
        List<String> mutableCrnList = new ArrayList<>(datahubCrns);
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectDatahubStartResults(mutableCrnList, remaining, envId);
            mutableCrnList.retainAll(remaining);
            return clusterPollerResultEvaluator.evaluateResult(results);
        };
    }

    public AttemptMaker<Void> stopDatahubClustersPoller(List<String> datahubCrns, Long envId) {
        List<String> mutableCrnList = new ArrayList<>(datahubCrns);
        return () -> {
            List<String> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectDatahubStopResults(mutableCrnList, remaining, envId);
            mutableCrnList.retainAll(remaining);
            return clusterPollerResultEvaluator.evaluateResult(results);
        };
    }

    public AttemptMaker<Void> upgradeCcmPoller(Long envId, List<FlowIdentifier> upgradeFlows) {
        List<FlowIdentifier> mutableFlowIds = new ArrayList<>(upgradeFlows);
        return () -> {
            List<FlowIdentifier> remaining = new ArrayList<>();
            List<AttemptResult<Void>> results = collectUpgradeCcmResults(mutableFlowIds, remaining, envId);
            mutableFlowIds.retainAll(remaining);
            return flowResultPollerEvaluator.evaluateResult(results);
        };
    }

    private List<AttemptResult<Void>> collectDatahubStartResults(List<String> pollingCrns, List<String> remaining, Long envId) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "Datahub polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }
        return pollingCrns.stream()
                .map(crn -> fetchStartDatahubClustersResult(remaining, crn))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStartDatahubClustersResult(List<String> remainingCrns, String crn) {
        StackV4Response stack = datahubService.getByCrn(crn, Collections.emptySet());
        if (stackAndClusterAvailable(stack, stack.getCluster())) {
            return AttemptResults.finishWith(null);
        } else {
            remainingCrns.add(crn);
            return checkDatahubStartStatus(stack);
        }
    }

    private List<AttemptResult<Void>> collectDatahubStopResults(List<String> datahubCrns, List<String> remaining, Long envId) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "Datahub polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }

        return datahubCrns.stream()
                .map(datahubCrn -> fetchStopDatahubClustersResult(remaining, datahubCrn))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchStopDatahubClustersResult(List<String> remainingCrns, String datahubCrn) {
        StackV4Response stack = datahubService.getByCrn(datahubCrn, Collections.emptySet());
        if (stackAndClusterStopped(stack, stack.getCluster())) {
            return AttemptResults.finishWith(null);
        } else {
            remainingCrns.add(datahubCrn);
            return checkDatahubStopStatus(stack);
        }
    }

    private AttemptResult<Void> checkDatahubStartStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.START_FAILED.equals(stack.getStatus())) {
            LOGGER.error("Datahub stack start failed for '{}' with status: {} and reason: {}",
                    stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stack start failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.START_FAILED.equals(cluster.getStatus())) {
            LOGGER.error("Datahub cluster start failed for '{}' with status: {} and reason: {}",
                    cluster.getName(), cluster.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub cluster start failed '" + cluster.getName() + "', " + cluster.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean stackAndClusterAvailable(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isAvailable()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isAvailable();
    }

    private AttemptResult<Void> checkDatahubStopStatus(StackV4Response stack) {
        ClusterV4Response cluster = stack.getCluster();
        if (Status.STOP_FAILED.equals(stack.getStatus())) {
            LOGGER.error("Datahub cluster stop failed for '{}' with status: {} and reason: {}",
                    stack.getName(), stack.getStatus(), stack.getStatusReason());
            return AttemptResults.breakFor("Datahub stack stop failed '" + stack.getName() + "', " + stack.getStatusReason());
        } else if (cluster != null && Status.STOP_FAILED.equals(cluster.getStatus())) {
            LOGGER.error("Datahub cluster stop failed for '{}' with status: {} and reason: {}", cluster.getName(), cluster.getStatus(),
                    cluster.getStatusReason());
            return AttemptResults.breakFor("Datahub cluster stop failed '" + cluster.getName() + "', " + cluster.getStatusReason());
        } else {
            return AttemptResults.justContinue();
        }
    }

    private boolean stackAndClusterStopped(StackV4Response stackV4Response, ClusterV4Response cluster) {
        return stackV4Response.getStatus().isStopped()
                && cluster != null
                && cluster.getStatus() != null
                && cluster.getStatus().isStopped();
    }

    private List<AttemptResult<Void>> collectUpgradeCcmResults(List<FlowIdentifier> mutableFlowIds, List<FlowIdentifier> remainingFlowIds, Long envId) {
        if (PollGroup.CANCELLED.equals(EnvironmentInMemoryStateStore.get(envId))) {
            String message = "Datahub polling cancelled in inmemory store, id: " + envId;
            LOGGER.info(message);
            throw new PollerStoppedException(message);
        }

        return mutableFlowIds.stream()
                .map(flowId -> fetchUpgradeCcmResult(remainingFlowIds, flowId))
                .collect(Collectors.toList());
    }

    private AttemptResult<Void> fetchUpgradeCcmResult(List<FlowIdentifier> remainingFlowIds, FlowIdentifier flowId) {
        LOGGER.debug("Flow being checked for upgrade CCM result: {}", flowId);
        FlowCheckResponse flowCheckResponse;
        switch (flowId.getType()) {
            case FLOW:
                flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> flowEndpoint.hasFlowRunningByFlowId(flowId.getPollableId()));
                break;
            case FLOW_CHAIN:
                flowCheckResponse = ThreadBasedUserCrnProvider.doAsInternalActor(
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> flowEndpoint.hasFlowRunningByChainId(flowId.getPollableId()));
                break;
            case NOT_TRIGGERED:
                remainingFlowIds.add(flowId);
                return AttemptResults.justContinue();
            default:
                throw new IllegalStateException("Unexpected Flow type: " + flowId.getType());
        }
        AttemptResult<Void> upgradeFlowStatus = checkUpgradeCcmStatus(flowCheckResponse);
        if (AttemptState.FINISH != upgradeFlowStatus.getState()) {
            remainingFlowIds.add(flowId);
        }
        return upgradeFlowStatus;
    }

    private AttemptResult<Void> checkUpgradeCcmStatus(FlowCheckResponse flowCheckResponse) {
        if (Boolean.TRUE.equals(flowCheckResponse.getLatestFlowFinalizedAndFailed())) {
            LOGGER.error("Datahub upgrade CCM flow {} in flowchain {} failed", flowCheckResponse.getFlowChainId(), flowCheckResponse.getFlowId());
            return AttemptResults.justFinish();
        }
        if (Boolean.TRUE.equals(flowCheckResponse.getHasActiveFlow())) {
            return AttemptResults.justContinue();
        }
        return AttemptResults.justFinish();
    }

}
