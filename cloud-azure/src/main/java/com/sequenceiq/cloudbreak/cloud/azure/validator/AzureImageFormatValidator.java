package com.sequenceiq.cloudbreak.cloud.azure.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@Component
public class AzureImageFormatValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageFormatValidator.class);

    private static final Pattern MARKETPLACE_PATTERN =
            Pattern.compile("^[a-zA-Z0-9-_]+:[a-zA-Z0-9-_]+:[a-zA-Z0-9-_.]+:((\\d+\\.)?(\\d+\\.)?(\\*|\\d+)|(latest))$");

    private static final Pattern VHD_PATTERN = Pattern.compile("^(http).*(\\.vhd)$");

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Inject
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @Value("${cb.arm.marketplace.image.automatic.signer:false}")
    private boolean enableAzureImageTermsAutomaticSigner;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        Image image = cloudStack.getImage();
        String imageUri = image.getImageName();

        if (isVhdImageFormat(image)) {
            LOGGER.debug("Image {} seems to be a valid VHD image", imageUri);

        } else if (isMarketplaceImageFormat(image)) {
            LOGGER.debug("Checking presence of Azure Marketplace entitlement for Marketplace image {}", imageUri);
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            if (!entitlementService.azureMarketplaceImagesEnabled(accountId)) {
                String errorMessage = String.format("Your image %s seems to be an Azure Marketplace image! "
                        + "If you would like to use it please open Cloudera support ticket to enable this capability!", imageUri);
                LOGGER.warn(errorMessage);
                throw new CloudConnectorException(errorMessage);
            }
            LOGGER.debug("Checking if Terms and Conditions for your Azure Marketplace image {} are accepted", imageUri);
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            AzureMarketplaceImage azureMarketplaceImage = azureMarketplaceImageProviderService.get(image);
            if (!enableAzureImageTermsAutomaticSigner
                    && !azureImageTermsSignerService.isSigned(azureClient.getCurrentSubscription().subscriptionId(), azureMarketplaceImage, azureClient)) {
                String errorMessage = String.format("Your image %s seems to be an Azure Marketplace image, "
                        + "however its Terms and Conditions are not accepted! On how to accept them please refer to azure documentation " +
                        "at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.", imageUri);
                LOGGER.warn(errorMessage);
                throw new CloudConnectorException(errorMessage);
            }

        } else {
            String errorMessage = String.format("Your image name %s is invalid. Please check the desired format in the documentation!", imageUri);
            LOGGER.warn(errorMessage);
            throw new CloudConnectorException(errorMessage);
        }
    }

    public boolean isMarketplaceImageFormat(Image image) {
        String imageUri = image.getImageName();
        Matcher matcher = MARKETPLACE_PATTERN.matcher(imageUri);
        if (matcher.matches()) {
            LOGGER.debug("Image with name {} is a valid marketplace image", imageUri);
            return true;
        } else {
            LOGGER.debug("Image with name {} is not a valid marketplace image", imageUri);
            return false;
        }
    }

    public boolean isVhdImageFormat(Image image) {
        String imageUri = image.getImageName();
        Matcher matcher = VHD_PATTERN.matcher(imageUri);
        if (matcher.matches()) {
            LOGGER.debug("Image with name {} is a valid VHD based image", imageUri);
            return true;
        } else {
            LOGGER.debug("Image with name {} is not a valid VHD based image", imageUri);
            return false;
        }
    }
}
