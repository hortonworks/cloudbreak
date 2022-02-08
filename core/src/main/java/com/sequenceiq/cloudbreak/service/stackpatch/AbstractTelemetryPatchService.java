package com.sequenceiq.cloudbreak.service.stackpatch;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

public abstract class AbstractTelemetryPatchService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTelemetryPatchService.class);

    private static final int DIVIDER = 1000;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    protected Set<Node> getAvailableNodes(String stackName, Set<InstanceMetaData> instanceMetaDataSet, List<GatewayConfig> gatewayConfigs,
            ClusterDeletionBasedExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException, ExistingStackPatchApplyException {
        Set<Node> allNodes = getNodes(instanceMetaDataSet);
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(gatewayConfigs, allNodes, exitModel);
        Set<String> unresponsiveHostnames = unresponsiveNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        Set<Node> availableNodes = allNodes.stream()
                .filter(n -> !unresponsiveHostnames.contains(n.getHostname()))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(availableNodes)) {
            String message = "Not found any available nodes for patch, stack: " + stackName;
            LOGGER.info(message);
            throw new ExistingStackPatchApplyException(message);
        }
        return availableNodes;
    }

    protected Set<Node> getNodes(Set<InstanceMetaData> instanceMetaDataSet) {
        return instanceMetaDataSet.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .collect(Collectors.toSet());
    }

    protected byte[] getCurrentSaltStateStack(Stack stack) throws ExistingStackPatchApplyException {
        byte[] currentSaltState = clusterComponentConfigProvider.getSaltStateComponent(stack.getCluster().getId());
        if (currentSaltState == null) {
            String message = "Salt state is empty for stack " + stack.getResourceCrn();
            LOGGER.info(message);
            throw new ExistingStackPatchApplyException(message);
        }
        return currentSaltState;
    }

    protected TelemetryOrchestrator getTelemetryOrchestrator() {
        return telemetryOrchestrator;
    }

    protected long dateStringToTimestampForImage(String dateString) {
        return Date.from(LocalDate.parse(dateString).atStartOfDay().toInstant(ZoneOffset.UTC)).getTime() / DIVIDER;
    }
}
