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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.service.DecommissionException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
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
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));

            Map<String, InstanceMetaData> hostsToRemove = clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
            updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.DELETE_REQUESTED, "delete requested for instance");
            Set<String> decommissionedHostNames;
            if (skipClusterDecomission(request, hostsToRemove)) {
                decommissionedHostNames = hostNames;
            } else {
                executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
                decommissionedHostNames = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
            }
            KerberosConfig kerberosConfig = kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName()).orElse(null);
            Set<Node> decommissionedNodes = stackUtil.collectNodesFromHostnames(stack, decommissionedHostNames);
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            boolean forced = request.getDetails() != null && request.getDetails().isForced();
            hostOrchestrator.stopClusterManagerAgent(gatewayConfig, stackUtil.collectNodes(stack), decommissionedNodes, clusterDeletionBasedModel(stack.getId(),
                    cluster.getId()), kerberosDetailService.isAdJoinable(kerberosConfig), kerberosDetailService.isIpaJoinable(kerberosConfig), forced);
            cleanUpFreeIpa(stack, hostsToRemove);
            List<InstanceMetaData> decommissionedInstances = decommissionedHostNames.stream()
                    .map(hostsToRemove::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            decommissionedInstances.forEach(clusterDecomissionService::deleteHostFromCluster);
            clusterDecomissionService.deleteUnusedCredentialsFromCluster();
            updateInstanceStatuses(decommissionedInstances, InstanceStatus.DECOMMISSIONED, "instance successfully downscaled");
            clusterDecomissionService.restartStaleServices();

            result = new DecommissionResult(request, decommissionedHostNames);
        } catch (DecommissionException e) {
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, DECOMMISSION_ERROR_PHASE);
        } catch (Exception e) {
            LOGGER.info("Exception occurred during decommissioning.", e);
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, UNKNOWN_ERROR_PHASE);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private void updateInstanceStatuses(Collection<InstanceMetaData> instanceMetadatas, InstanceStatus instanceStatus, String statusReason) {
        for (InstanceMetaData instanceMetaData : instanceMetadatas) {
            instanceMetaDataService.updateInstanceStatus(instanceMetaData, instanceStatus, statusReason);
        }
    }

    private void cleanUpFreeIpa(Stack stack, Map<String, InstanceMetaData> hostsToRemove) {
        try {
            Set<String> ips = hostsToRemove.values().stream().map(InstanceMetaData::getPrivateIp).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            freeIpaCleanupService.cleanupOnScale(stack, hostsToRemove.keySet(), ips);
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
