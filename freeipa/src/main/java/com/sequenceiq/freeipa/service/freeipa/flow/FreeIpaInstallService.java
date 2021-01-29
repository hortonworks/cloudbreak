package com.sequenceiq.freeipa.service.freeipa.flow;

import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.proxy.ProxyConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.tag.TagConfigService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;

@Service
public class FreeIpaInstallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaInstallService.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaConfigService freeIpaConfigService;

    @Inject
    private ProxyConfigService proxyConfigService;

    @Inject
    private TagConfigService tagConfigService;

    @Inject
    private TelemetryConfigService telemetryConfigService;

    public void installFreeIpa(Long stackId) throws CloudbreakOrchestratorException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);
        Set<Node> allNodes = mapInstancesToNodes(instanceMetaDatas);

        createAndPushPillarsToNodes(stackId, stack, gatewayConfigs, allNodes);
        installFreeIpa(stackId, stack, gatewayConfigs, allNodes);
    }

    private void installFreeIpa(Long stackId, Stack stack, List<GatewayConfig> gatewayConfigs, Set<Node> allNodes) throws CloudbreakOrchestratorException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        hostOrchestrator.installFreeIpa(primaryGatewayConfig, gatewayConfigs, allNodes, new StackBasedExitCriteriaModel(stackId));
    }

    private void createAndPushPillarsToNodes(Long stackId, Stack stack, List<GatewayConfig> gatewayConfigs, Set<Node> allNodes)
            throws CloudbreakOrchestratorFailedException {
        SaltConfig saltConfig = new SaltConfig();
        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(stack, allNodes);
        servicePillarConfig.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", Collections.singletonMap("freeipa", freeIpaConfigView.toMap())));
        servicePillarConfig.put("discovery", new SaltPillarProperties("/discovery/init.sls", singletonMap("platform", stack.getCloudPlatform())));
        servicePillarConfig.putAll(telemetryConfigService.createTelemetryPillarConfig(stack));
        servicePillarConfig.putAll(proxyConfigService.createProxyPillarConfig(stack.getEnvironmentCrn()));
        servicePillarConfig.putAll(tagConfigService.createTagsPillarConfig(stack));
        hostOrchestrator.initSaltConfig(gatewayConfigs, allNodes, saltConfig, new StackBasedExitCriteriaModel(stackId));
    }

    private Set<Node> mapInstancesToNodes(Set<InstanceMetaData> instanceMetaDatas) throws CloudbreakOrchestratorFailedException {
        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), "testGroup"))
                .collect(Collectors.toSet());
        validateNodeIsPresent(allNodes);
        return allNodes;
    }

    private void validateNodeIsPresent(Set<Node> allNodes) throws CloudbreakOrchestratorFailedException {
        if (allNodes.isEmpty()) {
            String errorMessage = "There are no nodes to install with FreeIPA.";
            LOGGER.error(errorMessage);
            throw new CloudbreakOrchestratorFailedException(errorMessage);
        }
    }
}
