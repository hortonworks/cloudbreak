package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public interface ResourceVolumeConnector {

    /**
     * Detaches all attached volumes on an instance.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param cloudResources       contains the list of cloud resources being modified
     * @throws Exception in case of any error
     */
    default void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Deletes all detached volumes on an instance.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param cloudResources       contains the list of cloud resources being modified
     * @throws Exception in case of any error
     */
    default void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Updates the type & size of the given disk volumes to the specified new values.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param volumeIds contains the list of cloud volumes being modified
     * @param diskType desired disk type of EBS volumes being modified
     * @param size desired disk size of EBS volumes being modified
     * @throws Exception in case of any error
     */
    default void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        throw new UnsupportedOperationException("Interface not implemented.");
    }
}
