package com.sequenceiq.cloudbreak.cloud.azure.resource;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Service
public class AzureResourceVolumeConnector implements ResourceVolumeConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceVolumeConnector.class);

    @Inject
    private AzureVolumeResourceBuilder azureVolumeResourceBuilder;

    @Override
    public void updateDiskVolumes(AuthenticatedContext authenticatedContext, List<String> volumeIds, String diskType, int size) {
        LOGGER.info("Calling update volumes in AwsCommonDiskUpdateService for volumes : {} : to disk type : {} and size : {}", volumeIds,
                diskType, size);
        azureVolumeResourceBuilder.modifyVolumes(authenticatedContext, volumeIds, diskType, size);
    }
}
