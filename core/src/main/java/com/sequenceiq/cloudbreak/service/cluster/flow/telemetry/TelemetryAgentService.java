package com.sequenceiq.cloudbreak.service.cluster.flow.telemetry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Component
public class TelemetryAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAgentService.class);

    private final HostOrchestrator hostOrchestrator;

    private final GatewayConfigService gatewayConfigService;

    public TelemetryAgentService(HostOrchestrator hostOrchestrator, GatewayConfigService gatewayConfigService) {
        this.hostOrchestrator = hostOrchestrator;
        this.gatewayConfigService = gatewayConfigService;
    }

    public void stopTelemetryAgent(Stack stack) {
        try {
            Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            Set<Node> allNodes = instanceMetaDataSet.stream()
                    .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                            im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                    .collect(Collectors.toSet());
            hostOrchestrator.stopTelemetryAgent(gatewayConfigs, allNodes, ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Non-critical error during stopping telemetry agent", e);
        } catch (Exception e) {
            LOGGER.error("Error during stopping telemetry agent", e);
        }
    }
}
