package com.sequenceiq.environment.environment.service.stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.flow.config.update.config.EnvStackConfigUpdatesFlowConfig;
import com.sequenceiq.environment.exception.StackOperationFailedException;
import com.sequenceiq.environment.store.EnvironmentInMemoryStateStore;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.flow.service.flowlog.FlowLogDBService;

@Service
public class StackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackService.class);

    private static final List<Status> DELETED_STATUS = List.of(
            Status.DELETE_IN_PROGRESS,
            Status.DELETE_COMPLETED,
            Status.DELETED_ON_PROVIDER_SIDE,
            Status.DELETE_FAILED);

    private final StackV4Endpoint stackV4Endpoint;

    private final FlowCancelService flowCancelService;

    private final FlowLogDBService flowLogDBService;

    private final WebApplicationExceptionMessageExtractor messageExtractor;

    private final FlowEndpoint flowEndpoint;

    public StackService(
            StackV4Endpoint stackV4Endpoint,
            FlowCancelService flowCancelService,
            FlowLogDBService flowLogDBService,
            WebApplicationExceptionMessageExtractor messageExtractor,
            FlowEndpoint flowEndpoint) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.flowCancelService = flowCancelService;
        this.flowLogDBService = flowLogDBService;
        this.messageExtractor = messageExtractor;
        this.flowEndpoint = flowEndpoint;
    }

    public FlowIdentifier triggerSaltUpdateForStack(String stackName) {
        try {
            return stackV4Endpoint.updateSaltByName(0L, stackName, ThreadBasedUserCrnProvider.getAccountId(), false);
        } catch (WebApplicationException wae) {
            LOGGER.info("Unable to start stack update for stack {}.  Message is {}", stackName, messageExtractor.getErrorMessage(wae));
            throw wae;
        }
    }

    public void triggerConfigUpdateForStack(String stackCrn) {
        try {
            stackV4Endpoint.updatePillarConfigurationByCrn(0L, stackCrn);
        } catch (WebApplicationException wae) {
            LOGGER.info("Unable to start config update for stack {}.  Message is {}", stackCrn, messageExtractor.getErrorMessage(wae));
            throw wae;
        }
    }

    public void cancelRunningStackConfigUpdates(EnvironmentView environment) {
        List<FlowLog> flowLogs = flowLogDBService.findAllByResourceIdAndFinalizedIsFalseOrderByCreatedDesc(environment.getId());
        if (!flowLogs.isEmpty() && flowLogs.get(0).isFlowType(EnvStackConfigUpdatesFlowConfig.class)) {
            LOGGER.info("Canceling running Stack config update flow for environment {}", environment.getResourceCrn());
            EnvironmentInMemoryStateStore.put(environment.getId(), PollGroup.CANCELLED);
            flowCancelService.cancelFlowSilently(flowLogs.get(0));
        } else {
            LOGGER.debug("No running Stack config update flow to cancel");
        }
    }

    public List<FlowIdentifier> updateLoadBalancer(Set<String> names) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<FlowIdentifier> flowIdentifiers = new ArrayList<>();
        for (String name : names) {
            try {
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> {
                            FlowIdentifier flowidentifier = stackV4Endpoint.updateLoadBalancersInternal(0L, name, initiatorUserCrn);
                            flowIdentifiers.add(flowidentifier);
                        });
            } catch (WebApplicationException e) {
                String errorMessage = messageExtractor.getErrorMessage(e);
                LOGGER.error("Failed update load balancer for stack {} due to: '{}'.", name, errorMessage, e);
                throw e;
            }
        }
        return flowIdentifiers;
    }

    public FlowCheckResponse checkFlow(FlowIdentifier flowIdentifier) {
        try {
            LOGGER.debug("Getting stack operation status for flowIdentifier {}", flowIdentifier);
            return switch (flowIdentifier.getType()) {
                case FLOW -> flowEndpoint.hasFlowRunningByFlowId(flowIdentifier.getPollableId());
                case FLOW_CHAIN -> flowEndpoint.hasFlowRunningByChainId(flowIdentifier.getPollableId());
                case NOT_TRIGGERED -> throw new IllegalStateException("Stack flow is not triggered");
            };
        } catch (WebApplicationException e) {
            String errorMessage = messageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to get operation status '{}' due to: '{}'", flowIdentifier, errorMessage, e);
            throw new StackOperationFailedException(errorMessage, e);
        }
    }

    public List<StackViewV4Response> getAllNotDeletedClustersByEnvironmentCrn(String environmentCrn) {
        StackViewV4Responses stackViewV4Responses = stackV4Endpoint.list(0L, environmentCrn, false);
        return stackViewV4Responses.getResponses()
                .stream()
                .filter(stack -> !DELETED_STATUS.contains(stack.getCluster().getStatus()))
                .collect(Collectors.toList());
    }

    public FlowIdentifier triggerUserDefinedTagsUpdate(String crn, Map<String, String> userDefinedTags) {
        try {
            LOGGER.debug("Calling triggerUserDefinedTagsUpdateInternal endpoint for stack {} with tags {}", crn, userDefinedTags);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> stackV4Endpoint.triggerUserDefinedTagsUpdateInternal(0L, crn, userDefinedTags)
            );
        } catch (WebApplicationException e) {
            String errorMessage = messageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to update user defined tags for stack: {} due to: {}", crn, errorMessage);
            throw new StackOperationFailedException(errorMessage, e);
        }
    }

    public FlowIdentifier updatePillarConfigurationByCrn(String stackCrn) {
        try {
            LOGGER.debug("Updating pillar configuration for cluster {}", stackCrn);
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.updatePillarConfigurationByCrn(0L, stackCrn));
        } catch (WebApplicationException e) {
            String errorMessage = messageExtractor.getErrorMessage(e);
            LOGGER.error("Failed to update pillar configuration for cluster {} due to: '{}'.", stackCrn, errorMessage, e);
            throw new StackOperationFailedException(errorMessage, e);
        }
    }
}
