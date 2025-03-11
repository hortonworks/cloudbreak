package com.sequenceiq.freeipa.service.telemetry;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class TelemetryAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAgentService.class);

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void stopTelemetryAgent(Long stackId) {
        stopTelemetryAgent(stackId, null);
    }

    public void stopTelemetryAgent(Long stackId, List<String> instanceIds) {
        try {
            Stack stack = stackService.getStackById(stackId);
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet);
            Set<Node> targetNodes = instanceMetaDataSet.stream()
                    .filter(instanceMetaData -> Objects.isNull(instanceIds) || instanceIds.contains(instanceMetaData.getInstanceId()))
                    .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                            im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                    .collect(Collectors.toSet());
            if (!targetNodes.isEmpty()) {
                telemetryOrchestrator.stopTelemetryAgent(gatewayConfigs, targetNodes,  new StackBasedExitCriteriaModel(stackId));
            }
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Non-critical error during stopping telemetry agent", e);
        } catch (Exception e) {
            LOGGER.error("Error during stopping telemetry agent", e);
        }
    }

}
