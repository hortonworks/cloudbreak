package com.sequenceiq.freeipa.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;

@Service
public class BootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapService.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private ImageService imageService;

    public void bootstrap(Long stackId) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findAllInStack(stackId);
        Stack stack = stackRepository.findById(stackId).get();
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);

        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> {
                    String generatedHostName = hostDiscoveryService.generateHostname(freeIpa.getHostname(), null, im.getPrivateId(), false);
                    return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), im.getInstanceId(), im.getInstanceGroup().getTemplate().getInstanceType(),
                            generatedHostName, freeIpa.getDomain(), im.getInstanceGroup().getGroupName());
                }).collect(Collectors.toSet());
        BootstrapParams params = new BootstrapParams();
        params.setCloud(stack.getCloudPlatform());

        ImageEntity image = imageService.getByStack(stack);
        params.setOs(image.getOs());
        try {
            byte[] stateConfigZip = getStateConfigZip();
            hostOrchestrator.bootstrapNewNodes(gatewayConfigs, allNodes, allNodes,
                    stateConfigZip, params, new StackBasedExitCriteriaModel(stackId));
        } catch (IOException e) {
            LOGGER.error("Couldnt read state config", e);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Bootstrap failed", e);
        }
    }

    private byte[] getStateConfigZip() throws IOException {
        return CompressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt");
    }
}
