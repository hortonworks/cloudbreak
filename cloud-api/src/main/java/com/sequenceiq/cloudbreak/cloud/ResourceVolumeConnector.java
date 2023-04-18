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
    void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception;

    /**
     * Deletes all detached volumes on an instance.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param cloudResources       contains the list of cloud resources being modified
     * @throws Exception in case of any error
     */
    void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception;
}
