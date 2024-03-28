package com.sequenceiq.cloudbreak.cloud.azure.image;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Service
public class AzureImageInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageInfoService.class);

    @Inject
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Inject
    private CustomVMImageNameProvider customVMImageNameProvider;

    public AzureImageInfo getImageInfo(String resourceGroup, String vhdUri, AuthenticatedContext ac, AzureClient client) {
        LOGGER.debug("About to fetch Azure image info based on the resource group [{}] and name from VHD URI [{}].", resourceGroup, vhdUri);
        String region = getRegion(ac);
        String imageNameWithRegion = customVMImageNameProvider.getImageNameWithRegion(region, vhdUri);
        String imageName = customVMImageNameProvider.getImageNameFromConnectionString(vhdUri);
        String imageId = getImageId(resourceGroup, client, imageNameWithRegion);

        AzureImageInfo imageInfo = new AzureImageInfo(imageNameWithRegion, imageName, imageId, region, resourceGroup);
        LOGGER.debug(AzureImageInfo.class.getSimpleName() + " was created: {}", imageInfo);
        return imageInfo;
    }

    private String getRegion(AuthenticatedContext ac) {
        return ac.getCloudContext()
                .getLocation()
                .getRegion()
                .getRegionName();
    }

    private String getImageId(String resourceGroup, AzureClient client, String imageName) {
        return azureResourceIdProviderService.generateImageId(
                client.getCurrentSubscription().subscriptionId(), resourceGroup, imageName);
    }

}