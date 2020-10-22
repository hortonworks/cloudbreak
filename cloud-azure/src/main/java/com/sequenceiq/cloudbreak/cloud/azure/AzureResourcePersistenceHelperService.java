package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Optional;

import javax.inject.Inject;

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
        return findDeploymentByStatus(dnsZoneDeploymentId, CommonStatus.REQUESTED, resourceType).isPresent();
    }

    public boolean isCreated(String dnsZoneDeploymentId, ResourceType resourceType) {
        return findDeploymentByStatus(dnsZoneDeploymentId, CommonStatus.CREATED, resourceType).isPresent();
    }

    public void persistCloudResource(AuthenticatedContext ac, String deploymentName, String deploymentId, ResourceType resourceType) {
        LOGGER.debug("Persisting {} deployment with REQUESTED status: {} and name {}", resourceType, deploymentId, deploymentName);
        persistenceNotifier.notifyAllocation(buildCloudResource(deploymentName, deploymentId, CommonStatus.REQUESTED, resourceType), ac.getCloudContext());
    }

    public void updateCloudResource(AuthenticatedContext ac, String deploymentName, String deploymentId, CommonStatus commonStatus,
            ResourceType resourceType) {
        LOGGER.debug("Updating {} deployment to {}: {}", resourceType, commonStatus, deploymentId);
        persistenceNotifier.notifyUpdate(buildCloudResource(deploymentName, deploymentId, commonStatus, resourceType), ac.getCloudContext());
    }

    private CloudResource buildCloudResource(String name, String reference, CommonStatus status, ResourceType resourceType) {
        return CloudResource.builder()
                .name(name)
                .status(status)
                .persistent(true)
                .reference(reference)
                .type(resourceType)
                .build();
    }

    private Optional<CloudResource> findDeploymentByStatus(String reference, CommonStatus status, ResourceType resourceType) {
        return resourcePersistenceRetriever.notifyRetrieve(reference, status, resourceType);
    }
}
