package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Service
public class AwsNativeResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeResourceVolumeConnector.class);

    @Inject
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Override
    public void detachVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        awsCommonDiskUpdateService.detachVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public void deleteVolumes(AuthenticatedContext authenticatedContext, List<CloudResource> cloudResources) throws Exception {
        awsCommonDiskUpdateService.deleteVolumes(authenticatedContext, cloudResources);
    }

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) throws Exception {
        awsCommonDiskUpdateService.modifyVolumes(authenticatedContext, volumeIds, diskType, size);
    }
}
