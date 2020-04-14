package com.sequenceiq.freeipa.service.stack;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class HostMetadataSetup {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostMetadataSetup.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void setupHostMetadata(Long stackId) throws CloudbreakOrchestratorException {
        LOGGER.debug("Setting up host metadata for the cluster.");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> allInstanceMetaData = stack.getNotDeletedInstanceMetaDataSet();
        updateWithHostData(stack, stack.getNotDeletedInstanceMetaDataSet());
        instanceMetaDataService.saveAll(allInstanceMetaData);
    }

    private void updateWithHostData(Stack stack, Collection<InstanceMetaData> metadataToUpdate) throws CloudbreakOrchestratorException {
        List<String> privateIps = metadataToUpdate.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.toList());
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Map<String, String> members = hostOrchestrator.getMembers(gatewayConfig, privateIps);
        LOGGER.debug("Received host names from hosts: {}, original targets: {}", members.values(), privateIps);
        for (InstanceMetaData instanceMetaData : metadataToUpdate) {
            String address = members.get(instanceMetaData.getPrivateIp());
            instanceMetaData.setDiscoveryFQDN(address);
            LOGGER.debug("Domain used for instance: {} original: {}, fqdn: {}", instanceMetaData.getInstanceId(), address,
                    instanceMetaData.getDiscoveryFQDN());
        }
    }

}