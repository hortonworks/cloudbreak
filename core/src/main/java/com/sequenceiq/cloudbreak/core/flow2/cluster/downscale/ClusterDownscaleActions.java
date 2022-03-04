package com.sequenceiq.cloudbreak.core.flow2.cluster.downscale;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.downscale.ClusterDownscaleEvent.FAILURE_EVENT;
import static java.util.Collections.emptySet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.ContextKeys;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartState;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterDownscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.RemoveHostsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CollectDownscaleCandidatesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Configuration
public class ClusterDownscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDownscaleActions.class);

    private static final String REPAIR = "REPAIR";

    private ClusterDownscaleService clusterDownscaleService;

    private StackService stackService;

    public ClusterDownscaleActions(ClusterDownscaleService clusterDownscaleService, StackService stackService) {
        this.clusterDownscaleService = clusterDownscaleService;
        this.stackService = stackService;
    }

    @Bean(name = "COLLECT_CANDIDATES_STATE")
    public Action<?, ?> collectCandidatesAction() {
        return new AbstractClusterAction<>(ClusterDownscaleTriggerEvent.class) {
            @Override
            protected void doExecute(ClusterViewContext context, ClusterDownscaleTriggerEvent payload, Map<Object, Object> variables) {
                ClusterDownscaleDetails clusterDownscaleDetails = payload.getDetails();
                variables.put(REPAIR, clusterDownscaleDetails == null ? Boolean.FALSE : Boolean.valueOf(payload.getDetails().isRepair()));
                clusterDownscaleService.clusterDownscaleStarted(context.getStackId(), payload.getHostGroupsWithAdjustment(),
                        payload.getHostGroupsWithPrivateIds(), payload.getDetails());
                CollectDownscaleCandidatesRequest request = new CollectDownscaleCandidatesRequest(context.getStackId(), payload.getHostGroupsWithAdjustment(),
                        payload.getHostGroupsWithPrivateIds(), payload.getDetails());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "DECOMMISSION_STATE")
    public Action<?, ?> decommissionAction() {
        return new AbstractClusterAction<>(CollectDownscaleCandidatesResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, CollectDownscaleCandidatesResult payload, Map<Object, Object> variables) {
                variables.put(ContextKeys.PRIVATE_IDS, payload.getPrivateIds());
                Selectable event;
                if (isPayloadContainsAnyPrivateId(payload)) {
                    Boolean repair = (Boolean) variables.get(REPAIR);
                    Stack stack = stackService.getByIdWithListsInTransaction(context.getStackId());
                    DecommissionRequest decommissionRequest =
                            new DecommissionRequest(context.getStackId(), payload.getHostGroupNames(), payload.getPrivateIds(),
                                    payload.getRequest().getDetails());
                    if (repair && stack.hasCustomHostname()) {
                        LOGGER.debug("Cluster decommission state identified that the current action is a repair, hence we're going that way from now.");
                        event = new DecommissionResult(decommissionRequest, getHostNamesForPrivateIds(payload.getPrivateIds(), stack));
                    } else {
                        event = decommissionRequest;
                    }
                } else {
                    LOGGER.info("Handler wasn't able to collect any candidate [stackId:{}, host group name: {}] for downscaling, therefore we're handling " +
                            "it as a success (since nothing to decommission)", context.getStackId(), payload.getHostGroupNames());
                    event = new RemoveHostsSuccess(context.getStackId(), payload.getHostGroupNames(), emptySet());
                }
                sendEvent(context, event.selector(), event);
            }
        };
    }

    @Bean(name = "REMOVE_HOSTS_FROM_ORCHESTRATION_STATE")
    public Action<?, ?> removeHostsFromOrchestrationAction() {
        return new AbstractClusterAction<>(DecommissionResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DecommissionResult payload, Map<Object, Object> variables) {
                RemoveHostsRequest request = new RemoveHostsRequest(context.getStackId(), payload.getHostGroupNames(), payload.getHostNames());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "UPDATE_INSTANCE_METADATA_STATE")
    public Action<?, ?> updateInstanceMetadataAction() {
        return new AbstractClusterAction<>(RemoveHostsSuccess.class) {
            @Override
            protected void doExecute(ClusterViewContext context, RemoveHostsSuccess payload, Map<Object, Object> variables) {
                if (isEmpty(payload.getHostNames())) {
                    clusterDownscaleService.finalizeClusterScaleDown(context.getStackId(), null);
                } else {
                    clusterDownscaleService.finalizeClusterScaleDown(context.getStackId(), payload.getHostGroups());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(ClusterViewContext context) {
                return new StackEvent(ClusterDownscaleEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "DECOMISSION_FAILED_STATE")
    public Action<?, ?> updateInstanceMetadataDecomissionFailedAction() {
        return new AbstractClusterAction<>(DecommissionResult.class) {
            @Override
            protected void doExecute(ClusterViewContext context, DecommissionResult payload, Map<Object, Object> variables) {
                if (payload.getErrorDetails() != null) {
                    clusterDownscaleService.updateMetadataStatusToFailed(payload);
                    sendEvent(context, FAILURE_EVENT.event(), new StackFailureEvent(payload.getResourceId(), payload.getErrorDetails()));
                }
            }
        };
    }

    @Bean(name = "REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_STATE")
    public Action<?, ?> updateInstanceMetadataOrchestrationFailedAction() {
        return new AbstractClusterAction<>(RemoveHostsFailed.class) {
            @Override
            protected void doExecute(ClusterViewContext context, RemoveHostsFailed payload, Map<Object, Object> variables) {
                clusterDownscaleService.updateMetadataStatusToFailed(payload);
                sendEvent(context, FAILURE_EVENT.event(), new StackFailureEvent(payload.getResourceId(), payload.getException()));
            }
        };
    }

    @Bean(name = "CLUSTER_DOWNSCALE_FAILED_STATE")
    public Action<?, ?> clusterDownscalescaleFailedAction() {
        return new AbstractStackFailureAction<ClusterStartState, ClusterStartEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                clusterDownscaleService.handleClusterDownscaleFailure(context.getStackView().getId(), payload.getException());
                ClusterDownscaleFailedConclusionRequest request = new ClusterDownscaleFailedConclusionRequest(context.getStackView().getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    private Set<String> getHostNamesForPrivateIds(Set<Long> privateIds, Stack stack) {
        return privateIds.stream().map(privateId -> {
            Optional<InstanceMetaData> instanceMetadata = stackService.getInstanceMetadata(stack.getInstanceMetaDataAsList(), privateId);
            return instanceMetadata.map(InstanceMetaData::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private boolean isPayloadContainsAnyPrivateId(CollectDownscaleCandidatesResult payload) {
        return !isEmpty(payload.getPrivateIds());
    }

}
