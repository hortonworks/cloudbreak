package com.sequenceiq.cloudbreak.cloud.azure.image.copy;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.azure.image.copy.parallel.ParallelImageCopyService;
import com.sequenceiq.cloudbreak.cloud.azure.image.copy.sequential.SequentialImageCopyService;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Service
public class ImageCopyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCopyService.class);

    private boolean isAzureImageCopyParallelEnabled = true;

    @Inject
    private ParallelImageCopyService parallelImageCopyService;

    @Inject
    private SequentialImageCopyService sequentialImageCopyService;

    public void copyImage(Image image, AzureClient client, String imageStorageName, String imageResourceGroupName, AzureImageInfo azureImageInfo) {
        if (isAzureImageCopyParallelEnabled) {
            LOGGER.info("Running parallel image copy {}", azureImageInfo);
            parallelImageCopyService.copyImage(image, client, imageStorageName, imageResourceGroupName, azureImageInfo);
        } else {
            LOGGER.info("Running sequential image copy {}", azureImageInfo);
            sequentialImageCopyService.copyImage(image, client, imageStorageName, imageResourceGroupName, azureImageInfo);
        }
    }
}
