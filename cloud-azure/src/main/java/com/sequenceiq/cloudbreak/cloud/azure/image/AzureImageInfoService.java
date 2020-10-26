package com.sequenceiq.cloudbreak.cloud.azure.image;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureResourceIdProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.util.CustomVMImageNameProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Service
public class AzureImageInfoService {

    @Inject
    private AzureResourceIdProviderService azureResourceIdProviderService;

    @Inject
    private CustomVMImageNameProvider customVMImageNameProvider;

    public AzureImageInfo getImageInfo(String resourceGroup, String fromVhdUri, AuthenticatedContext ac, AzureClient client) {
        String region = getRegion(ac);
        String imageNameWithRegion = customVMImageNameProvider.getImageNameWithRegion(region, fromVhdUri);
        String imageName = customVMImageNameProvider.getImageNameFromConnectionString(fromVhdUri);
        String imageId = getImageId(resourceGroup, client, imageNameWithRegion);

        return new AzureImageInfo(imageNameWithRegion, imageName, imageId, region, resourceGroup);
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
