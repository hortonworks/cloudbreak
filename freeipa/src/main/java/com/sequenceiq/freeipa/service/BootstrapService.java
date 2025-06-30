package com.sequenceiq.freeipa.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
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
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

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

    @Inject
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    public void bootstrap(Long stackId) throws CloudbreakOrchestratorException {
        bootstrap(stackId, null, false);
    }

    public void bootstrap(Long stackId, boolean restartAll) throws CloudbreakOrchestratorException {
        bootstrap(stackId, null, restartAll);
    }

    public void bootstrap(Long stackId, List<String> instanceIds, boolean restartAll) throws CloudbreakOrchestratorException {
        Stack stack = stackRepository.findOneWithLists(stackId).get();
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataService.findNotTerminatedForStack(stack.getId()).stream()
                .filter(instanceMetaData -> Objects.isNull(instanceIds) || instanceIds.contains(instanceMetaData.getInstanceId()))
                .collect(Collectors.toSet());
        Set<String> hostNames = instanceMetaDatas.stream().map(InstanceMetaData::getShortHostname).collect(Collectors.toSet());
        LOGGER.info("Bootstrapping nodes {} of stack {}", hostNames, stack.getResourceCrn());
        bootstrap(stack, instanceMetaDatas, false, restartAll);
    }

    public void reBootstrap(Stack stack) throws CloudbreakOrchestratorException {
        LOGGER.info("Re-bootstrapping nodes of stack {}", stack.getResourceCrn());
        bootstrap(stack, stack.getNotDeletedInstanceMetaDataSet(), true, false);
    }

    public void reBootstrap(Stack stack, Set<InstanceMetaData> instanceMetaDatas) throws CloudbreakOrchestratorException {
        LOGGER.info("Re-bootstrapping nodes {} of stack {}", instanceMetaDatas, stack.getResourceCrn());
        bootstrap(stack, instanceMetaDatas, true, false);
    }

    private void bootstrap(Stack stack, Set<InstanceMetaData> instanceMetaDatas, boolean reBootstrap, boolean restartAll)
            throws CloudbreakOrchestratorException {
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);
        boolean fqdnAsHostnameSupported = saltBootstrapVersionChecker.isFqdnAsHostnameSupported(stack);

        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> {
                    String hostname = getHostname(freeIpa, im, fqdnAsHostnameSupported);
                    return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), im.getInstanceId(), im.getInstanceGroup().getTemplate().getInstanceType(),
                            hostname, freeIpa.getDomain(), im.getInstanceGroup().getGroupName());
                }).collect(Collectors.toSet());
        BootstrapParams params = new BootstrapParams();
        params.setCloud(stack.getCloudPlatform());

        ImageEntity image = imageService.getByStack(stack);
        params.setOs(image.getOs());
        params.setSaltBootstrapFpSupported(true);
        params.setRestartNeededFlagSupported(true);

        StackBasedExitCriteriaModel exitCriteriaModel = new StackBasedExitCriteriaModel(stack.getId());
        try {
            if (reBootstrap) {
                hostOrchestrator.reBootstrapExistingNodes(gatewayConfigs, allNodes, params, exitCriteriaModel);
            } else {
                byte[] stateConfigZip = getStateConfigZip();
                hostOrchestrator.bootstrapNewNodes(gatewayConfigs, allNodes, allNodes,
                        stateConfigZip, params, exitCriteriaModel, restartAll);
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't read state config", e);
            throw new CloudbreakOrchestratorFailedException("Couldn't read state config", e);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Bootstrap failed", e);
            throw e;
        }
    }

    private String getHostname(FreeIpa freeIpa, InstanceMetaData im, boolean fqdnAsHostnameSupported) {
        if (StringUtils.isNotBlank(im.getDiscoveryFQDN()) && StringUtils.endsWith(im.getDiscoveryFQDN(), freeIpa.getDomain())) {
            String hostname = fqdnAsHostnameSupported ? im.getDiscoveryFQDN()
                    : StringUtils.removeEnd(StringUtils.removeEnd(im.getDiscoveryFQDN(), freeIpa.getDomain()), ".");
            LOGGER.info("Using already set hostname [{}] from InstanceMetaData for [{}]. Final hostname: [{}]",
                    im.getDiscoveryFQDN(), im.getInstanceId(), hostname);
            return hostname;
        } else {
            String generatedHostname = hostDiscoveryService.generateHostname(freeIpa.getHostname(), null, im.getPrivateId(), false);
            String hostname = fqdnAsHostnameSupported ? generatedHostname + "." + freeIpa.getDomain() : generatedHostname;
            LOGGER.info("Hostname is not set in InstanceMetaData for [{}], generated hostname: [{}]. Final hostname: [{}]",
                    im.getDiscoveryFQDN(), generatedHostname, hostname);
            return hostname;
        }
    }

    private byte[] getStateConfigZip() throws IOException {
        return compressUtil.generateCompressedOutputFromFolders("salt-common", "freeipa-salt");
    }
}
