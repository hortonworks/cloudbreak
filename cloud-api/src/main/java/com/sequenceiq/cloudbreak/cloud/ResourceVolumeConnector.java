package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

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

    /**
     * Creates new EBS volumes and attaches it to instances on cloud provider side.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param group       instance group
     * @param volumeRequest       contains the volume attributes that need to be created
     * @param cloudStack       contains the cloud stack object
     * @throws Exception        in case of any error
     */
    List<CloudResource> createAndAttachVolumes(AuthenticatedContext authenticatedContext, Group group, Volume volumeRequest, CloudStack cloudStack,
            int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException;
}
