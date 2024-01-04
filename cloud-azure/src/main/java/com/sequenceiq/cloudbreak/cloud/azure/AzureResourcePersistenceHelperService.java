package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureResourcePersistenceHelperService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourcePersistenceHelperService.class);

    @Inject
    private PersistenceRetriever resourcePersistenceRetriever;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    public boolean isRequested(String dnsZoneDeploymentId, ResourceType resourceType) {
        return findResourceByStatus(dnsZoneDeploymentId, CommonStatus.REQUESTED, resourceType).isPresent();
    }

    public boolean isCreated(String dnsZoneDeploymentId, ResourceType resourceType) {
        return findResourceByStatus(dnsZoneDeploymentId, CommonStatus.CREATED, resourceType).isPresent();
    }

    public CloudResource persistCloudResource(AuthenticatedContext ac, String deploymentName, String deploymentId, ResourceType resourceType) {
        LOGGER.debug("Persisting {} deployment with REQUESTED status: {} and name {}", resourceType, deploymentId, deploymentName);
        CloudResource cloudResource = buildCloudResource(deploymentName, deploymentId, CommonStatus.REQUESTED, resourceType);
        persistenceNotifier.notifyAllocation(cloudResource, ac.getCloudContext());
        return cloudResource;
    }

    public CloudResource updateCloudResource(AuthenticatedContext ac, String deploymentName, String deploymentId, CommonStatus commonStatus,
            ResourceType resourceType) {
        LOGGER.debug("Updating {} deployment to {}: {}", resourceType, commonStatus, deploymentId);
        CloudResource cloudResource = buildCloudResource(deploymentName, deploymentId, commonStatus, resourceType);
        persistenceNotifier.notifyUpdate(cloudResource, ac.getCloudContext());
        return cloudResource;
    }

    private CloudResource buildCloudResource(String name, String reference, CommonStatus status, ResourceType resourceType) {
        return CloudResource.builder()
                .withName(name)
                .withStatus(status)
                .withPersistent(true)
                .withReference(reference)
                .withType(resourceType)
                .build();
    }

    public Optional<CloudResource> findResourceByStatus(String reference, CommonStatus status, ResourceType resourceType) {
        return resourcePersistenceRetriever.notifyRetrieve(reference, status, resourceType);
    }
}
