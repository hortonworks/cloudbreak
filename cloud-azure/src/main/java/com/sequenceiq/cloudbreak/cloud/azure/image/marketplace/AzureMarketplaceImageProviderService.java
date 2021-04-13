package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Service
public class AzureMarketplaceImageProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceImageProviderService.class);

    public AzureMarketplaceImage get(Image image) {
        String imageUri = image.getImageName();
        String[] splitUri = imageUri.split(":");
        if (splitUri.length != 4) {
            String errorMessage = String.format("Invalid Marketplace image URN in the image catalog! "
                    + "Please specify the image in an URN format, 4 segments separated by a colon (actual value is: %s)!", imageUri);
            LOGGER.warn(errorMessage);
            throw new CloudConnectorException(errorMessage);
        }
        return new AzureMarketplaceImage(splitUri[0], splitUri[1], splitUri[2], splitUri[3]);
    }
}
