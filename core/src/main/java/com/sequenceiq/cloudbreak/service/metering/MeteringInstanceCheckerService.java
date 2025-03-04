package com.sequenceiq.cloudbreak.service.metering;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class MeteringInstanceCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringInstanceCheckerService.class);

    private static final String AWS_AUTH_ERROR_MESSAGE = "is not authorized to perform";

    private static final String META_DATA = "meta-data";

    private static final String INSTANCE_TYPE = "instance-type";

    private static final String PROVIDER = "provider";

    private static final String SALT = "salt";

    private static final String PROVIDER_INSTANCES_ARE_DIFFERENT = "PROVIDER_INSTANCES_ARE_DIFFERENT";

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
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackService stackService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void checkInstanceTypes(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        try {
            checkInstanceTypesWithFallback(stack);
        } catch (Exception e) {
            LOGGER.warn("Checking instance types with salt failed. Fallback and check instance types on provider.", e);
            try {
                checkInstanceTypesOnProvider(stack);
            } catch (ProviderAuthenticationFailedException ex) {
                LOGGER.warn("Checking instance types on provider failed due to auth failure: {}", ex.getMessage());
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().contains(AWS_AUTH_ERROR_MESSAGE)) {
                    LOGGER.warn("Checking instance types on provider failed due to auth failure: {}", ex.getMessage());
                } else {
                    throw ex;
                }
            }
        }
    }

    private void checkInstanceTypesWithFallback(Stack stack) throws CloudbreakOrchestratorFailedException {
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
            syncInstanceTypes(stack, instanceTypesByInstanceId, SALT);
        } else {
            throw new RuntimeException("Getting metadata grain from salt returned empty results.");
        }
    }

    private void checkInstanceTypesOnProvider(Stack stack) {
        List<InstanceMetadataView> instanceMetaDataList = stack.getAllAvailableInstances();
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
        syncInstanceTypes(stack, instanceTypeMetadata.getInstanceTypes(), PROVIDER);
    }

    private void syncInstanceTypes(Stack stack, Map<String, String> providerInstanceTypes, String source) {
        Set<String> mismatchingInstanceIds = new HashSet<>();

        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetadata : instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet()) {
                if (StringUtils.isNotEmpty(instanceMetadata.getInstanceId())) {
                    if (providerInstanceTypes.containsKey(instanceMetadata.getInstanceId())) {
                        String providerInstanceType = providerInstanceTypes.get(instanceMetadata.getInstanceId());
                        if (!providerInstanceType.equals(instanceMetadata.getProviderInstanceType())) {
                            LOGGER.info("Update {} provider instance type from {} to {}",
                                    instanceMetadata.getInstanceId(), instanceMetadata.getProviderInstanceType(), providerInstanceType);
                            instanceMetadata.setProviderInstanceType(providerInstanceType);
                            instanceMetaDataService.save(instanceMetadata);
                        }
                        if (!instanceGroup.getTemplate().getInstanceType().equals(providerInstanceType)) {
                            LOGGER.warn("Instance {} instance type {} does not match template {}",
                                    instanceMetadata.getInstanceId(), providerInstanceType, instanceMetadata.getProviderInstanceType());
                            mismatchingInstanceIds.add(instanceMetadata.getInstanceId());
                        }
                    } else {
                        LOGGER.warn("Instance is missing from instance type response: {}", instanceMetadata.getInstanceId());
                    }
                }
            }
        }

        if (!mismatchingInstanceIds.isEmpty()) {
            Optional<StructuredNotificationEvent> latestEvent = cloudbreakEventService.cloudbreakLastEventsForStack(stack.getId(),
                    stack.getType().getResourceType(), 1).stream().findFirst();
            if (shouldSendWarningMessage(latestEvent)) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), PROVIDER_INSTANCES_ARE_DIFFERENT, ResourceEvent.STACK_PROVIDER_INSTANCE_TYPE_MISMATCH,
                        Set.of(mismatchingInstanceIds.toString()));
            }
        }
    }

    private boolean shouldSendWarningMessage(Optional<StructuredNotificationEvent> latestEvent) {
        if (latestEvent.isEmpty()) {
            return true;
        }
        StructuredNotificationEvent event = latestEvent.get();
        return event.getNotificationDetails() != null && !PROVIDER_INSTANCES_ARE_DIFFERENT.equals(event.getNotificationDetails().getNotificationType());
    }
}
