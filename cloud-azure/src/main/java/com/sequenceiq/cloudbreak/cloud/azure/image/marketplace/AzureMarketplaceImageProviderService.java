package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Service
public class AzureMarketplaceImageProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceImageProviderService.class);

    private static final int MARKETPLACE_IMAGE_PARTS_COUNT = 4;

    private static final int INDEX_MARKETPLACE_PUBLISHER_ID = 0;

    private static final int INDER_MARKETPLACE_OFFER_ID = 1;

    private static final int INDEX_MARKETPLACE_PLAN_ID = 2;

    private static final int INDEX_MARKETPLACE_VERSION = 3;

    public AzureMarketplaceImage get(Image image) {
        String imageUri = image.getImageName();
        String[] splitUri = imageUri.split(":");
        if (splitUri.length != MARKETPLACE_IMAGE_PARTS_COUNT) {
            String errorMessage = String.format("Invalid Marketplace image URN in the image catalog! "
                    + "Please specify the image in an URN format, 4 segments separated by a colon (actual value is: %s)!", imageUri);
            LOGGER.warn(errorMessage);
            throw new CloudConnectorException(errorMessage);
        }
        return new AzureMarketplaceImage(
                splitUri[INDEX_MARKETPLACE_PUBLISHER_ID],
                splitUri[INDER_MARKETPLACE_OFFER_ID],
                splitUri[INDEX_MARKETPLACE_PLAN_ID],
                splitUri[INDEX_MARKETPLACE_VERSION]
        );
    }
}