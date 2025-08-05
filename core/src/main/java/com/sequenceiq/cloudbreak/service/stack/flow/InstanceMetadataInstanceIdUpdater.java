package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class InstanceMetadataInstanceIdUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataInstanceIdUpdater.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void updateWithInstanceIdAndStatus(StackCreationContext stackContext, List<CloudResourceStatus> affectedResources) {
        CloudConnector connector = cloudPlatformConnectors.get(stackContext.getCloudContext().getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(stackContext.getCloudContext(), stackContext.getCloudCredential());
        try {
            updateWithInstanceIdAndStatus(ac, connector, affectedResources);
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Transaction error occurred while updating instance ids and status for instance metadata", e);
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    public void updateWithInstanceIdAndStatus(AuthenticatedContext ac, CloudConnector connector, List<CloudResourceStatus> cloudResourceStatuses)
            throws TransactionService.TransactionExecutionException {
        LOGGER.info("Syncing the status and instanceId of the created instances to the 'instancemetadata' table...");

        transactionService.required(() -> {
            try {
                ResourceType instanceResourceType = connector.resources().getInstanceResourceType();
                List<CloudResource> instanceCloudResources = getCreatedInstanceCloudResourcesWithType(cloudResourceStatuses, instanceResourceType);
                List<InstanceMetaData> requestedInstanceMetadatas = instanceMetaDataService.findAllByStackIdAndStatus(ac.getCloudContext().getId(), REQUESTED);
                LOGGER.debug("Requested instance metadata entries with private ids: {}",
                        requestedInstanceMetadatas.stream().map(InstanceMetaData::getPrivateId).collect(Collectors.toSet()));
                for (InstanceMetaData instanceMetaData : requestedInstanceMetadatas) {
                    Optional<CloudResource> cloudResource = getCloudInstanceResourceForInstanceMetadata(instanceMetaData, instanceCloudResources);
                    cloudResource.ifPresentOrElse(cr -> {
                        instanceMetaData.setInstanceId(cr.getInstanceId());
                        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
                    }, () -> LOGGER.warn("The instanceId and instanceStatus for instance {} was not set, " +
                            "because the corresponding resource was not found int the 'resource' table", instanceMetaData.getInstanceName()));
                }
                LOGGER.info("Updated instance metadatas with instance id to '{}' status: {}", InstanceStatus.CREATED, requestedInstanceMetadatas);
                instanceMetaDataService.saveAll(requestedInstanceMetadatas);
            } catch (UnsupportedOperationException uo) {
                LOGGER.warn("Instance metadata update is not supported for cloud platform variant: {}", ac.getCloudContext().getPlatformVariant(), uo);
            }
        });
    }

    private List<CloudResource> getCreatedInstanceCloudResourcesWithType(List<CloudResourceStatus> cloudResourceStatuses, ResourceType instanceResourceType) {
        return cloudResourceStatuses.stream()
                .filter(cloudResourceStatus -> ResourceStatus.CREATED.equals(cloudResourceStatus.getStatus()))
                .map(CloudResourceStatus::getCloudResource)
                .filter(cloudResource -> instanceResourceType.equals(cloudResource.getType()))
                .toList();
    }

    private Optional<CloudResource> getCloudInstanceResourceForInstanceMetadata(InstanceMetaData instanceMetaData, List<CloudResource> instanceCloudResources) {
        return instanceCloudResources.stream()
                .filter(cr -> instanceMetaData.getPrivateId().equals(cr.getPrivateId())
                        && instanceMetaData.getInstanceGroup().getGroupName().equals(cr.getGroup()))
                .findFirst();
    }
}
