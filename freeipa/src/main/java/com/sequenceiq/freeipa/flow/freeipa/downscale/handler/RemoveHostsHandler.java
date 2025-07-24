package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent.REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.removehosts.RemoveHostsFromOrchestrationSuccess;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class RemoveHostsHandler implements EventHandler<RemoveHostsFromOrchestrationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveHostsHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private EventBus eventBus;

    @Inject
    private BootstrapService bootstrapService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RemoveHostsFromOrchestrationRequest.class);
    }

    @Override
    public void accept(Event<RemoveHostsFromOrchestrationRequest> removeHostsRequestEvent) {
        RemoveHostsFromOrchestrationRequest request = removeHostsRequestEvent.getData();
        Set<String> hostNames = request.getHosts();
        Selectable result;
        try {
            if (!hostNames.isEmpty()) {
                Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
                PollingResult orchestratorRemovalPollingResult =
                        removeHostsFromOrchestrator(stack, new ArrayList<>(hostNames));
                if (!orchestratorRemovalPollingResult.isSuccess()) {
                    LOGGER.warn("Can not remove hosts from orchestrator: {}", hostNames);
                }
                updateMinionMultiMasterConfig(stack, hostNames);
            }
            result = new RemoveHostsFromOrchestrationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Failed to remove hosts from orchestration", e);
            result = new DownscaleFailureEvent(REMOVE_HOSTS_FROM_ORCHESTRATION_FAILED_EVENT.event(),
                    request.getResourceId(), "Removing host from orchestration", Set.of(), Map.of(), e);
        }
        eventBus.notify(result.selector(), new Event<>(removeHostsRequestEvent.getHeaders(), result));
    }

    private void updateMinionMultiMasterConfig(Stack stack, Set<String> hostNames) throws CloudbreakOrchestratorException {
        Set<InstanceMetaData> remainingInstances = stack.getNotDeletedInstanceMetaDataSet().stream()
                .filter(metadata -> Objects.nonNull(metadata.getDiscoveryFQDN()))
                .filter(metadata -> !hostNames.contains(metadata.getDiscoveryFQDN()))
                .collect(Collectors.toSet());
        bootstrapService.reBootstrap(stack, remainingInstances);
    }

    private PollingResult removeHostsFromOrchestrator(Stack stack, List<String> hostNames) throws CloudbreakException {
        LOGGER.debug("Remove hosts from orchestrator: [{}]", hostNames);
        try {
            Multimap<String, String> removeNodePrivateIPsByFQDN = stack.getAllInstanceMetaDataList().stream()
                    .filter(instanceMetaData -> Objects.nonNull(instanceMetaData.getDiscoveryFQDN()))
                    .filter(instanceMetaData ->
                            hostNames.stream()
                                    .anyMatch(hn -> hn.equals(instanceMetaData.getDiscoveryFQDN())))
                    .collect(Multimaps.toMultimap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getPrivateIp, HashMultimap::create));
            Set<InstanceMetaData> remainingInstanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet().stream()
                    .filter(instanceMetaData -> !shouldRemove(instanceMetaData, removeNodePrivateIPsByFQDN))
                    .collect(Collectors.toSet());
            Set<InstanceMetaData> invalidInstanceMetadata = remainingInstanceMetaDatas.stream()
                    .filter(instanceMetaData -> Objects.isNull(instanceMetaData.getDiscoveryFQDN()))
                    .collect(Collectors.toSet());
            Set<InstanceMetaData> validRemainingNodes = remainingInstanceMetaDatas.stream()
                    .filter(instanceMetaData -> Objects.nonNull(instanceMetaData.getDiscoveryFQDN()))
                    .collect(Collectors.toSet());
            Set<Node> remainingNodes = validRemainingNodes.stream()
                    .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                            im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                    .collect(Collectors.toSet());
            List<GatewayConfig> remainingGatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, validRemainingNodes);
            LOGGER.debug("Tearing down [{}]. The following were dropped because they did not contain a FQDN [{}]. The remaining nodes are [{}].",
                    removeNodePrivateIPsByFQDN, invalidInstanceMetadata, remainingNodes);
            hostOrchestrator.tearDown(stack, remainingGatewayConfigs, removeNodePrivateIPsByFQDN, remainingNodes,
                    new StackBasedExitCriteriaModel(stack.getId()));
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.info("Failed to delete orchestrator components while decommissioning: ", e);
            throw new CloudbreakException("Failed to delete orchestrator components while decommissioning: ", e);
        }
        return SUCCESS;
    }

    private boolean shouldRemove(InstanceMetaData instanceMetaData, Multimap<String, String> removeNodePrivateIPsByFQDN) {
        if (removeNodePrivateIPsByFQDN.containsValue(instanceMetaData.getPrivateIp())) {
            if (instanceMetaData.getDiscoveryFQDN() == null) {
                return true;
            } else {
                return removeNodePrivateIPsByFQDN.containsKey(instanceMetaData.getDiscoveryFQDN());
            }
        } else {
            return false;
        }
    }
}
