package com.sequenceiq.cloudbreak.service.metering;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTypeMetadata;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class MeteringInstanceCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringInstanceCheckerService.class);

    private static final String META_DATA = "meta-data";

    private static final String INSTANCE_TYPE = "instance-type";

    private static final String PROVIDER = "provider";

    private static final String SALT = "salt";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CloudContextProvider cloudContextProvider;

    @Inject
    private MismatchedInstanceHandlerService mismatchedInstanceHandlerService;

    public void checkInstanceTypes(StackDto stack) {
        try {
            checkInstanceTypesWithFallback(stack);
        } catch (Exception e) {
            LOGGER.warn("Checking instance types with salt failed. Fallback and check instance types on provider.", e);
            try {
                checkInstanceTypesOnProvider(stack);
            } catch (ProviderAuthenticationFailedException ex) {
                LOGGER.warn("Checking instance types on provider failed: {}", ex.getMessage());
            }
        }
    }

    private void checkInstanceTypesWithFallback(StackDto stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Map<String, JsonNode> responseByHost = hostOrchestrator.getGrainOnAllHosts(gatewayConfig, META_DATA);
        Map<String, String> instanceTypesByHost = responseByHost.entrySet().stream()
                .filter(entry -> !StringUtils.equalsAny(entry.getValue().textValue(), "false", "null", "") && entry.getValue().has(INSTANCE_TYPE))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue().get(INSTANCE_TYPE).textValue()));
        if (!instanceTypesByHost.isEmpty()) {
            Map<String, String> instanceIdsByHost = stack.getAllAvailableInstances().stream()
                    .filter(instanceMetadata -> instanceMetadata.getDiscoveryFQDN() != null)
                    .collect(Collectors.toMap(InstanceMetadataView::getDiscoveryFQDN, InstanceMetadataView::getInstanceId));
            Map<String, String> instanceTypesByInstanceId = instanceTypesByHost.entrySet().stream()
                    .filter(instanceTypeEntry -> instanceIdsByHost.containsKey(instanceTypeEntry.getKey()))
                    .collect(Collectors.toMap(instanceTypeEntry -> instanceIdsByHost.get(instanceTypeEntry.getKey()), Map.Entry::getValue));
            compareInstanceTypes(stack, instanceTypesByInstanceId, SALT);
        } else {
            throw new RuntimeException("Getting metadata grain from salt returned empty results.");
        }
    }

    private void checkInstanceTypesOnProvider(StackDto stack) {
        Collection<InstanceMetadataView> instanceMetaDataList = stack.getAllAvailableInstances();
        if (instanceMetaDataList.isEmpty()) {
            LOGGER.debug("No available instance found.");
            return;
        }
        List<String> instanceIds = instanceMetaDataList.stream().map(InstanceMetadataView::getInstanceId).toList();
        CloudContext cloudContext = cloudContextProvider.getCloudContext(stack);
        CloudCredential cloudCredential = credentialClientService.getCloudCredential(stack.getEnvironmentCrn());
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        InstanceTypeMetadata instanceTypeMetadata = connector.metadata().collectInstanceTypes(ac, instanceIds);
        compareInstanceTypes(stack, instanceTypeMetadata.getInstanceTypes(), PROVIDER);
    }

    private void compareInstanceTypes(StackDto stack, Map<String, String> instanceTypes, String source) {
        Set<MismatchingInstanceGroup> mismatchingInstanceGroups = collectMismatchingInstanceGroup(stack, instanceTypes, source);
        mismatchedInstanceHandlerService.handleMismatchingInstanceTypes(stack, mismatchingInstanceGroups);
    }

    private Set<MismatchingInstanceGroup> collectMismatchingInstanceGroup(StackDto stack, Map<String, String> instanceTypes, String source) {
        Set<MismatchingInstanceGroup> mismatchingInstanceGroups = new HashSet<>();
        for (InstanceGroupDto instanceGroup : stack.getInstanceGroupDtos()) {
            String instanceTypeFromTemplate = instanceGroup.getInstanceGroup().getTemplate().getInstanceType();
            Map<String, String> actualInstanceTypesInGroup = new HashMap<>();
            boolean mismatchingInstanceType = false;
            for (InstanceMetadataView instanceMetadata : instanceGroup.getNotDeletedAndNotZombieInstanceMetaData()) {
                String actualInstanceType = instanceTypes.get(instanceMetadata.getInstanceId());
                if (!instanceTypes.containsKey(instanceMetadata.getInstanceId())) {
                    LOGGER.warn("Missig actual instance type info for instance with instanceId: {}, fqdn: {}, instanceTypeFromTemplate: {}, source: {}",
                            instanceMetadata.getInstanceId(), instanceMetadata.getDiscoveryFQDN(), instanceTypeFromTemplate, source);
                } else if (instanceTypeFromTemplate != null && !instanceTypeFromTemplate.equals(actualInstanceType)) {
                    LOGGER.warn("Instance type is different in our DB and on the cluster for instance with instanceId: {}, fqdn: {}, " +
                                    "instanceTypeFromTemplate: {}, actualInstanceType: {}, source: {}",
                            instanceMetadata.getInstanceId(), instanceMetadata.getDiscoveryFQDN(), instanceTypeFromTemplate, actualInstanceType, source);
                    mismatchingInstanceType = true;
                    actualInstanceTypesInGroup.put(instanceMetadata.getInstanceId(), actualInstanceType);
                } else {
                    actualInstanceTypesInGroup.put(instanceMetadata.getInstanceId(), actualInstanceType);
                }
            }
            if (mismatchingInstanceType) {
                mismatchingInstanceGroups.add(new MismatchingInstanceGroup(instanceGroup.getInstanceGroup().getGroupName(),
                        instanceTypeFromTemplate, actualInstanceTypesInGroup));
            }
        }
        return mismatchingInstanceGroups;
    }
}
