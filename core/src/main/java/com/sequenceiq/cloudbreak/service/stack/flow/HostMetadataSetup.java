package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class HostMetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostMetadataSetup.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void setupHostMetadata(Long stackId) {
        LOGGER.debug("Setting up host metadata for the cluster.");
        Set<InstanceMetaData> allInstanceMetadataByStackId = instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId);
        updateWithHostData(stackId, allInstanceMetadataByStackId);
        instanceMetaDataService.saveAll(allInstanceMetadataByStackId);
    }

    public void setupNewHostMetadata(Long stackId, Collection<String> newAddresses) {
        LOGGER.info("Extending host metadata: {}", newAddresses);
        Set<InstanceMetaData> newInstanceMetadata = instanceMetaDataService.getNotDeletedAndNotZombieInstanceMetadataByStackId(stackId).stream()
                .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
                .collect(Collectors.toSet());
        updateWithHostData(stackId, newInstanceMetadata);
        instanceMetaDataService.saveAll(newInstanceMetadata);
    }

    private void updateWithHostData(Long stackId, Collection<InstanceMetaData> metadataToUpdate) {
        try {
            LOGGER.info("Update metadatas: {}", metadataToUpdate);
            List<String> privateIps = metadataToUpdate.stream()
                    .filter(instanceMetaData -> InstanceStatus.CREATED.equals(instanceMetaData.getInstanceStatus()))
                    .map(InstanceMetaData::getPrivateIp)
                    .collect(Collectors.toList());
            if (!privateIps.isEmpty()) {
                StackView stack = stackDtoService.getStackViewById(stackId);
                SecurityConfig securityConfig = stackDtoService.getSecurityConfig(stackId);
                InstanceMetadataView primaryGatewayInstanceMetadata = instanceMetaDataService.getPrimaryGatewayInstanceMetadataOrError(stackId);
                Boolean hasGateway = stackDtoService.hasGateway(stackId);
                GatewayConfig gatewayConfig = gatewayConfigService.getGatewayConfig(stack, securityConfig, primaryGatewayInstanceMetadata, hasGateway);
                Map<String, String> members = hostOrchestrator.getMembers(gatewayConfig, privateIps);
                LOGGER.info("Received host names from hosts: {}, original targets: {}", members.values(), privateIps);
                for (InstanceMetaData instanceMetaData : metadataToUpdate) {
                    String privateIp = instanceMetaData.getPrivateIp();
                    String fqdnFromTheCluster = members.get(privateIp);
                    String discoveryFQDN = instanceMetaData.getDiscoveryFQDN();
                    if (Strings.isNullOrEmpty(discoveryFQDN) || !discoveryFQDN.equals(fqdnFromTheCluster)) {
                        instanceMetaData.setDiscoveryFQDN(fqdnFromTheCluster);
                        LOGGER.info("Discovery FQDN has been updated for instance: {} original: {}, fqdn: {}", instanceMetaData.getInstanceId(),
                                fqdnFromTheCluster, discoveryFQDN);
                    } else {
                        LOGGER.debug("There is no need to update the FQDN for node, private ip: '{}' with FQDN: '{}'", privateIp, discoveryFQDN);
                    }
                }
            } else {
                LOGGER.info("There is no hosts to update");
            }
        } catch (Exception e) {
            throw new CloudbreakRuntimeException(e);
        }
    }

}