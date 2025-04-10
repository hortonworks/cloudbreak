package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Service
public class SeLinuxEnablementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeLinuxEnablementService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void modifySeLinuxOnAllNodes(Stack stack) throws CloudbreakOrchestratorException {
        LOGGER.debug("Changing SeLinux mode on stack - {}", stack.getResourceCrn());
        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = mapInstancesToNodes(instanceMetaDataSet);
        ClusterDeletionBasedExitCriteriaModel exitCriteriaModel = new ClusterDeletionBasedExitCriteriaModel(stack.getId(), stack.getClusterId());
        LOGGER.debug("Calling hostOrchestrator for modifying SeLinux on stack - {}", stack.getResourceCrn());
        hostOrchestrator.enableSeLinuxOnNodes(allGatewayConfigs, allNodes, exitCriteriaModel);
    }

    private Set<Node> mapInstancesToNodes(Set<InstanceMetaData> instanceMetaDatas) {
        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIpWrapper(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .collect(Collectors.toSet());
        validateNodeIsPresent(allNodes);
        return allNodes;
    }

    private void validateNodeIsPresent(Set<Node> allNodes) {
        if (allNodes.isEmpty()) {
            String errorMessage = "There are no nodes while scanning instance metadata.";
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }
}
