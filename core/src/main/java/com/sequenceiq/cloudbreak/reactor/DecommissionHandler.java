package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.DECOMMISSION_ERROR_PHASE;
import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.UNKNOWN_ERROR_PHASE;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.DecommissionException;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationFailedException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.RuntimeVersionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private FreeIpaCleanupService freeIpaCleanupService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private RuntimeVersionService runtimeVersionService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DecommissionRequest.class);
    }

    @Override
    public void accept(Event<DecommissionRequest> event) {
        DecommissionRequest request = event.getData();
        DecommissionResult result;
        Set<String> hostNames = Collections.emptySet();
        boolean forced = request.getDetails() != null && request.getDetails().isForced();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();
            hostNames = getHostNamesForPrivateIds(request, stack);
            Map<String, InstanceMetaData> hostsToRemove = getHostsForRemoval(request.getHostGroupName(), hostNames, stack, clusterDecomissionService);
            updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.DELETE_REQUESTED, "delete requested for instance");
            Optional<String> runtimeVersion = runtimeVersionService.getRuntimeVersion(stack.getCluster().getId());
            if (entitlementService.bulkHostsRemovalFromCMSupported(Crn.fromString(stack.getResourceCrn()).getAccountId()) &&
                    CMRepositoryVersionUtil.isCmBulkHostsRemovalAllowed(runtimeVersion)) {
                result = bulkHostsRemoval(request, forced, stack, clusterDecomissionService, hostsToRemove);
            } else {
                result = singleHostsRemoval(request, hostNames, forced, stack, clusterDecomissionService, hostsToRemove);
            }
        } catch (DecommissionException e) {
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, DECOMMISSION_ERROR_PHASE);
        } catch (Exception e) {
            LOGGER.info("Exception occurred during decommissioning.", e);
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, UNKNOWN_ERROR_PHASE);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private DecommissionResult bulkHostsRemoval(DecommissionRequest request, boolean forced,
            Stack stack, ClusterDecomissionService clusterDecomissionService, Map<String, InstanceMetaData> hostsToRemove)
            throws CloudbreakOrchestratorFailedException, CloudbreakException {
        if (!forced) {
            executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
        }
        clusterDecomissionService.deleteHostsFromCluster(Lists.newArrayList(hostsToRemove.values()));
        stopClusterManagerAgent(stack, hostsToRemove.keySet(), forced);
        cleanUpFreeIpa(stack, hostsToRemove);
        clusterDecomissionService.deleteUnusedCredentialsFromCluster();
        updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.DECOMMISSIONED, "instance successfully downscaled");
        clusterDecomissionService.restartStaleServices(forced);
        return new DecommissionResult(request, hostsToRemove.keySet());
    }

    private DecommissionResult singleHostsRemoval(DecommissionRequest request, Set<String> hostNames, boolean forced,
            Stack stack, ClusterDecomissionService clusterDecomissionService, Map<String, InstanceMetaData> hostsToRemove)
            throws CloudbreakOrchestratorFailedException, CloudbreakException {
        Set<String> decommissionedHostNames = decommissionHosts(request, hostNames, stack, clusterDecomissionService, hostsToRemove);
        stopClusterManagerAgent(stack, decommissionedHostNames, forced);
        cleanUpFreeIpa(stack, hostsToRemove);
        List<InstanceMetaData> decommissionedInstances = decommissionedHostNames.stream()
                .map(hostsToRemove::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        decommissionedInstances.forEach(clusterDecomissionService::deleteHostFromCluster);
        clusterDecomissionService.deleteUnusedCredentialsFromCluster();
        updateInstanceStatuses(decommissionedInstances, InstanceStatus.DECOMMISSIONED, "instance successfully downscaled");
        clusterDecomissionService.restartStaleServices(forced);
        return new DecommissionResult(request, decommissionedHostNames);
    }

    private Map<String, InstanceMetaData> getHostsForRemoval(String hostGroupName, Set<String> hostNames, Stack stack,
            ClusterDecomissionService clusterDecomissionService) {
        Cluster cluster = stack.getCluster();
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        return clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
    }

    private Set<String> decommissionHosts(DecommissionRequest request, Set<String> hostNames, Stack stack,
            ClusterDecomissionService clusterDecomissionService, Map<String, InstanceMetaData> hostsToRemove) {
        Set<String> decommissionedHostNames;
        if (skipClusterDecomission(request, hostsToRemove)) {
            decommissionedHostNames = hostNames;
        } else {
            executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
            decommissionedHostNames = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
        }
        return decommissionedHostNames;
    }

    private void stopClusterManagerAgent(Stack stack, Set<String> decommissionedHostNames, boolean forced) throws CloudbreakOrchestratorFailedException {
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        Set<Node> decommissionedNodes = stackUtil.collectNodesFromHostnames(stack, decommissionedHostNames);
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        hostOrchestrator.stopClusterManagerAgent(gatewayConfig, stackUtil.collectNodes(stack), decommissionedNodes, clusterDeletionBasedModel(stack.getId(),
                stack.getCluster().getId()), kerberosDetailService.isAdJoinable(kerberosConfig), kerberosDetailService.isIpaJoinable(kerberosConfig), forced);
    }

    private void updateInstanceStatuses(Collection<InstanceMetaData> instanceMetadatas, InstanceStatus instanceStatus, String statusReason) {
        for (InstanceMetaData instanceMetaData : instanceMetadatas) {
            instanceMetaDataService.updateInstanceStatus(instanceMetaData, instanceStatus, statusReason);
        }
    }

    private void cleanUpFreeIpa(Stack stack, Map<String, InstanceMetaData> hostsToRemove) {
        try {
            freeIpaCleanupService.cleanupOnScale(stack, hostsToRemove.keySet(), Set.of());
        } catch (FreeIpaOperationFailedException | CloudbreakServiceException e) {
            LOGGER.warn("FreeIPA cleanup has failed during decommission, ignoring error", e);
        }
    }

    private boolean skipClusterDecomission(DecommissionRequest request, Map<String, InstanceMetaData> hostsToRemove) {
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
            Optional<HostGroup> hostGroup = Optional.ofNullable(hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName));
            if (hostGroup.isPresent()) {
                recipeEngine.executePreTerminationRecipes(stack, hostGroup.get().getRecipes(), hostNames);
            }
        } catch (Exception ex) {
            LOGGER.warn(ex.getLocalizedMessage(), ex);
        }
    }

}
