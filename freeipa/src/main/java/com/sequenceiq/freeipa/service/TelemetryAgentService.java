package com.sequenceiq.freeipa.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.StackRepository;

@Service
public class TelemetryAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryAgentService.class);

    private final HostOrchestrator hostOrchestrator;

    private final GatewayConfigService gatewayConfigService;

    private final StackRepository stackRepository;

    private final InstanceMetaDataRepository instanceMetaDataRepository;

    public TelemetryAgentService(HostOrchestrator hostOrchestrator, GatewayConfigService gatewayConfigService,
            StackRepository stackRepository, InstanceMetaDataRepository instanceMetaDataRepository) {
        this.hostOrchestrator = hostOrchestrator;
        this.gatewayConfigService = gatewayConfigService;
        this.stackRepository = stackRepository;
        this.instanceMetaDataRepository = instanceMetaDataRepository;
    }

    public void stopTelemetryAgent(Long stackId) {
        try {
            Stack stack = stackRepository.findById(stackId).get();
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataRepository.findAllInStack(stackId);
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet);
            Set<Node> allNodes = instanceMetaDataSet.stream()
                    .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                            im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                    .collect(Collectors.toSet());
            hostOrchestrator.stopTelemetryAgent(gatewayConfigs, allNodes,  StackBasedExitCriteriaModel.nonCancellableModel());
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Non-critical error during stopping telemetry agent", e);
        } catch (Exception e) {
            LOGGER.error("Error during stopping telemetry agent", e);
        }
    }

}
