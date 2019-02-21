package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isSuccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DecommissionHandler implements ReactorEventHandler<DecommissionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecommissionHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DecommissionRequest.class);
    }

    @Override
    public void accept(Event<DecommissionRequest> event) {
        DecommissionRequest request = event.getData();
        DecommissionResult result;
        String hostGroupName = request.getHostGroupName();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();
            Set<String> hostNames = getHostNamesForPrivateIds(request, stack);
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
            Map<String, HostMetadata> hostsToRemove = clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
            Set<String> decomissionedHostNames;
            if (skipClusterDecomission(request, hostsToRemove)) {
                decomissionedHostNames = hostNames;
            } else {
                executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
                Set<HostMetadata> decomissionedHostMetadatas = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
                decomissionedHostMetadatas.forEach(hostMetadata -> hostMetadataRepository.delete(hostMetadata));
                decomissionedHostNames = decomissionedHostMetadatas.stream().map(HostMetadata::getHostName).collect(Collectors.toSet());
            }
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            PollingResult orchestratorRemovalPollingResult =
                    removeHostsFromOrchestrator(stack, new ArrayList<>(decomissionedHostNames), hostOrchestrator, allGatewayConfigs);
            if (!isSuccess(orchestratorRemovalPollingResult)) {
                LOGGER.debug("Can not remove hosts from orchestrator: {}", decomissionedHostNames);
            }
            result = new DecommissionResult(request, decomissionedHostNames);
        } catch (Exception e) {
            result = new DecommissionResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private boolean skipClusterDecomission(DecommissionRequest request, Map<String, HostMetadata> hostsToRemove) {
        return hostsToRemove.isEmpty() || request.getDetails() != null && request.getDetails().isForced();
    }

    private Set<String> getHostNamesForPrivateIds(DecommissionRequest request, Stack stack) {
        return request.getPrivateIds().stream().map(privateId -> {
            Optional<InstanceMetaData> instanceMetadata = stackService.getInstanceMetadata(stack.getInstanceMetaDataAsList(), privateId);
            if (instanceMetadata.isPresent()) {
                return instanceMetadata.get().getDiscoveryFQDN();
            } else {
                return null;
            }
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private void executePreTerminationRecipes(Stack stack, String hostGroupName, Set<String> hostNames) {
        try {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
            recipeEngine.executePreTerminationRecipes(stack, Collections.singleton(hostGroup), hostNames);
        } catch (Exception ex) {
            LOGGER.warn(ex.getLocalizedMessage(), ex);
        }
    }

    private PollingResult removeHostsFromOrchestrator(Stack stack, List<String> hostNames, HostOrchestrator hostOrchestrator,
            List<GatewayConfig> allGatewayConfigs) throws CloudbreakException {
        LOGGER.debug("Remove hosts from orchestrator: {}", hostNames);
        try {
            Map<String, String> privateIpsByFQDN = new HashMap<>();
            stack.getInstanceMetaDataAsList().stream()
                    .filter(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null)
                    .filter(instanceMetaData ->
                            hostNames.stream()
                                    .anyMatch(hn -> hn.equals(instanceMetaData.getDiscoveryFQDN())))
                    .forEach(instanceMetaData -> privateIpsByFQDN.put(instanceMetaData.getDiscoveryFQDN(), instanceMetaData.getPrivateIp()));
            hostOrchestrator.tearDown(allGatewayConfigs, privateIpsByFQDN);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.info("Failed to delete orchestrator components while decommissioning: ", e);
            throw new CloudbreakException("Failed to delete orchestrator components while decommissioning: ", e);
        }
        return SUCCESS;
    }
}
