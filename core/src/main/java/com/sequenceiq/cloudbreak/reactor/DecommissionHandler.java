package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
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
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

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
    private HostMetadataService hostMetadataService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackUtil stackUtil;

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
            Cluster cluster = stack.getCluster();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(cluster.getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
            Map<String, HostMetadata> hostsToRemove = clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
            Set<String> decomissionedHostNames;
            if (skipClusterDecomission(request, hostsToRemove)) {
                decomissionedHostNames = hostNames;
            } else {
                executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
                Set<HostMetadata> decomissionedHostMetadatas = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
                decomissionedHostMetadatas.forEach(hostMetadata -> hostMetadataService.delete(hostMetadata));
                decomissionedHostNames = decomissionedHostMetadatas.stream().map(HostMetadata::getHostName).collect(Collectors.toSet());
            }
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());

            Set<Node> decommissionedNodes = stackUtil.collectNodesFromHostnames(stack, decomissionedHostNames);
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            hostOrchestrator.stopClusterManagerAgent(gatewayConfig, decommissionedNodes, clusterDeletionBasedModel(stack.getId(), cluster.getId()),
                    cluster.isAdJoinable(), cluster.isIpaJoinable());
            decomissionedHostNames.stream().parallel().map(hostsToRemove::get).forEach(clusterDecomissionService::deleteHostFromCluster);

            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            PollingResult orchestratorRemovalPollingResult =
                    removeHostsFromOrchestrator(stack, new ArrayList<>(decomissionedHostNames), hostOrchestrator, allGatewayConfigs);
            if (!isSuccess(orchestratorRemovalPollingResult)) {
                LOGGER.debug("Can not remove hosts from orchestrator: {}", decomissionedHostNames);
            }
            result = new DecommissionResult(request, decomissionedHostNames);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.info("Exception occurred during decommissioning.", e);
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
            return instanceMetadata.map(InstanceMetaData::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private void executePreTerminationRecipes(Stack stack, String hostGroupName, Set<String> hostNames) {
        try {
            Optional<HostGroup> hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupName);
            recipeEngine.executePreTerminationRecipes(stack, hostGroup.map(Collections::singleton).orElse(Collections.emptySet()), hostNames);
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
