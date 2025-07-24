package com.sequenceiq.freeipa.flow.freeipa.downscale.action;

import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.FAIL_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.STARTING_DOWNSCALE_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.UPDATE_METADATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.UPDATE_METADATA_FOR_DELETION_REQUEST_FINISHED_EVENT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.ClientErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackCollectResourcesResult;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.PayloadConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.FailureDetails;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.cert.RevokeCertsResponse;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsRequest;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.event.dns.RemoveDnsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleState;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.collecthostnames.CollectAdditionalHostnamesResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.dnssoarecords.UpdateDnsSoaRecordsResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationSuccess;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removereplication.RemoveReplicationAgreementsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removeserver.RemoveServersResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stophealthagent.StopHealthAgentRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stoptelemetry.StopTelemetryResponse;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets.RemoveUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets.RemoveUserdataSecretsSuccess;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.ClusterProxyUpdateRegistrationFailedToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.DownscaleStackCollectResourcesResultToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.DownscaleStackResultToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RemoveDnsResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RemoveHostsResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RemoveServersResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.downscale.failure.RevokeCertsResponseToDownscaleFailureEventConverter;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.provision.event.clusterproxy.ClusterProxyUpdateRegistrationSuccess;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.termination.action.TerminationService;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.config.KerberosConfigUpdateService;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@Configuration
public class FreeIpaDownscaleActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaDownscaleActions.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private TerminationService terminationService;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Bean(name = "STARTING_DOWNSCALE_STATE")
    public Action<?, ?> startingDownscaleAction() {
        return new AbstractDownscaleAction<>(DownscaleEvent.class) {
            @Override
            protected void doExecute(StackContext context, DownscaleEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Starting downscale");
                List<String> instanceIds = payload.getInstanceIds();
                setInstanceIds(variables, instanceIds);
                String operationId = payload.getOperationId();
                setOperationId(variables, operationId);
                List<String> fqdns = getInstanceMetadataFromStack(stack, instanceIds).stream()
                        .map(InstanceMetaData::getDiscoveryFQDN)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                setDownscaleHosts(variables, fqdns);
                setRepair(variables, payload.isRepair());
                setChainedAction(variables, payload.isChained());
                setFinalChain(variables, payload.isFinalChain());
                setInstanceCountByGroup(variables, payload.getInstanceCountByGroup());
                LOGGER.info("Starting downscale {}", payload);
                sendEvent(context, STARTING_DOWNSCALE_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_UPDATE_METADATA_FOR_DELETION_REQUEST_STATE")
    public Action<?, ?> updateMetadataForDeletionRequestAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating metadata for deletion request");
                List<String> repairInstanceIds = getInstanceIds(variables);
                terminationService.requestDeletion(stack.getId(), repairInstanceIds);
                sendEvent(context, UPDATE_METADATA_FOR_DELETION_REQUEST_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_CLUSTERPROXY_REGISTRATION_STATE")
    public Action<?, ?> downscaleClusterProxyRegistrationAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating cluster proxy registration.");
                List<String> repairInstanceIds = getInstanceIds(variables);
                List<String> instanceIdsToRegister = stack.getNotDeletedInstanceMetaDataSet().stream()
                        .map(InstanceMetaData::getInstanceId)
                        .filter(instanceId -> !repairInstanceIds.contains(instanceId))
                        .collect(Collectors.toList());
                ClusterProxyUpdateRegistrationRequest request = new ClusterProxyUpdateRegistrationRequest(stack.getId(), instanceIdsToRegister);
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_COLLECT_ADDITIONAL_HOSTNAMES_STATE")
    public Action<?, ?> downscaleCollectAdditionalHostnamesAction() {
        return new AbstractDownscaleAction<>(ClusterProxyUpdateRegistrationSuccess.class) {
            @Override
            protected void doExecute(StackContext context, ClusterProxyUpdateRegistrationSuccess payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Collecting additional hostnames.");
                CollectAdditionalHostnamesRequest request = new CollectAdditionalHostnamesRequest(stack.getId());
                sendEvent(context, request.selector(), request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_STATE")
    public Action<?, ?> downscaleAddAdditionalHostnamesAction() {
        return new AbstractDownscaleAction<>(CollectAdditionalHostnamesResponse.class) {
            @Override
            protected void doExecute(StackContext context, CollectAdditionalHostnamesResponse payload, Map<Object, Object> variables) throws Exception {
                Stack stack = context.getStack();
                Set<String> knownHostnamesFromStack = stack.getNotDeletedInstanceMetaDataSet().stream()
                        .map(InstanceMetaData::getDiscoveryFQDN)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                List<String> currentHostsToRemove = getDownscaleHosts(variables);
                Set<String> newHostsToRemove = payload.getHostnames().stream()
                        .filter(hostname -> !currentHostsToRemove.contains(hostname))
                        .filter(hostname -> !knownHostnamesFromStack.contains(hostname))
                        .collect(Collectors.toSet());
                if (isRepair(variables) && !newHostsToRemove.isEmpty()) {
                    LOGGER.info("Adding hostnames [{}] to the list of hostnames to remove", newHostsToRemove);
                    List<String> allHostnamesToRemove = new LinkedList<>(currentHostsToRemove);
                    allHostnamesToRemove.addAll(newHostsToRemove);
                    setDownscaleHosts(variables, allHostnamesToRemove);
                }
                sendEvent(context, DOWNSCALE_ADD_ADDITIONAL_HOSTNAMES_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_STOP_HEALTH_AGENT_STATE")
    public Action<?, ?> stopHealthAgenAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {

            @Inject
            private FreeIpaLoadBalancerService loadBalancerService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                Optional<LoadBalancer> loadBalancer = loadBalancerService.findByStackId(payload.getResourceId());
                if (loadBalancer.isPresent()) {
                    stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Stopping health agent");
                    List<String> downscaleHosts = getDownscaleHosts(variables);
                    StopHealthAgentRequest stopHealthAgentRequest = new StopHealthAgentRequest(stack.getId(), downscaleHosts);
                    sendEvent(context, stopHealthAgentRequest);
                } else {
                    sendEvent(context, new StackEvent(DownscaleFlowEvent.STOP_HEALTH_AGENT_FINISHED.event(), stack.getId()));
                }
            }
        };
    }

    @Bean(name = "DOWNSCALE_STOP_TELEMETRY_STATE")
    public Action<?, ?> stopTelemetryAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Stopping telemetry");
                List<String> repairInstanceIds = getInstanceIds(variables);
                StopTelemetryRequest stopTelemetryRequest = new StopTelemetryRequest(stack.getId(), repairInstanceIds);
                sendEvent(context, stopTelemetryRequest.selector(), stopTelemetryRequest);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_USERDATA_SECRETS_STATE")
    public Action<?, ?> deleteUserdataSecretsAction() {
        return new AbstractDownscaleAction<>(StopTelemetryResponse.class) {
            @Override
            protected void doExecute(StackContext context, StopTelemetryResponse payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                DetailedEnvironmentResponse environment = cachedEnvironmentClientService.getByCrn(stack.getEnvironmentCrn());
                if (environment.isEnableSecretEncryption()) {
                    stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Removing userdata secrets");
                    RemoveUserdataSecretsRequest request = new RemoveUserdataSecretsRequest(stack.getId(), context.getCloudContext(),
                            context.getCloudCredential(), getDownscaleHosts(variables));
                    sendEvent(context, request.selector(), request);
                } else {
                    LOGGER.info("Skipping userdata secret deletion, since secret encryption is not enabled.");
                    sendEvent(context, new RemoveUserdataSecretsSuccess(stack.getId()));
                }
            }
        };
    }

    @Bean(name = "DOWNSCALE_COLLECT_RESOURCES_STATE")
    public Action<?, ?> collectResourcesAction() {
        return new AbstractDownscaleAction<>(RemoveUserdataSecretsSuccess.class) {
            @Override
            protected void doExecute(StackContext context, RemoveUserdataSecretsSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Collecting resources");
                List<String> repairInstanceIds = getInstanceIds(variables);
                List<CloudResource> cloudResources = getCloudResources(stack);
                List<CloudInstance> cloudInstances = getCloudInstances(stack, repairInstanceIds);
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
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Decommissioning instances");
                List<String> repairInstanceIds = getInstanceIds(variables);
                List<CloudResource> cloudResources = getCloudResources(stack);
                List<CloudInstance> cloudInstances = getNonTerminatedCloudInstances(stack, repairInstanceIds);
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
                stackUpdater.updateStackStatus(context.getStack(), getInProgressStatus(variables), "Removing servers");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getDownscaleHosts(variables));
                RemoveServersRequest request = new RemoveServersRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_REPLICATION_AGREEMENTS_STATE")
    public Action<?, ?> removeReplicationAgreementsAction() {
        return new AbstractDownscaleAction<>(RemoveServersResponse.class) {
            @Override
            protected void doExecute(StackContext context, RemoveServersResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack(), getInProgressStatus(variables), "Removing servers");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getDownscaleHosts(variables));
                RemoveReplicationAgreementsRequest request = new RemoveReplicationAgreementsRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REVOKE_CERTS_STATE")
    public Action<?, ?> revokeCertsAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack(), getInProgressStatus(variables), "Revoking certificates");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getDownscaleHosts(variables));
                RevokeCertsRequest request = new RevokeCertsRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_DNS_ENTRIES_STATE")
    public Action<?, ?> removeDnsEntriesAction() {
        return new AbstractDownscaleAction<>(RevokeCertsResponse.class) {
            @Override
            protected void doExecute(StackContext context, RevokeCertsResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack(), getInProgressStatus(variables), "Remove DNS entries");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getDownscaleHosts(variables));
                RemoveDnsRequest request = new RemoveDnsRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_UPDATE_DNS_SOA_RECORDS_STATE")
    public Action<?, ?> updateDnsSoaRecordsAction() {
        return new AbstractDownscaleAction<>(RemoveDnsResponse.class) {
            @Override
            protected void doExecute(StackContext context, RemoveDnsResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack(), getInProgressStatus(variables), "Update DNS SOA records");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getDownscaleHosts(variables));
                UpdateDnsSoaRecordsRequest request = new UpdateDnsSoaRecordsRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_REMOVE_HOSTS_FROM_ORCHESTRATION_STATE")
    public Action<?, ?> removeHostsFromOrchestrationAction() {
        return new AbstractDownscaleAction<>(UpdateDnsSoaRecordsResponse.class) {
            @Override
            protected void doExecute(StackContext context, UpdateDnsSoaRecordsResponse payload, Map<Object, Object> variables) {
                stackUpdater.updateStackStatus(context.getStack(), getInProgressStatus(variables), "Removing hosts from orchestration");
                CleanupEvent cleanupEvent = buildCleanupEvent(context, getDownscaleHosts(variables));
                RemoveHostsFromOrchestrationRequest request = new RemoveHostsFromOrchestrationRequest(cleanupEvent);
                sendEvent(context, request);
            }
        };
    }

    @Bean(name = "DOWNSCALE_UPDATE_METADATA_STATE")
    public Action<?, ?> updateMetadataAction() {
        return new AbstractDownscaleAction<>(RemoveHostsFromOrchestrationSuccess.class) {
            @Inject
            private InstanceGroupService instanceGroupService;

            @Override
            protected void doExecute(StackContext context, RemoveHostsFromOrchestrationSuccess payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating metadata");
                List<String> repairInstanceIds = getInstanceIds(variables);
                terminationService.finalizeTermination(stack.getId(), repairInstanceIds);
                terminationService.finalizeTerminationForInstancesWithoutInstanceIds(stack.getId());
                if (!isRepair(variables)) {
                    int nodeCount = getInstanceCountByGroup(variables);
                    instanceGroupService.findByStackId(stack.getId()).forEach(instanceGroup -> {
                        instanceGroup.setNodeCount(nodeCount);
                        instanceGroupService.save(instanceGroup);
                    });
                }
                sendEvent(context, UPDATE_METADATA_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
            }
        };
    }

    @Bean(name = "DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_STATE")
    public Action<?, ?> updateKerberosNameserversConfigAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Inject
            private KerberosConfigUpdateService kerberosConfigUpdateService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating kerberos nameserver config");
                try {
                    kerberosConfigUpdateService.updateNameservers(stack.getId());
                    sendEvent(context, DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                } catch (Exception e) {
                    LOGGER.error("Failed to update the kerberos nameserver config", e);
                    sendEvent(context, DOWNSCALE_UPDATE_KERBEROS_NAMESERVERS_CONFIG_FAILED_EVENT.selector(),
                            new DownscaleFailureEvent(stack.getId(), "Updating kerberos nameserver config", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_STATE")
    public Action<?, ?> updateEnvironmentStackConfigAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Inject
            private EnvironmentEndpoint environmentEndpoint;

            @Inject
            private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getInProgressStatus(variables), "Updating environment stack config");
                try {
                    if (!isRepair(variables) || !isChainedAction(variables) || isFinalChain(variables)) {
                        ThreadBasedUserCrnProvider.doAsInternalActor(
                                () -> environmentEndpoint.updateConfigsInEnvironmentByCrn(stack.getEnvironmentCrn()));
                    }
                    sendEvent(context, DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FINISHED_EVENT.selector(), new StackEvent(stack.getId()));
                } catch (ClientErrorException e) {
                    String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                    LOGGER.error("Failed to update the stack config due to {}", errorMessage, e);
                    sendEvent(context, DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT.selector(),
                            new DownscaleFailureEvent(stack.getId(), "Updating environment stack config", Set.of(), Map.of(), e));
                } catch (Exception e) {
                    LOGGER.error("Failed to update the stack config", e);
                    sendEvent(context, DOWNSCALE_UPDATE_ENVIRONMENT_STACK_CONFIG_FAILED_EVENT.selector(),
                            new DownscaleFailureEvent(stack.getId(), "Updating environment stack config", Set.of(), Map.of(), e));
                }
            }
        };
    }

    @Bean(name = "DOWNSCALE_FINISHED_STATE")
    public Action<?, ?> downscaleFinsihedAction() {
        return new AbstractDownscaleAction<>(StackEvent.class) {
            @Inject
            private OperationService operationService;

            @Inject
            private EnvironmentService environmentService;

            @Override
            protected void doExecute(StackContext context, StackEvent payload, Map<Object, Object> variables) {
                Stack stack = context.getStack();
                stackUpdater.updateStackStatus(stack, getDownscaleCompleteStatus(variables), "Downscale complete");
                if (!isChainedAction(variables)) {
                    environmentService.setFreeIpaNodeCount(stack.getEnvironmentCrn(),  stack.getNotDeletedInstanceMetaDataSet().size());
                }
                if (shouldCompleteOperation(variables)) {
                    SuccessDetails successDetails = new SuccessDetails(stack.getEnvironmentCrn());
                    successDetails.getAdditionalDetails().put("Hosts", getDownscaleHosts(variables));
                    operationService.completeOperation(stack.getAccountId(), getOperationId(variables), List.of(successDetails), Collections.emptyList());
                }
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
            protected StackContext createFlowContext(FlowParameters flowParameters, StateContext<DownscaleState, DownscaleFlowEvent> stateContext,
                    DownscaleFailureEvent payload) {
                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());
                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected void doExecute(StackContext context, DownscaleFailureEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Downscale failed with payload: " + payload);
                Stack stack = context.getStack();
                String errorReason = getErrorReason(payload.getException());
                stackUpdater.updateStackStatus(context.getStack(), getFailedStatus(variables), errorReason);
                String environmentCrn = stack.getEnvironmentCrn();
                SuccessDetails successDetails = new SuccessDetails(environmentCrn);
                successDetails.getAdditionalDetails()
                        .put(payload.getFailedPhase(), payload.getSuccess() == null ? List.of() : new ArrayList<>(payload.getSuccess()));
                String message = "Downscale failed during " + payload.getFailedPhase();
                FailureDetails failureDetails = new FailureDetails(environmentCrn, message);
                if (payload.getFailureDetails() != null) {
                    failureDetails.getAdditionalDetails().putAll(payload.getFailureDetails());
                }
                operationService.failOperation(stack.getAccountId(), getOperationId(variables), message, List.of(successDetails), List.of(failureDetails));
                enableStatusChecker(stack, "Failed downscaling FreeIPA");
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