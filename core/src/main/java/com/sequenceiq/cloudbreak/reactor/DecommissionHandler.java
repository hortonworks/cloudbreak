package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DECOMMISSION_FAILED_FORCE_DELETE_CONTINUE;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaCleanupService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeIpaOperationFailedException;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
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
    private CloudbreakEventService eventService;

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
            hostNames = getHostNamesForPrivateIds(request, stack);
            ClusterDecomissionService clusterDecomissionService = getClusterDecomissionService(stack);
            Map<String, InstanceMetaData> hostsToRemove = getRemovableHosts(clusterDecomissionService, stack, request.getHostGroupName(), hostNames);
            updateInstancesToDeleteRequested(hostsToRemove.values());
            Set<String> decommissionedHostNames;
            if (hostsToRemove.isEmpty() || forced) {
                decommissionedHostNames = hostNames;
            } else {
                executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
                decommissionedHostNames = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
            }
            stopClusterManagerAgent(stack, decommissionedHostNames, forced);
            cleanUpFreeIpa(stack, hostsToRemove);
            List<InstanceMetaData> deletedHosts = deleteHosts(decommissionedHostNames, clusterDecomissionService, hostsToRemove);
            clusterDecomissionService.deleteUnusedCredentialsFromCluster();
            updateInstancesToDecommissioned(deletedHosts);
            clusterDecomissionService.restartStaleServices(forced);
            result = new DecommissionResult(request, decommissionedHostNames);
        } catch (Exception e) {
            LOGGER.info("Exception occurred during decommission.", e);
            if (isTolerableError(e) && forced && !request.getDetails().isRepair()) {
                eventService.fireCloudbreakEvent(
                        request.getResourceId(),
                        UPDATE_IN_PROGRESS.name(),
                        CLUSTER_DECOMMISSION_FAILED_FORCE_DELETE_CONTINUE,
                        Collections.singletonList(e.getMessage()));
                result = new DecommissionResult(request, hostNames);
            } else {
                result = new DecommissionResult(e.getMessage(), e, request, hostNames, UNKNOWN_ERROR_PHASE);
            }
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private Set<String> getHostNamesForPrivateIds(DecommissionRequest request, Stack stack) {
        return request.getPrivateIds().stream().map(privateId -> {
            Optional<InstanceMetaData> instanceMetadata = stackService.getInstanceMetadata(stack.getInstanceMetaDataAsList(), privateId);
            return instanceMetadata.map(InstanceMetaData::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private ClusterDecomissionService getClusterDecomissionService(Stack stack) {
        return clusterApiConnectors.getConnector(stack).clusterDecomissionService();
    }

    private Map<String, InstanceMetaData> getRemovableHosts(ClusterDecomissionService decomissionService, Stack stack, String hostGroupName,
            Set<String> hostNames) {
        HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName)
                .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
        return decomissionService.collectHostsToRemove(hostGroup, hostNames);
    }

    private void updateInstancesToDecommissioned(List<InstanceMetaData> instances) {
        updateInstanceStatuses(instances, InstanceStatus.DECOMMISSIONED, "instance successfully downscaled");
    }

    private void updateInstancesToDeleteRequested(Collection<InstanceMetaData> instances) {
        updateInstanceStatuses(instances, InstanceStatus.DELETE_REQUESTED, "delete requested for instance");
    }

    private void updateInstanceStatuses(Collection<InstanceMetaData> instances, InstanceStatus instanceStatus, String statusReason) {
        instances.forEach(instance -> instanceMetaDataService.updateInstanceStatus(instance, instanceStatus, statusReason));
    }

    private void executePreTerminationRecipes(Stack stack, String hostGroupName, Set<String> hostNames) {
        try {
            Optional<HostGroup> hostGroup = Optional.ofNullable(hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName));
            if (hostGroup.isPresent()) {
                recipeEngine.executePreTerminationRecipes(stack, Set.of(hostGroup.get()), hostNames);
            }
        } catch (Exception ex) {
            LOGGER.warn(ex.getLocalizedMessage(), ex);
        }
    }

    private void stopClusterManagerAgent(Stack stack, Set<String> decommissionedHostNames, boolean forced) throws CloudbreakOrchestratorFailedException {
        KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
        Set<Node> decommissionedNodes = stackUtil.collectNodesFromHostnames(stack, decommissionedHostNames);
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> allNodes = stackUtil.collectNodes(stack);
        hostOrchestrator.stopClusterManagerAgent(
                gatewayConfig,
                allNodes,
                decommissionedNodes,
                clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId()),
                kerberosDetailService.isAdJoinable(kerberosConfig),
                kerberosDetailService.isIpaJoinable(kerberosConfig),
                forced);
    }

    private void cleanUpFreeIpa(Stack stack, Map<String, InstanceMetaData> hostsToRemove) {
        try {
            freeIpaCleanupService.cleanupOnScale(stack, hostsToRemove.keySet(), Set.of());
        } catch (FreeIpaOperationFailedException | CloudbreakServiceException e) {
            LOGGER.warn("FreeIPA cleanup has failed during decommission, ignoring error", e);
        }
    }

    private List<InstanceMetaData> deleteHosts(Set<String> decommissionedHostNames, ClusterDecomissionService clusterDecomissionService,
            Map<String, InstanceMetaData> hostsToRemove) {
        List<InstanceMetaData> decommissionedInstances = decommissionedHostNames.stream()
                .map(hostsToRemove::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        decommissionedInstances.forEach(clusterDecomissionService::deleteHostFromCluster);
        return decommissionedInstances;
    }

    private boolean isTolerableError(Exception exception) {
        return !NullPointerException.class.equals(exception.getClass());
    }

}
