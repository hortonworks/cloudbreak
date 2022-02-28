package com.sequenceiq.freeipa.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
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
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class BootstrapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapService.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private HostDiscoveryService hostDiscoveryService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private ImageService imageService;

    @Inject
    private CompressUtil compressUtil;

    public void bootstrap(Long stackId) throws CloudbreakOrchestratorException {
        bootstrap(stackId, null);
    }

    public void bootstrap(Long stackId, List<String> instanceIds) throws CloudbreakOrchestratorException {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataService.findNotTerminatedForStack(stackId).stream()
                .filter(instanceMetaData -> Objects.isNull(instanceIds) || instanceIds.contains(instanceMetaData.getInstanceId()))
                .collect(Collectors.toSet());
        Stack stack = stackRepository.findById(stackId).get();
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);

        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> {
                    String hostname = getHostname(freeIpa, im);
                    return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), im.getInstanceId(), im.getInstanceGroup().getTemplate().getInstanceType(),
                            hostname, freeIpa.getDomain(), im.getInstanceGroup().getGroupName());
                }).collect(Collectors.toSet());
        BootstrapParams params = new BootstrapParams();
        params.setCloud(stack.getCloudPlatform());

        ImageEntity image = imageService.getByStack(stack);
        params.setOs(image.getOs());
        params.setSaltBootstrapFpSupported(true);
        params.setRestartNeededFlagSupported(true);
        try {
            byte[] stateConfigZip = getStateConfigZip();
            hostOrchestrator.bootstrapNewNodes(gatewayConfigs, allNodes, allNodes,
                    stateConfigZip, params, new StackBasedExitCriteriaModel(stackId));
        } catch (IOException e) {
            LOGGER.error("Couldn't read state config", e);
            throw new CloudbreakOrchestratorFailedException("Couldn't read state config", e);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Bootstrap failed", e);
            throw e;
        }
    }

    private String getHostname(FreeIpa freeIpa, InstanceMetaData im) {
        if (StringUtils.isNotBlank(im.getDiscoveryFQDN()) && StringUtils.endsWith(im.getDiscoveryFQDN(), freeIpa.getDomain())) {
            LOGGER.info("Using already set hostname [{}] from InstanceMetaData for [{}]", im.getDiscoveryFQDN(), im.getInstanceId());
            return StringUtils.removeEnd(StringUtils.removeEnd(im.getDiscoveryFQDN(), freeIpa.getDomain()), ".");
        } else {
            String generateHostname = hostDiscoveryService.generateHostname(freeIpa.getHostname(), null, im.getPrivateId(), false);
            LOGGER.info("Hostname is not set in InstanceMetaData for [{}], generated hostname: [{}]", im.getDiscoveryFQDN(), generateHostname);
            return generateHostname;
        }
    }

    private byte[] getStateConfigZip() throws IOException {
        return compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt");
    }
}
