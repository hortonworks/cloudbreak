package com.sequenceiq.cloudbreak.cloud.notification;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * The implementations of this interface are used to retrieve data from the database.
 */
public interface PersistenceRetriever {

    /**
     * Retrieve resources from the database based on the given parameters.
     *
     * @param resourceReference the resource reference. Basically, this is the ID of the resource.
     * @param status the status of the resource.
     * @param resourceType the type of the resource.
     * @return the retrieved {@link CloudResource}
     */
    Optional<CloudResource> notifyRetrieve(String resourceReference, CommonStatus status, ResourceType resourceType);

    /**
     * Retrieve resources from the database based on the given parameters.
     *
     * @param resourceReference the resource reference. Basically, this is the ID of the resource.
     * @param status the status of the resource.
     * @param resourceType the type of the resource.
     * @param stackId  Id of the related cloud stack
     * @return the retrieved {@link CloudResource}
     */
    default Optional<CloudResource> notifyRetrieve(Long stackId, String resourceReference, CommonStatus status, ResourceType resourceType) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }
}
