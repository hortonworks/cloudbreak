package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType.GATEWAY_PRIMARY;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@Service
public class ChangePrimaryGatewayService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangePrimaryGatewayService.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    public Optional<String> getPrimaryGatewayInstanceId(Stack stack) {
        Optional<String> primaryGatewayInstanceId = Optional.empty();
        try {
            primaryGatewayInstanceId = Optional.of(gatewayConfigService.getPrimaryGatewayConfig(stack).getInstanceId());
        } catch (NotFoundException e) {
            LOGGER.debug("No active primary gateway found");
        }
        return primaryGatewayInstanceId;
    }

    public String selectNewPrimaryGatewayInstanceId(Stack stack, List<String> repairInstanceIds) {
        String newPrimaryGatewayInstanceId;
        try {
            GatewayConfig currentPrimaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
            Optional<String> freeIpaMasterInstanceId = findFreeIpaMasterInstanceId(stack, gatewayConfigService.getPrimaryGatewayConfigForSalt(stack));
            if (freeIpaMasterInstanceId.isPresent() && !repairInstanceIds.contains(freeIpaMasterInstanceId.get())) {
                LOGGER.debug("Using the FreeIPA master as the new primary gateway");
                newPrimaryGatewayInstanceId = freeIpaMasterInstanceId.get();
            } else {
                newPrimaryGatewayInstanceId = currentPrimaryGatewayConfig.getInstanceId();
                if (repairInstanceIds.contains(newPrimaryGatewayInstanceId)) {
                    LOGGER.debug("The current primary gateway is on the list of to avoid, searching for a different primary gateway");
                    newPrimaryGatewayInstanceId = assignNewPrimaryGatewayInstanceId(stack, repairInstanceIds);
                }
            }
        } catch (NotFoundException | CloudbreakOrchestratorException e) {
            LOGGER.debug("No primary gateway found, searching for a different primary gateway");
            newPrimaryGatewayInstanceId = assignNewPrimaryGatewayInstanceId(stack, repairInstanceIds);
        }
        LOGGER.debug("The new primary gateway will be {}", newPrimaryGatewayInstanceId);
        return newPrimaryGatewayInstanceId;
    }

    private Optional<String> findFreeIpaMasterInstanceId(Stack stack, GatewayConfig currentPrimaryGatewayConfig) throws CloudbreakOrchestratorException {
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
        return hostOrchestrator.getFreeIpaMasterHostname(currentPrimaryGatewayConfig, allNodes).stream()
                .flatMap(hostname ->
                        stack.getNotDeletedInstanceMetaDataSet().stream()
                                .filter(im -> hostname.equals(im.getDiscoveryFQDN()))
                                .map(InstanceMetaData::getInstanceId)
                )
                .findFirst();
    }

    private String assignNewPrimaryGatewayInstanceId(Stack stack, List<String> repairInstanceIds) throws NotFoundException {
        return stack.getNotDeletedInstanceMetaDataSet().stream()
                .map(InstanceMetaData::getInstanceId)
                .filter(id -> Objects.isNull(repairInstanceIds) || !repairInstanceIds.contains(id))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("No candidates were found for the new primary gateway"));
    }

    public void changePrimaryGatewayMetadata(Stack stack, Optional<String> formerPrimaryGatwayInstanceId, String newPrimaryGatewayInstanceId) {
        if (formerPrimaryGatwayInstanceId.isEmpty() || !newPrimaryGatewayInstanceId.equals(formerPrimaryGatwayInstanceId.get())) {
            LOGGER.debug("Changing the primary gateway metadata from {} to {}", formerPrimaryGatwayInstanceId, newPrimaryGatewayInstanceId);
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataRepository.findNotTerminatedForStack(stack.getId());
            InstanceMetaData newPrimaryGateway = findInstanceMetaDataByInstanceId(instanceMetaDataSet, newPrimaryGatewayInstanceId);

            formerPrimaryGatwayInstanceId.ifPresent(formerId -> {
                InstanceMetaData formerPrimaryGateway = findInstanceMetaDataByInstanceId(instanceMetaDataSet, formerId);
                formerPrimaryGateway.setInstanceMetadataType(GATEWAY);
                instanceMetaDataRepository.save(formerPrimaryGateway);
                LOGGER.debug("The primary gateway {} metadata has been changed to a regular gateway.", formerId);
            });
            newPrimaryGateway.setInstanceMetadataType(GATEWAY_PRIMARY);
            instanceMetaDataRepository.save(newPrimaryGateway);
            LOGGER.debug("The regular gateway {} metadata has been changed to a primary gateway.", newPrimaryGatewayInstanceId);
        } else {
            LOGGER.debug("The primary gateway {} metadata did not change", newPrimaryGatewayInstanceId);
        }
    }

    private InstanceMetaData findInstanceMetaDataByInstanceId(Set<InstanceMetaData> instanceMetaDataSet, String instanceId) {
        return instanceMetaDataSet.stream()
                .filter(im -> instanceId.equals(im.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(String.format("The instance id %s could not be found", instanceId)));
    }
}
