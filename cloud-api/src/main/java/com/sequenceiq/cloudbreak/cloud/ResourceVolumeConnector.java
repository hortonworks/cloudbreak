package com.sequenceiq.cloudbreak.cloud;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

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

    /**
     * Creates new EBS volumes on cloud provider side.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param group       instance group
     * @param volumeRequest       contains the volume attributes that need to be created
     * @param cloudStack       contains the cloud stack object
     * @param volToAddPerInstance       number of volumes to add per instance in the group
     * @param cloudResources       resources that are being modified
     * @throws Exception        in case of any error
     */
    default List<CloudResource> createVolumes(AuthenticatedContext authenticatedContext, Group group, VolumeSetAttributes.Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Attaches created block storages to instances on cloud provider side.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param cloudResources       resources that are being modified
     * @throws Exception        in case of any error
     */
    default void attachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, CloudStack cloudStack)
            throws CloudbreakServiceException {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Gets root volume information of instances on cloud provider side.
     *
     * @param rootVolumeFetchDto the dto that contains all required parameters for fetching Root Volumes
     * @return returns a list of root volume resources for the instances in the cloud stack
     * @throws Exception        in case of any error
     */
    default List<CloudResource> getRootVolumes(RootVolumeFetchDto rootVolumeFetchDto) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    /**
     * Gets the count of attached volumes per instance.
     *
     * @param authenticatedContext the authenticated context which holds the client object
     * @param instanceIds          the list of instance IDs for which to get the attached volume counts
     * @return a map where keys are instance IDs and values are the counts of attached volumes
     */
    default Map<String, Integer> getAttachedVolumeCountPerInstance(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            Collection<String> instanceIds) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }
}
