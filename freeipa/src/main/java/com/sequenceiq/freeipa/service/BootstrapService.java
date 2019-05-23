package com.sequenceiq.freeipa.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.HostDiscoveryService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.DummyExitCriteriaModel;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.StackRepository;

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

    public void bootstrap(Long stackId) {
        Set<InstanceMetaData> instanceMetaDatas = instanceMetaDataRepository.findAllInStack(stackId);
        Stack stack = stackRepository.findById(stackId).get();
        FreeIpa freeIpa = freeIpaService.findByStack(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getGatewayConfigs(stack, instanceMetaDatas);

        Set<Node> allNodes = instanceMetaDatas.stream()
                .map(im -> {
                    String generatedHostName = hostDiscoveryService.generateHostname(freeIpa.getHostname(), null, im.getPrivateId(), false);
                    return new Node(im.getPrivateIp(), im.getPublicIpWrapper(), generatedHostName, freeIpa.getDomain(), "testGroup");
                }).collect(Collectors.toSet());
        BootstrapParams params = new BootstrapParams();
        params.setCloud(stack.getCloudPlatform());
        //FIXME set from image
        params.setOs("amazonlinux2");
        try {
            byte[] stateConfigZip = getStateConfigZip();
            hostOrchestrator.bootstrapNewNodes(gatewayConfigs, allNodes, allNodes,
                    stateConfigZip, params, new DummyExitCriteriaModel());
        } catch (IOException e) {
            LOGGER.error("Couldnt read state config", e);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Bootstrap failed", e);
        }
    }

    private byte[] getStateConfigZip() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (ZipOutputStream zout = new ZipOutputStream(baos)) {
                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Map<String, List<Resource>> structure = new TreeMap<>();
                for (Resource resource : resolver.getResources("classpath*:freeipa-salt/**")) {
                    String path = resource.getURL().getPath();
                    String dir = path.substring(path.indexOf("/freeipa-salt") + "/freeipa-salt".length(), path.lastIndexOf('/') + 1);
                    List<Resource> list = structure.get(dir);
                    if (list == null) {
                        list = new ArrayList<>();
                    }
                    structure.put(dir, list);
                    if (!path.endsWith("/")) {
                        list.add(resource);
                    }
                }
                for (Map.Entry<String, List<Resource>> entry : structure.entrySet()) {
                    zout.putNextEntry(new ZipEntry(entry.getKey()));
                    for (Resource resource : entry.getValue()) {
                        LOGGER.debug("Zip salt entry: {}", resource.getFilename());
                        zout.putNextEntry(new ZipEntry(entry.getKey() + resource.getFilename()));
                        InputStream inputStream = resource.getInputStream();
                        byte[] bytes = IOUtils.toByteArray(inputStream);
                        zout.write(bytes);
                        zout.closeEntry();
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed to zip salt configurations", e);
                throw new IOException("Failed to zip salt configurations", e);
            }
            return baos.toByteArray();
        }
    }
}
