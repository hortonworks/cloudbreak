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
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
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
    private HostOrchestrator hostOrchestrator;

    @Inject
    private TransactionService transactionService;

    public void setupHostMetadata(Long stackId) {
        try {
            transactionService.required(() -> {
                LOGGER.debug("Setting up host metadata for the cluster.");
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                Set<InstanceMetaData> allInstanceMetadataByStackId = instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stackId);
                updateWithHostData(stack, allInstanceMetadataByStackId);
                instanceMetaDataService.saveAll(allInstanceMetadataByStackId);
            });
        } catch (TransactionExecutionException e) {
            throw new CloudbreakRuntimeException(e.getCause());
        }
    }

    public void setupNewHostMetadata(Long stackId, Collection<String> newAddresses) {
        try {
            transactionService.required(() -> {
                LOGGER.info("Extending host metadata: {}", newAddresses);
                Stack stack = stackService.getByIdWithListsInTransaction(stackId);
                Set<InstanceMetaData> newInstanceMetadata = instanceMetaDataService.getNotDeletedInstanceMetadataByStackId(stackId).stream()
                        .filter(instanceMetaData -> newAddresses.contains(instanceMetaData.getPrivateIp()))
                        .collect(Collectors.toSet());
                updateWithHostData(stack, newInstanceMetadata);
                instanceMetaDataService.saveAll(newInstanceMetadata);
            });
        } catch (TransactionExecutionException e) {
            throw new CloudbreakRuntimeException(e.getCause());
        }
    }

    private void updateWithHostData(Stack stack, Collection<InstanceMetaData> metadataToUpdate) {
        try {
            List<String> privateIps = metadataToUpdate.stream()
                    .filter(instanceMetaData -> InstanceStatus.CREATED.equals(instanceMetaData.getInstanceStatus()))
                    .map(InstanceMetaData::getPrivateIp)
                    .collect(Collectors.toList());
            if (!privateIps.isEmpty()) {
                GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
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