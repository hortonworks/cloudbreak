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

import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    public void setupHostMetadata(Long stackId) throws CloudbreakException {
        LOGGER.debug("Setting up host metadata for the cluster.");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (!orchestratorTypeResolver.resolveType(stack.getOrchestrator()).containerOrchestrator()) {
            Set<InstanceMetaData> allInstanceMetaData = stack.getNotDeletedInstanceMetaDataSet();
            updateWithHostData(stack, stack.getNotDeletedInstanceMetaDataSet());
            instanceMetaDataService.saveAll(allInstanceMetaData);
        }
    }

    public void setupNewHostMetadata(Long stackId, Collection<String> newAddresses) throws CloudbreakException {
        LOGGER.debug("Extending host metadata.");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (!orchestratorTypeResolver.resolveType(stack.getOrchestrator()).containerOrchestrator()) {
            Set<InstanceMetaData> newInstanceMetadata = stack.getNotDeletedInstanceMetaDataSet().stream()
                    .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
                    .collect(Collectors.toSet());
            updateWithHostData(stack, newInstanceMetadata);
            instanceMetaDataService.saveAll(newInstanceMetadata);
        }
    }

    private void updateWithHostData(Stack stack, Collection<InstanceMetaData> metadataToUpdate) throws CloudbreakSecuritySetupException {
        try {
            List<String> privateIps = metadataToUpdate.stream().map(InstanceMetaData::getPrivateIp).collect(Collectors.toList());
            GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
            Map<String, String> members = hostOrchestrator.getMembers(gatewayConfig, privateIps);
            LOGGER.debug("Received host names from hosts: {}, original targets: {}", members.values(), privateIps);
            for (InstanceMetaData instanceMetaData : metadataToUpdate) {
                instanceMetaData.setConsulServer(false);
                String address = members.get(instanceMetaData.getPrivateIp());
                instanceMetaData.setDiscoveryFQDN(address);
                LOGGER.debug("Domain used for instance: {} original: {}, fqdn: {}", instanceMetaData.getInstanceId(), address,
                        instanceMetaData.getDiscoveryFQDN());
            }
        } catch (Exception e) {
            throw new CloudbreakSecuritySetupException(e);
        }
    }

}