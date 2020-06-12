package com.sequenceiq.freeipa.flow.freeipa.downscale.action;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.STARTING_DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.UPDATE_METADATA_FINISHED_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.ClusterProxyUpdateRegistrationFailedToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.DownscaleStackCollectResourcesResultToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RemoveDnsResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RemoveHostsResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.DownscaleStackResultToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RemoveServersResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RevokeCertsResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;

@Configuration
public class FreeIpaDownscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDownscaleActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private TerminationService terminationService;

    @Bean(name = "STARTING_DOWNSCALE_STATE")
    public Action<?, ?> startingDownscaleAction() {
        return new AbstractDownscaleAction<>(DownscaleEvent.class) {
            @Override
            protected void doExecute(StackContext context, DownscaleEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                List<String> instanceIds = payload.getInstanceIds();
                setInstanceIds(variables, instanceIds);
                String operationId = payload.getOperationId();
                setOperationId(variables, operationId);
                List<String> fqdns = getInstanceMetadataFromStack(stack, instanceIds).stream()
                        .map(InstanceMetaData::getDiscoveryFQDN)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                setHosts(variables, fqdns);
                LOGGER.info("Starting downscale of stack {} instances [{}]", stack.getId(), instanceIds);
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Starting downscale");
                sendEvent(context, STARTING_DOWNSCALE_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_CLUSTERPROXY_REGISTRATION_STATE")
    public Action<?, ?> downscaleClusterProxyRegistrationAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Updating cluster proxy registration.");
                List<String> instanceIds = getInstanceIds(variables);
                List<String> instanceIdsToRegister = stack.getNotDeletedInstanceMetaDataList().stream()
                        .map(InstanceMetaData::getInstanceId)
                        .filter(instanceId -> !instanceIds.contains(instanceId))
                        .collect(Collectors.toList());
                ClusterProxyUpdateRegistrationRequest request = new ClusterProxyUpdateRegistrationRequest(stack.getId(), instanceIdsToRegister);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_STOP_TELEMETRY_STATE")
    public Action<?, ?> stopTelemetryAction() {
        return new AbstractDownscaleAction<>(ClusterProxyUpdateRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, ClusterProxyUpdateRegistrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Stopping telemetry");
                List<String> instanceIds = getInstanceIds(variables);
                StopTelemetryRequest stopTelemetryRequest = new StopTelemetryRequest(stack.getId(), instanceIds);
                sendEvent(context, stopTelemetryRequest.selector(), stopTelemetryRequest);
            }
        };
    }

    @Bean(name = "DOWNSCALE_COLLECT_RESOURCES_STATE")
    public Action<?, ?> collectResourcesAction() {
        return new AbstractDownscaleAction<>(StopTelemetryResponse.class) {
            @Override
            protected void doExecute(StackContext context, StopTelemetryResponse payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Collecting resources");
                List<String> instanceIds = getInstanceIds(variables);
                List<CloudResource> cloudResources = getCloudResources(stack);
                List<CloudInstance> cloudInstances = getCloudInstances(stack, instanceIds);
                DownscaleStackCollectResourcesRequest request = new DownscaleStackCollectResourcesRequest(context.getCloudContext(),
                        context.getCloudCredential(), context.getCloudStack(), cloudResources, cloudInstances);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_INSTANCES_STATE")
    public Action<?, ?> removeInstancesAction() {
        return new AbstractDownscaleAction<>(DownscaleStackCollectResourcesResult.class) {
            @Override
            protected void doExecute(StackContext context, DownscaleStackCollectResourcesResult payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Decommissioning instances");
                List<String> instanceIds = getInstanceIds(variables);
                List<CloudResource> cloudResources = getCloudResources(stack);
                List<CloudInstance> cloudInstances = getCloudInstances(stack, instanceIds);
                DownscaleStackRequest request = new DownscaleStackRequest(context.getCloudContext(), context.getCloudCredential(), context.getCloudStack(),
                        cloudResources, cloudInstances, payload.getResourcesToScale());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_SERVERS_STATE")
    public Action<?, ?> removeServersAction() {
        return new AbstractDownscaleAction<>(DownscaleStackResult.class) {
            @Override
            protected void doExecute(StackContext context, DownscaleStackResult payload, Map<Object, Object> variables) {
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getHosts(variables));
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Removing servers");
                RemoveServersRequest request = new RemoveServersRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REVOKE_CERTS_STATE")
    public Action<?, ?> revokeCertsAction() {
        return new AbstractDownscaleAction<>(RemoveServersResponse.class) {
            @Override
            protected void doExecute(StackContext context, RemoveServersResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Revoking certificates");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getHosts(variables));
                RevokeCertsRequest request = new RevokeCertsRequest(cleanupEvent, context.getStack());
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_DNS_ENTRIES_STATE")
    public Action<?, ?> removeDnsEntriesAction() {
        return new AbstractDownscaleAction<>(RevokeCertsResponse.class) {
            @Override
            protected void doExecute(StackContext context, RevokeCertsResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Remove DNS entries");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getHosts(variables));
                RemoveDnsRequest request = new RemoveDnsRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_HOSTS_FROM_ORCHESTRATION_STATE")
    public Action<?, ?> removeHostsFromOrchestrationAction() {
        return new AbstractDownscaleAction<>(RemoveDnsResponse.class) {
            @Override
            protected void doExecute(StackContext context, RemoveDnsResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Removing hosts from orchestration");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getHosts(variables));
                RemoveHostsFromOrchestrationRequest request = new RemoveHostsFromOrchestrationRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_UPDATE_METADATA_STATE")
    public Action<?, ?> updateMetadataAction() {
        return new AbstractDownscaleAction<>(RemoveHostsFromOrchestrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, RemoveHostsFromOrchestrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_IN_PROGRESS, "Updating metadata");
                List<String> instanceIds = getInstanceIds(variables);
                terminationService.finalizeTermination(stack.getId(), instanceIds);
                sendEvent(context, UPDATE_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_FINISHED_STATE")
    public Action<?, ?> downscaleFinsihedAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Inject
            private OperationService operationService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                successDetails.getAdditionalDetails().put("Hosts", getHosts(variables));
                stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.DOWNSCALE_COMPLETED, "Downscale complete");
                operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(successDetails), Collections.emptyList());
                sendEvent(context, DOWNSCALE_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_FAIL_STATE")
    public Action<?, ?> downscaleFailureAction() {
        return new AbstractDownscaleAction<>(DownscaleFailureEvent.class) {

            @Inject
            private OperationService operationService;

            @Override
            protected Object getFailurePayload(DownscaleFailureEvent payload, Optional<StackContext> flowContext, Exception ex) {
                return null;
            }

            @Override
            protected void doExecute(StackContext context, DownscaleFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Downscale failed with payload: " + payload);
                Stack stack = context.getStack();
                String environmentCrn = stack.getEnvironmentCrn();
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Downscale failed during " + payload.getFailedPhase();
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                String errorReason = payload.getException() == null ? "Unknown error" : payload.getException().getMessage();
                stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.DOWNSCALE_FAILED, errorReason);
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                sendEvent(context, FAIL_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected void initPayloadConverterMap(List<PayloadConverter<DownscaleFailureEvent>> payloadConverters) {
                payloadConverters.add(new ClusterProxyUpdateRegistrationFailedToDownscaleFailureEventConverter());
                payloadConverters.add(new DownscaleStackCollectResourcesResultToDownscaleFailureEventConverter());
                payloadConverters.add(new DownscaleStackResultToDownscaleFailureEventConverter());
                payloadConverters.add(new RemoveServersResponseToDownscaleFailureEventConverter());
                payloadConverters.add(new RemoveDnsResponseToDownscaleFailureEventConverter());
                payloadConverters.add(new RemoveHostsResponseToDownscaleFailureEventConverter());
                payloadConverters.add(new RevokeCertsResponseToDownscaleFailureEventConverter());
            }
        };
    }

    private CleanupEvent buildCleanupEvent(StackContext context, List<String> hosts) {
        Stack stack = context.getStack();
        Set<String> users = Set.of();
        Set<String> hostsSet = hosts.stream().collect(Collectors.toSet());
        Set<String> roles = Set.of();
        Set<String> ips = Set.of();
        Set<String> statesToSkip = Set.of();
        String accountId = stack.getAccountId();
        String operationId = "";
        String clusterName = "";
        String environmentCrn = stack.getEnvironmentCrn();
        return new CleanupEvent(stack.getId(), users, hostsSet, roles, ips, statesToSkip, accountId, operationId, clusterName, environmentCrn);
    }
}
