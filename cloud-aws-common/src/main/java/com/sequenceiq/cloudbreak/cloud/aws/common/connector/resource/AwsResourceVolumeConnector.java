package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsAdditionalDiskAttachmentService;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsAdditionalDiskCreator;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.RootVolumeFetchDto;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Service
public class AwsResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceVolumeConnector.class);

    @Inject
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Inject
    private AwsAdditionalDiskCreator awsAdditionalDiskCreator;

    @Inject
    private AwsAdditionalDiskAttachmentService awsAdditionalDiskAttachmentService;

    @Override
    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        LOGGER.debug("Calling detach volumes in AwsCommonDiskUpdateService with resources : {}", cloudResources);
        awsCommonDiskUpdateService.detachVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        LOGGER.debug("Calling delete volumes in AwsCommonDiskUpdateService with resources : {}", cloudResources);
        awsCommonDiskUpdateService.deleteVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        LOGGER.debug("Calling update volumes in AwsCommonDiskUpdateService for volumes : {} : to disk type : {} and size : {}", volumeIds,
                diskType, size);
        awsCommonDiskUpdateService.modifyVolumes(authenticatedContext, volumeIds, diskType, size);
    }

    @Override
    public List<CloudResource> createVolumes(AuthenticatedContext authenticatedContext, Group group, VolumeSetAttributes.Volume volumeRequest,
            CloudStack cloudStack, int volToAddPerInstance, List<CloudResource> cloudResources) throws CloudbreakServiceException {
        return awsAdditionalDiskCreator.createVolumes(authenticatedContext, group, volumeRequest, cloudStack, volToAddPerInstance, cloudResources);
    }

    @Override
    public void attachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources, CloudStack cloudStack)
            throws CloudbreakServiceException {
        awsAdditionalDiskAttachmentService.attachAllVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public List<CloudResource> getRootVolumes(RootVolumeFetchDto rootVolumeFetchDto) {
        return awsCommonDiskUpdateService.getRootVolumes(rootVolumeFetchDto.getAuthenticatedContext(), rootVolumeFetchDto.getGroup());
    }

    @Override
    public Map<String, Integer> getAttachedVolumeCountPerInstance(AuthenticatedContext authenticatedContext, CloudStack cloudStack,
            Collection<String> instanceIds) {
        return awsAdditionalDiskAttachmentService.getAttachedVolumeCountPerInstance(authenticatedContext, instanceIds);
    }
}
