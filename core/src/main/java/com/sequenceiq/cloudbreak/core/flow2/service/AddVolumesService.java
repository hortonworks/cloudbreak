package com.sequenceiq.cloudbreak.core.flow2.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.ConfigUpdateUtilService;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class AddVolumesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddVolumesService.class);

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ConfigUpdateUtilService configUpdateUtilService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    public Map<String, Map<String, String>> redeployStatesAndMountDisks(Stack stack, String requestGroup) throws Exception {
        String blueprintText = stack.getBlueprint().getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        Set<ServiceComponent> hostTemplateServiceComponents = processor.getServiceComponentsByHostGroup().get(requestGroup);
        InMemoryStateStore.putStack(stack.getId(), PollGroup.POLLABLE);
        stopClouderaManagerServices(stack.getId(), requestGroup, hostTemplateServiceComponents);
        LOGGER.info("RE-Bootstrap machines");
        clusterBootstrapper.reBootstrapMachines(stack.getId());
        Map<String, Map<String, String>> fstabInformation = formatAndMountAfterAddingDisks(stack, requestGroup);
        StackDto stackDto = stackDtoService.getById(stack.getId());
        clusterHostServiceRunner.updateClusterConfigs(stackDto, true);
        InMemoryStateStore.deleteStack(stack.getId());
        LOGGER.info("Successfully mounted additional block storages for stack {} and group {}", stack.getId(), requestGroup);
        return fstabInformation;
    }

    private Map<String, Map<String, String>> formatAndMountAfterAddingDisks(Stack stack, String requestGroup) throws CloudbreakOrchestratorFailedException {
        Set<Node> allNodes = stackUtil.collectNodes(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup)).collect(Collectors.toSet());
        Set<Node> nodesWithDiskData = stackUtil.collectNodesWithDiskData(stack).stream().filter(node -> node.getHostGroup().equals(requestGroup))
                .collect(Collectors.toSet());
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        ExitCriteriaModel exitCriteriaModel = clusterDeletionBasedModel(stack.getId(), stack.getCluster().getId());
        LOGGER.debug("Formatting and mounting disks.");
        Map<String, Map<String, String>> fstabInformation = hostOrchestrator.formatAndMountDisksAfterModifyingVolumesOnNodes(gatewayConfigs,
                nodesWithDiskData, allNodes, exitCriteriaModel);
        return fstabInformation;
    }

    private void stopClouderaManagerServices(Long stackId, String requestGroup, Set<ServiceComponent> hostTemplateServiceComponents) {
        try {
            StackDto stack = stackDtoService.getById(stackId);
            configUpdateUtilService.stopClouderaManagerServices(stack, hostTemplateServiceComponents);
        } catch (Exception ex) {
            LOGGER.warn("Exception while trying to stop cloudera manager services for group: {}", requestGroup);
            throw new CloudbreakServiceException(ex.getMessage());
        }
    }
}
