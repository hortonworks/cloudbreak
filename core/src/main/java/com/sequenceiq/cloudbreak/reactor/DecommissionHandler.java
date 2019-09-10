package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.polling.PollingResult.SUCCESS;
import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.DECOMMISSION_ERROR_PHASE;
import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.UNKNOWN_ERROR_PHASE;

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
import com.sequenceiq.cloudbreak.cluster.service.DecommissionException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterKerberosService;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DecommissionHandler implements EventHandler<DecommissionRequest> {

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

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private ClusterKerberosService clusterKerberosService;

    @Inject
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DecommissionRequest.class);
    }

    @Override
    public void accept(Event<DecommissionRequest> event) {
        DecommissionRequest request = event.getData();
        DecommissionResult result;
        String hostGroupName = request.getHostGroupName();
        Set<String> hostNames = Collections.emptySet();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();
            hostNames = getHostNamesForPrivateIds(request, stack);
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

            KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
            Set<Node> decommissionedNodes = stackUtil.collectNodesFromHostnames(stack, decomissionedHostNames);
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            hostOrchestrator.stopClusterManagerAgent(gatewayConfig, decommissionedNodes, clusterDeletionBasedModel(stack.getId(), cluster.getId()),
                    kerberosDetailService.isAdJoinable(kerberosConfig), kerberosDetailService.isIpaJoinable(kerberosConfig));
            cleanUpFreeIpa(stack.getEnvironmentCrn(), hostsToRemove.keySet());
            decomissionedHostNames.stream().map(hostsToRemove::get).forEach(clusterDecomissionService::deleteHostFromCluster);
            clusterDecomissionService.restartStaleServices();

            result = new DecommissionResult(request, decomissionedHostNames);
        } catch (DecommissionException e) {
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, DECOMMISSION_ERROR_PHASE);
        } catch (Exception e) {
            LOGGER.info("Exception occurred during decommissioning.", e);
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, UNKNOWN_ERROR_PHASE);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private void cleanUpFreeIpa(String environmentCrn, Set<String> hostNames) {
        CleanupRequest cleanupRequest = new CleanupRequest();
        cleanupRequest.setHosts(hostNames);
        cleanupRequest.setEnvironmentCrn(environmentCrn);
        LOGGER.info("Sending cleanup request to FreeIPA: [{}]", cleanupRequest);
        try {
            CleanupResponse cleanupResponse = freeIpaV1Endpoint.cleanup(cleanupRequest);
            LOGGER.info("FreeIPA cleanup finished: [{}]", cleanupResponse);
        } catch (Exception e) {
            LOGGER.error("FreeIPA cleanup failed", e);
        }
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
