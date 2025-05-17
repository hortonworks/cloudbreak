package com.sequenceiq.cloudbreak.service.instancechecker;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCheckMetadata;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.conf.InstanceCheckerConfig;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class InstanceCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceCheckerService.class);

    private static final String AWS_AUTH_ERROR_MESSAGE = "is not authorized to perform";

    private static final String PROVIDER_INSTANCES_ARE_DIFFERENT = "PROVIDER_INSTANCES_ARE_DIFFERENT";

    private static final String POSSIBLE_OPRHAN_INSTANCES = "POSSIBLE_ORPHAN_INSTANCES";

    private static final int NUMBER_OF_EVENTS_TO_CHECK = 2;

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

    @Inject
    private MeteringConfig meteringConfig;

    @Inject
    private InstanceCheckerConfig instanceCheckerConfig;

    @Inject
    private StackToCloudStackConverter stackToCloudStackConverter;

    public void checkInstances(Long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        try {
            CloudContext cloudContext = cloudContextProvider.getCloudContext(stack);
            CloudCredential cloudCredential = credentialClientService.getCloudCredential(stack.getEnvironmentCrn());
            CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
            AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
            List<String> knownInstanceIds = stack.getAllAvailableInstances().stream().map(InstanceMetadataView::getInstanceId).toList();
            List<InstanceCheckMetadata> aliveProviderInstances = connector.metadata()
                    .collectCdpInstances(ac, stack.getResourceCrn(), stackToCloudStackConverter.convert(stack), knownInstanceIds).stream()
                    .filter(i -> !terminatedInstanceStatuses().contains(i.status()))
                    .toList();

            if (instanceCheckerConfig.isEnabled()) {
                try {
                    checkForOrphanInstances(connector, stack, aliveProviderInstances);
                } catch (Exception e) {
                    LOGGER.error("Unexpected error during instance checker job", e);
                }
            }
            if (meteringConfig.isEnabled() && meteringConfig.isInstanceCheckerEnabled() && StackType.WORKLOAD.equals(stack.getType())) {
                syncInstanceTypes(stack, aliveProviderInstances);
            }
        } catch (ProviderAuthenticationFailedException ex) {
            LOGGER.warn("Checking instances on provider failed due to auth failure: {}", ex.getMessage());
        } catch (Exception ex) {
            if (ex.getMessage() != null && ex.getMessage().contains(AWS_AUTH_ERROR_MESSAGE)) {
                LOGGER.warn("Checking instances on provider failed due to auth failure: {}", ex.getMessage());
            } else {
                throw ex;
            }
        }
    }

    private EnumSet<InstanceStatus> terminatedInstanceStatuses() {
        return EnumSet.of(InstanceStatus.TERMINATED, InstanceStatus.TERMINATED_BY_PROVIDER);
    }

    private void checkForOrphanInstances(CloudConnector cloudConnector, Stack stack, List<InstanceCheckMetadata> providerInstances) {
        List<InstanceMetadataView> instanceMetadataViewsFromDB = stack.getAllAvailableInstances();
        if (instanceMetadataViewsFromDB.isEmpty()) {
            LOGGER.debug("No available instance found.");
            return;
        }

        List<InstanceCheckMetadata> instancesMissingFromInstanceMetadataButPresentOnProvider =
                providerInstances.stream()
                        .filter(i -> !i.status().isTransient())
                        .filter(providerInstance -> instanceMetadataViewsFromDB.stream()
                                .noneMatch(dbInstanceMetadata -> Objects.equals(dbInstanceMetadata.getInstanceId(), providerInstance.instanceId())))
                        .toList();
        if (!instancesMissingFromInstanceMetadataButPresentOnProvider.isEmpty()) {
            LOGGER.warn("There are instances on the provider that are not present in the 'instancemetadata' table: {}",
                    instancesMissingFromInstanceMetadataButPresentOnProvider);
            if (shouldSendWarningMessage(stack, POSSIBLE_OPRHAN_INSTANCES)) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), POSSIBLE_OPRHAN_INSTANCES, ResourceEvent.STACK_POSSIBLE_ORPHAN_INSTANCES,
                        Set.of(instancesMissingFromInstanceMetadataButPresentOnProvider.stream().map(InstanceCheckMetadata::instanceId).toList().toString()));
            }
        }
    }

    private void syncInstanceTypes(Stack stack, List<InstanceCheckMetadata> instanceCheckMetadata) {
        if (stack.getAllAvailableInstances().isEmpty()) {
            LOGGER.debug("No available instance found.");
            return;
        }

        Map<String, String> providerInstanceTypes = instanceCheckMetadata.stream()
                .collect(Collectors.toMap(InstanceCheckMetadata::instanceId, InstanceCheckMetadata::instanceType));
        Set<String> mismatchingInstanceIds = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Set<InstanceMetaData> instanceMetadataSet = instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet().stream()
                    .filter(imd -> StringUtils.isNotEmpty(imd.getInstanceId()))
                    .collect(Collectors.toSet());
            for (InstanceMetaData instanceMetadata : instanceMetadataSet) {
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
                                instanceMetadata.getInstanceId(), providerInstanceType, instanceGroup.getTemplate().getInstanceType());
                        mismatchingInstanceIds.add(instanceMetadata.getInstanceId());
                    }
                } else {
                    LOGGER.warn("Instance is missing from instance type response: {}", instanceMetadata.getInstanceId());
                }
            }
        }

        if (!mismatchingInstanceIds.isEmpty()) {
            if (shouldSendWarningMessage(stack, PROVIDER_INSTANCES_ARE_DIFFERENT)) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), PROVIDER_INSTANCES_ARE_DIFFERENT, ResourceEvent.STACK_PROVIDER_INSTANCE_TYPE_MISMATCH,
                        Set.of(mismatchingInstanceIds.toString()));
            }
        }
    }

    private boolean shouldSendWarningMessage(Stack stack, String notificationType) {
        List<StructuredNotificationEvent> latestEvents =
                cloudbreakEventService.cloudbreakLastEventsForStack(stack.getId(), stack.getType().getResourceType(), NUMBER_OF_EVENTS_TO_CHECK);
        return latestEvents.stream()
                .noneMatch(event -> event.getNotificationDetails() != null && notificationType.equals(event.getNotificationDetails().getNotificationType()));
    }
}