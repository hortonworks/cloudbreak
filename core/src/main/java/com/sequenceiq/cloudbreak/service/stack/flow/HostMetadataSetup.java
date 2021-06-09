package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
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

    private static final int SINGLE_INSTANCE = 1;

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
                selectPrimaryGwBasedOnPreviousOne(stack, newInstanceMetadata);
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
                    if (StringUtils.isEmpty(discoveryFQDN) || !discoveryFQDN.equals(fqdnFromTheCluster)) {
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

    /**
     * Metadata collection happens in 2 steps. In the first step we collect most of the info
     * about an instance, IPs, IDs, but not the FQDN. This is because historically we could use
     * multiple methods to generate the FQDN - auto generated by the cloud provider or user provided.
     * In the second step we collect and update the FQDN of an instance. In todays CDP world this
     * does not make any sense to have 2 steps as we always generate the FQDN so it could be done
     * in 1 step. However, given the current 2 steps whenever we repair a primary gateway we need
     * to select another primary gateway. This happens in the first step when we don't have the info
     * about the generated FQDN so it can happen that we mark an instance as primary gateway that
     * is differerent from the previous one. By different I mean the FQDN is not the same as the
     * previous one. This was supposed to be deterministic that the same hostname will have the same
     * roles so in this second step we can correct this assignment. If there is no primary gateway
     * amongst the repaired instances we're not making any changes. If there is only a single primary
     * gateway that we're repairing then again we're not making any changes. This method only changes
     * the primary gateway selection if there are multiple gateways repaired at the same time including
     * a primary one and the previous primary gateway's hostname does not match the one selected in the
     * first step.
     *
     * @param stack               stack to update
     * @param newInstanceMetadata new instances to update
     */
    private void selectPrimaryGwBasedOnPreviousOne(Stack stack, Collection<InstanceMetaData> newInstanceMetadata) {
        Optional<InstanceMetaData> currentPrimaryGwOpt = newInstanceMetadata.stream()
                .filter(md -> InstanceMetadataType.GATEWAY_PRIMARY.equals(md.getInstanceMetadataType()))
                .filter(md -> InstanceStatus.CREATED.equals(md.getInstanceStatus())).findFirst();
        if (currentPrimaryGwOpt.isEmpty()) {
            LOGGER.debug("The primary gateway is not changing, nothing to do");
        } else if (isSingleMetadataList(newInstanceMetadata)) {
            LOGGER.debug("Primary gateway update is not required as the metadata list contains only a single instance");
        } else {
            InstanceMetaData currentPrimaryGw = currentPrimaryGwOpt.get();
            LOGGER.debug("Primary gateway repair is in progress, select an instance with matching FQDN, candidate: {}", currentPrimaryGw);
            trySelectingAMatchingInstanceForNewPrimaryGw(stack, newInstanceMetadata, currentPrimaryGw);
        }
    }

    private void trySelectingAMatchingInstanceForNewPrimaryGw(Stack stack, Collection<InstanceMetaData> newInstanceMetadata,
            InstanceMetaData currentPrimaryGw) {
        Optional<InstanceMetaData> lastTerminatedPrimaryGwOpt = instanceMetaDataService.getLastTerminatedPrimaryGatewayInstanceMetadata(stack.getId());
        if (lastTerminatedPrimaryGwOpt.isEmpty()) {
            LOGGER.debug("There is no former primary gateway, nothing to do");
        } else {
            InstanceMetaData lastTerminatedPrimaryGw = lastTerminatedPrimaryGwOpt.get();
            LOGGER.debug("The last terminated primary gateway is: {}", lastTerminatedPrimaryGw);
            if (!lastTerminatedPrimaryGw.getDiscoveryFQDN().equalsIgnoreCase(currentPrimaryGw.getDiscoveryFQDN())) {
                LOGGER.debug("The current primary gateway's hostname is not the same as the previously terminated one"
                        + " current: {}, last: {}", currentPrimaryGw.getDiscoveryFQDN(), lastTerminatedPrimaryGw.getDiscoveryFQDN());
                findAndSetNewPrimaryGw(newInstanceMetadata, currentPrimaryGw, lastTerminatedPrimaryGw);
            }
        }
    }

    private void findAndSetNewPrimaryGw(Collection<InstanceMetaData> newInstanceMetadata, InstanceMetaData currentPrimaryGw, InstanceMetaData lastPrimaryGw) {
        Optional<InstanceMetaData> instanceWithMatchingFQDNWithLastPrimaryGwOpt = newInstanceMetadata.stream()
                .filter(md -> lastPrimaryGw.getDiscoveryFQDN().equalsIgnoreCase(md.getDiscoveryFQDN()))
                .findFirst();
        if (instanceWithMatchingFQDNWithLastPrimaryGwOpt.isPresent()) {
            InstanceMetaData newPrimaryGw = instanceWithMatchingFQDNWithLastPrimaryGwOpt.get();
            LOGGER.debug("There is a matching metadata with the former primary gateway, updating it to be the new primary gateway");
            newPrimaryGw.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
            newPrimaryGw.setServer(Boolean.TRUE);
            LOGGER.debug("The new primary gateway will be: {}", newPrimaryGw);
            currentPrimaryGw.setInstanceMetadataType(InstanceMetadataType.GATEWAY);
            currentPrimaryGw.setServer(Boolean.FALSE);
            LOGGER.debug("The current primary gateway is changed to be a simple gateway: {}", currentPrimaryGw);
        } else {
            LOGGER.debug("There is no instancec with matching FQDN with the previous primary gateway: {}", lastPrimaryGw.getDiscoveryFQDN());
        }
    }

    private boolean isSingleMetadataList(Collection<InstanceMetaData> metadataToUpdate) {
        return metadataToUpdate.size() == SINGLE_INSTANCE;
    }

}