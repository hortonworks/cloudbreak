package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_DECOMMISSION_FAILED_FORCE_DELETE_CONTINUE;
import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.UNKNOWN_ERROR_PHASE;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.CmAgentStopFlags;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
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
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class DecommissionHandler implements EventHandler<DecommissionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecommissionHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackDtoService stackDtoService;

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

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

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
            StackDto stackDto = stackDtoService.getById(request.getResourceId());
            StackView stack = stackDto.getStack();
            ClusterView cluster = stackDto.getCluster();
            hostNames = getHostNamesForPrivateIds(request, stackDto);
            ClusterDecomissionService clusterDecomissionService = getClusterDecomissionService(stackDto);
            Map<String, InstanceMetadataView> hostsToRemove = new HashMap<>();
            Set<String> hostGroupNames = request.getHostGroupNames();
            for (String hostGroupName : hostGroupNames) {
                hostsToRemove.putAll(getRemovableHosts(clusterDecomissionService, hostGroupName, hostNames));
            }

            updateInstancesToDeleteRequested(hostsToRemove.values());
            if (!hostsToRemove.isEmpty()) {
                executePreTerminationRecipes(stackDto, hostsToRemove.keySet());
            }
            clusterHostServiceRunner.redeployGatewayPillarOnly(stackDto, hostNames);

            Optional<String> runtimeVersion = runtimeVersionService.getRuntimeVersion(cluster.getId());
            if (entitlementService.bulkHostsRemovalFromCMSupported(Crn.fromString(stack.getResourceCrn()).getAccountId()) &&
                    CMRepositoryVersionUtil.isCmBulkHostsRemovalAllowed(runtimeVersion)) {
                result =
                        bulkHostsRemoval(request, hostNames, forced, stackDto, clusterDecomissionService, hostsToRemove);
            } else {
                result = singleHostsRemoval(request, hostNames, forced, stackDto, clusterDecomissionService, hostsToRemove);
            }
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

    private DecommissionResult bulkHostsRemoval(DecommissionRequest request, Set<String> hostNames, boolean forced, StackDto stackDto,
            ClusterDecomissionService clusterDecomissionService, Map<String, InstanceMetadataView> hostsToRemove)
            throws CloudbreakOrchestratorFailedException, CloudbreakException {
        try {
            // running it first time in order to decommission hosts before stopping agents
            updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.UNDER_DECOMMISSION, "decomissioning instance in cluster manager");
            clusterDecomissionService.removeHostsFromCluster(Lists.newArrayList(hostsToRemove.values()));
            stopClusterManagerAgent(stackDto, hostsToRemove.keySet(), forced);
            // running it second time after agent stop to delete hosts from CM before they are appearing again in CM
            updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.REMOVING_FROM_CLUSTER_MANAGER, "removing instance from cluster manager");
            clusterDecomissionService.removeHostsFromCluster(Lists.newArrayList(hostsToRemove.values()));
            cleanUpFreeIpa(stackDto.getStack(), hostsToRemove);
            cleanUpAfterRemoval(forced, clusterDecomissionService, hostsToRemove.values());
        } catch (ClusterClientInitException e) {
            LOGGER.warn("Bulk host removal was unsuccessful, fallback to single host removal.");
            return singleHostsRemoval(request, hostNames, forced, stackDto, clusterDecomissionService, hostsToRemove);
        }
        return new DecommissionResult(request, hostNames);
    }

    private DecommissionResult singleHostsRemoval(DecommissionRequest request, Set<String> hostNames, boolean forced, StackDto stackDto,
            ClusterDecomissionService clusterDecomissionService, Map<String, InstanceMetadataView> hostsToRemove)
            throws CloudbreakOrchestratorFailedException, CloudbreakException {
        Set<String> decommissionedHostNames;
        if (hostsToRemove.isEmpty() || forced) {
            decommissionedHostNames = hostNames;
        } else {
            updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.UNDER_DECOMMISSION, "decomissioning instance in cluster manager");
            decommissionedHostNames = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
        }
        if (!decommissionedHostNames.isEmpty()) {
            stopClusterManagerAgent(stackDto, decommissionedHostNames, forced);
            cleanUpFreeIpa(stackDto.getStack(), hostsToRemove);
            updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.REMOVING_FROM_CLUSTER_MANAGER, "removing instance from cluster manager");
            List<InstanceMetadataView> deletedHosts = deleteHosts(decommissionedHostNames, clusterDecomissionService, hostsToRemove);
            cleanUpAfterRemoval(forced, clusterDecomissionService, deletedHosts);
            LOGGER.info("Removed hostnames from CM and IPA : {}", decommissionedHostNames);
        }
        return new DecommissionResult(request, hostNames);
    }

    private void cleanUpAfterRemoval(boolean forced, ClusterDecomissionService clusterDecomissionService, Collection<InstanceMetadataView> deletedHosts)
            throws CloudbreakException {
        clusterDecomissionService.deleteUnusedCredentialsFromCluster();
        updateInstancesToDecommissioned(deletedHosts);
        clusterDecomissionService.restartStaleServices(forced);
    }

    private Set<String> getHostNamesForPrivateIds(DecommissionRequest request, StackDto stackDto) {
        return request.getPrivateIds().stream().map(privateId -> {
            Optional<InstanceMetadataView> instanceMetadata = stackDto.getInstanceMetadata(privateId);
            return instanceMetadata.map(InstanceMetadataView::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private ClusterDecomissionService getClusterDecomissionService(StackDto stackDto) {
        return clusterApiConnectors.getConnector(stackDto).clusterDecomissionService();
    }

    private Map<String, InstanceMetadataView> getRemovableHosts(ClusterDecomissionService decomissionService, String hostGroupName,
            Set<String> hostNames) {
        return decomissionService.collectHostsToRemove(hostGroupName, hostNames);
    }

    private void updateInstancesToDecommissioned(Collection<InstanceMetadataView> instances) {
        updateInstanceStatuses(instances, InstanceStatus.DECOMMISSIONED, "instance successfully downscaled");
    }

    private void updateInstancesToDeleteRequested(Collection<InstanceMetadataView> instances) {
        updateInstanceStatuses(instances, InstanceStatus.DELETE_REQUESTED, "delete requested for instance");
    }

    private void updateInstanceStatuses(Collection<InstanceMetadataView> instances, InstanceStatus instanceStatus, String statusReason) {
        List<Long> instanceIds = instances.stream().map(InstanceMetadataView::getId).collect(Collectors.toList());
        instanceMetaDataService.updateAllInstancesToStatus(instanceIds, instanceStatus, statusReason);
    }

    private void executePreTerminationRecipes(StackDto stackDto, Set<String> hostNames) {
        try {
            Set<HostGroup> byClusterWithRecipes = hostGroupService.getByClusterWithRecipes(stackDto.getCluster().getId());
            recipeEngine.executePreTerminationRecipes(stackDto, byClusterWithRecipes, hostNames);
        } catch (Exception ex) {
            LOGGER.warn(ex.getLocalizedMessage(), ex);
        }
    }

    private void stopClusterManagerAgent(StackDto stackDto, Set<String> decommissionedHostNames, boolean forced) throws CloudbreakOrchestratorFailedException {
        if (CollectionUtils.isNotEmpty(decommissionedHostNames)) {
            LOGGER.info("Stopping CM agent on hosts {}", decommissionedHostNames);
            StackView stack = stackDto.getStack();
            KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
            Set<Node> decommissionedNodes = stackUtil.collectNodes(stackDto, decommissionedHostNames);
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
            Set<Node> allNodes = stackUtil.collectNodes(stackDto);
            hostOrchestrator.stopClusterManagerAgent(
                    stackDto,
                    gatewayConfig,
                    allNodes,
                    decommissionedNodes,
                    clusterDeletionBasedModel(stack.getId(), stackDto.getCluster().getId()),
                    new CmAgentStopFlags(kerberosDetailService.isAdJoinable(kerberosConfig),
                            kerberosDetailService.isIpaJoinable(kerberosConfig),
                            forced));
        } else {
            LOGGER.info("Not found hosts to stop CM agent on.");
        }
    }

    private void cleanUpFreeIpa(StackView stack, Map<String, InstanceMetadataView> hostsToRemove) {
        try {
            freeIpaCleanupService.cleanupOnScale(stack, hostsToRemove.keySet(), Set.of());
        } catch (FreeIpaOperationFailedException | CloudbreakServiceException e) {
            LOGGER.warn("FreeIPA cleanup has failed during decommission, ignoring error", e);
        }
    }

    private List<InstanceMetadataView> deleteHosts(Set<String> decommissionedHostNames, ClusterDecomissionService clusterDecomissionService,
            Map<String, InstanceMetadataView> hostsToRemove) {
        List<InstanceMetadataView> decommissionedInstances = decommissionedHostNames.stream()
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
