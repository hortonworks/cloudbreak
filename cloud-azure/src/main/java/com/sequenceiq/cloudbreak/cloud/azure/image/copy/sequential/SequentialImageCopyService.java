package com.sequenceiq.cloudbreak.cloud.azure.image.copy.sequential;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureStorage.IMAGES_CONTAINER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Service
public class SequentialImageCopyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialImageCopyService.class);

    public void copyImage(Image image, AzureClient client, String imageStorageName, String imageResourceGroupName, AzureImageInfo azureImageInfo) {
        LOGGER.info("Starting to copy image: {}, into storage account: {}", image.getImageName(), imageStorageName);
        client.copyImageBlobInStorageContainer(
                imageResourceGroupName, imageStorageName, IMAGES_CONTAINER, image.getImageName(), azureImageInfo.getImageName());
    }


}
