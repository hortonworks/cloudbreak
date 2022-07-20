package com.sequenceiq.cloudbreak.service.cluster.flow.telemetry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Component
public class TelemetryAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAgentService.class);

    private final TelemetryOrchestrator telemetryOrchestrator;

    private final GatewayConfigService gatewayConfigService;

    public TelemetryAgentService(TelemetryOrchestrator telemetryOrchestrator, GatewayConfigService gatewayConfigService) {
        this.telemetryOrchestrator = telemetryOrchestrator;
        this.gatewayConfigService = gatewayConfigService;
    }

    public void stopTelemetryAgent(StackDto stackDto) {
        try {
            List<InstanceGroupDto> instanceGroupDtos = stackDto.getInstanceGroupDtos();
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
            Set<Node> allNodes = new HashSet<>();
            instanceGroupDtos.forEach(instanceGroupDto -> {
                InstanceGroupView ig = instanceGroupDto.getInstanceGroup();
                instanceGroupDto.getNotDeletedInstanceMetaData().forEach(im -> {
                    allNodes.add(new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                            ig.getTemplate().getInstanceType(), im.getDiscoveryFQDN(), ig.getGroupName()));
                });
            });
            telemetryOrchestrator.stopTelemetryAgent(gatewayConfigs, allNodes, ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Non-critical error during stopping telemetry agent", e);
        } catch (Exception e) {
            LOGGER.error("Error during stopping telemetry agent", e);
        }
    }
}
