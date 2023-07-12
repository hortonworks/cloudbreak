package com.sequenceiq.freeipa.service.orchestrator;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@Service
public class FreeIpaSaltPingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSaltPingService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void saltPing(Stack stack) throws SaltPingFailedException {
        InstanceMetaData pgwInstanceMetadata = stack.getPrimaryGatewayAndThrowExceptionIfEmpty();
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
        Set<String> hostNames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, pgwInstanceMetadata);
        try {
            hostOrchestrator.ping(hostNames, gatewayConfig);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.error("Salt ping failed", e);
            throw new SaltPingFailedException("Salt ping failed", e);
        }
    }
}
