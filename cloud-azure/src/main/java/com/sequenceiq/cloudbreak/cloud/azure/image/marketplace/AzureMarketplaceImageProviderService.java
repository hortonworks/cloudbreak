package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@Service
public class AzureMarketplaceImageProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceImageProviderService.class);

    private static final int MARKETPLACE_IMAGE_PARTS_COUNT = 4;

    private static final int INDEX_MARKETPLACE_PUBLISHER_ID = 0;

    private static final int INDER_MARKETPLACE_OFFER_ID = 1;

    private static final int INDEX_MARKETPLACE_PLAN_ID = 2;

    private static final int INDEX_MARKETPLACE_VERSION = 3;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    public AzureMarketplaceImage get(Image image) {
        return get(image.getImageName(), false);
    }

    public AzureMarketplaceImage getSourceImage(Image image) {
        String imageUrn = image.getPackageVersions().get(ImagePackageVersion.SOURCE_IMAGE.getKey());
        return get(imageUrn, true);
    }

    private AzureMarketplaceImage get(String imageUrn, boolean useAsSourceImage) {
        if (StringUtils.isBlank(imageUrn)) {
            String errorMessage = "Missing Marketplace image URN! "
                    + "Please specify the image in an URN format, 4 segments separated by a colon in the image catalog";
            LOGGER.warn(errorMessage);
            throw new CloudConnectorException(errorMessage);
        }
        if (!hasMarketplaceFormat(imageUrn)) {
            String errorMessage = String.format("Invalid Marketplace image URN in the image catalog! "
                    + "Please specify the image in an URN format, 4 segments separated by a colon (actual value is: %s)!", imageUrn);
            LOGGER.warn(errorMessage);
            throw new CloudConnectorException(errorMessage);
        }
        String[] splitUri = imageUrn.split(":");
        return new AzureMarketplaceImage(
                splitUri[INDEX_MARKETPLACE_PUBLISHER_ID],
                splitUri[INDER_MARKETPLACE_OFFER_ID],
                splitUri[INDEX_MARKETPLACE_PLAN_ID],
                splitUri[INDEX_MARKETPLACE_VERSION],
                useAsSourceImage
        );
    }

    public boolean hasMarketplaceFormat(String imageName) {
        return azureImageFormatValidator.isMarketplaceImageFormat(imageName);
    }
}
