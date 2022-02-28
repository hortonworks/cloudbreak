package com.sequenceiq.freeipa.service.telemetry;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.GatewayConfigService;

@Service
public class TelemetryAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAgentService.class);

    private final TelemetryOrchestrator telemetryOrchestrator;

    private final GatewayConfigService gatewayConfigService;

    private final StackRepository stackRepository;

    private final InstanceMetaDataRepository instanceMetaDataRepository;

    public TelemetryAgentService(TelemetryOrchestrator telemetryOrchestrator, GatewayConfigService gatewayConfigService,
            StackRepository stackRepository, InstanceMetaDataRepository instanceMetaDataRepository) {
        this.telemetryOrchestrator = telemetryOrchestrator;
        this.gatewayConfigService = gatewayConfigService;
        this.stackRepository = stackRepository;
        this.instanceMetaDataRepository = instanceMetaDataRepository;
    }

    public void stopTelemetryAgent(Long stackId) {
        stopTelemetryAgent(stackId, null);
    }

    public void stopTelemetryAgent(Long stackId, List<String> instanceIds) {
        try {
            Stack stack = stackRepository.findById(stackId).get();
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataRepository.findNotTerminatedForStack(stackId);
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
