package com.sequenceiq.externalizedcompute.flow;

import static com.sequenceiq.externalizedcompute.flow.chain.ExternalizedComputeClusterFlowChainTriggers.EXTERNALIZED_COMPUTE_CLUSTER_REINIT_TRIGGER_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_INITIATED_EVENT;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.exception.FlowNotAcceptedException;
import com.sequenceiq.cloudbreak.exception.FlowsAlreadyRunningException;
import com.sequenceiq.cloudbreak.ha.service.NodeValidator;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterStatusService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.flow.service.FlowNameFormatService;

@Service
public class ExternalizedComputeClusterFlowManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterFlowManager.class);

    private static final long WAIT_FOR_ACCEPT = 10L;

    @Inject
    private FlowNameFormatService flowNameFormatService;

    @Inject
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Inject
    private EventBus reactor;

    @Inject
    private NodeValidator nodeValidator;

    public FlowIdentifier triggerExternalizedComputeClusterCreation(ExternalizedComputeCluster externalizedComputeCluster) {
        LOGGER.info("Trigger Externalized Compute Cluster creation for: {}", externalizedComputeCluster);
        String selector = EXTERNALIZED_COMPUTE_CLUSTER_CREATE_INITIATED_EVENT.event();
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new ExternalizedComputeClusterEvent(selector, externalizedComputeCluster.getId(), actorCrn),
                externalizedComputeCluster.getName());
    }

    public FlowIdentifier triggerExternalizedComputeClusterReInitialization(ExternalizedComputeCluster externalizedComputeCluster) {
        String selector = EXTERNALIZED_COMPUTE_CLUSTER_REINIT_TRIGGER_EVENT;
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new ExternalizedComputeClusterEvent(selector, externalizedComputeCluster.getId(), actorCrn),
                externalizedComputeCluster.getName());
    }

    public FlowIdentifier triggerExternalizedComputeClusterDeletion(ExternalizedComputeCluster externalizedComputeCluster, boolean force) {
        LOGGER.info("Trigger Externalized Compute Cluster deletion for: {}", externalizedComputeCluster);
        externalizedComputeClusterStatusService.setStatus(externalizedComputeCluster.getId(), ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS,
                "Externalized compute cluster deletion triggered");
        String selector = EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_INITIATED_EVENT.event();
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return notify(selector, new ExternalizedComputeClusterDeleteEvent(selector, externalizedComputeCluster.getId(), actorCrn, force, false),
                externalizedComputeCluster.getName());
    }

    private FlowIdentifier notify(String selector, ExternalizedComputeClusterEvent acceptable, String identifier) {
        Map<String, Object> flowTriggerUserCrnHeader = Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, acceptable.getActorCrn());
        Event<Acceptable> event = eventFactory.createEventWithErrHandler(flowTriggerUserCrnHeader, acceptable);
        return notify(selector, identifier, event);
    }

    private FlowIdentifier notify(String selector, String identifier, Event<Acceptable> event) {
        nodeValidator.checkForRecentHeartbeat();
        reactor.notify(selector, event);
        try {
            FlowAcceptResult accepted = (FlowAcceptResult) event.getData().accepted().await(WAIT_FOR_ACCEPT, TimeUnit.SECONDS);
            if (accepted == null) {
                throw new FlowNotAcceptedException(String.format("Timeout happened when trying to start the flow for externalized compute cluster %s.",
                        event.getData().getResourceId()));
            } else {
                return switch (accepted.getResultType()) {
                    case ALREADY_EXISTING_FLOW ->
                            throw new FlowsAlreadyRunningException(String.format("Request not allowed, externalized compute cluster '%s' " +
                                            "already has a running operation. Running operation(s): [%s]",
                                    identifier,
                                    flowNameFormatService.formatFlows(accepted.getAlreadyRunningFlows())));
                    case RUNNING_IN_FLOW -> new FlowIdentifier(FlowType.FLOW, accepted.getAsFlowId());
                    case RUNNING_IN_FLOW_CHAIN -> new FlowIdentifier(FlowType.FLOW_CHAIN, accepted.getAsFlowChainId());
                };
            }
        } catch (InterruptedException e) {
            throw new CloudbreakServiceException(e.getMessage());
        }
    }
}
