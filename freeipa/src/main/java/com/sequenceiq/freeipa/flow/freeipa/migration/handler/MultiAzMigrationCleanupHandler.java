package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.flow.freeipa.migration.MultiAzMigrationFinalizeFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationCleanupRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationFinalizeFailedEvent;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.resource.ResourceService;

@Component
public class MultiAzMigrationCleanupHandler extends ExceptionCatcherEventHandler<MultiAzMigrationCleanupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzMigrationCleanupHandler.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(MultiAzMigrationCleanupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<MultiAzMigrationCleanupRequest> event) {
        LOGGER.error("Unexpected error during resource cleanup for multi-AZ migration of stack {}", resourceId, e);
        return new MultiAzMigrationFinalizeFailedEvent(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<MultiAzMigrationCleanupRequest> event) {
        MultiAzMigrationCleanupRequest request = event.getData();
        Long stackId = request.getResourceId();
        CloudContext cloudContext = request.getCloudContext();
        CloudCredential cloudCredential = request.getCloudCredential();
        CloudStack cloudStack = request.getCloudStack();

        List<Resource> availabilitySets =
                resourceService.findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus.CREATED, ResourceType.AZURE_AVAILABILITY_SET, stackId);

        if (availabilitySets.isEmpty()) {
            LOGGER.info("No availability sets found for stack {}, skipping cleanup", stackId);
        } else {
            List<String> availabilitySetNames = availabilitySets.stream().map(Resource::getResourceName).toList();
            LOGGER.info("Deleting {} availability set(s) for stack {}: {}", availabilitySetNames.size(), stackId, availabilitySetNames);
            AuthenticatedContext ac = cloudPlatformConnectors.get(cloudContext.getPlatform(), cloudContext.getVariant())
                    .authentication().authenticate(cloudContext, cloudCredential);
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, cloudStack);
            azureUtils.deleteAvailabilitySets(azureClient, resourceGroupName, availabilitySetNames);
            resourceService.deleteAll(availabilitySets);
            LOGGER.info("Deleted availability sets for stack {}", stackId);
        }

        return new StackEvent(MultiAzMigrationFinalizeFlowEvent.MULTI_AZ_MIGRATION_CLEANUP_FINISHED_EVENT.event(), stackId);
    }
}
