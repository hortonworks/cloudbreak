package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class ClusterManagerRestartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerRestartService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    public void restartClouderaManager(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        restartClouderaManager(stackDto);
    }

    public void restartClouderaManager(StackDto stackDto) {
        try {
            LOGGER.debug("Restarting CM server.");
            StackView stackView = stackDto.getStack();
            InstanceMetadataView gatewayInstance = stackDto.getPrimaryGatewayInstance();
            GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stackView, stackDto.getSecurityConfig(), gatewayInstance, stackDto.hasGateway());
            Set<String> gatewayFQDN = Collections.singleton(gatewayInstance.getDiscoveryFQDN());
            ExitCriteriaModel exitModel = ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackView.getId(), stackDto.getCluster().getId());
            hostOrchestrator.restartClusterManagerOnMaster(gatewayConfig, gatewayFQDN, exitModel);
            clusterManagerDefaultConfigAdjuster.waitForClusterManagerToBecomeAvailable(stackDto, false);
            LOGGER.debug("CM server has been restarted.");
        } catch (Exception e) {
            LOGGER.error("Failed to restart CM server", e);
            throw new CloudbreakServiceException(String.format("Failed to restart Cloudera Manager on host for %s.", stackDto.getName()));
        }
    }
}
