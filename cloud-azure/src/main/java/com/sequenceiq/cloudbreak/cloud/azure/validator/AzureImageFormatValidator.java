package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermStatus;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;

@Component
public class AzureImageFormatValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageFormatValidator.class);

    private static final Pattern MARKETPLACE_PATTERN =
            Pattern.compile("^[a-zA-Z0-9-_]+:[a-zA-Z0-9-_]+:[a-zA-Z0-9-_.]+:((\\d+\\.)?(\\d+\\.)?(\\*|\\d+)|(latest))$");

    private static final Pattern VHD_PATTERN = Pattern.compile("^(http).*(\\.vhd)$");

    private static final String VHD_AVAILABLE = "vhdAvailable";

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Inject
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @Override
    public void validate(AuthenticatedContext ac, CloudStack cloudStack) {
        Image image = cloudStack.getImage();
        String imageUri = image.getImageName();

        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        boolean onlyMarketplaceImagesEnabled = entitlementService.azureOnlyMarketplaceImagesEnabled(accountId);
        boolean vhdAvailable = Boolean.parseBoolean(cloudStack.getParameters().get(VHD_AVAILABLE));
        if (isVhdImageFormat(image)) {
            validateVhdPermitted(imageUri, onlyMarketplaceImagesEnabled);
        } else if (isMarketplaceImageFormat(image)) {
            boolean marketplaceImagesEnabled = entitlementService.azureMarketplaceImagesEnabled(accountId);
            validateMarketplacePermitted(imageUri, marketplaceImagesEnabled);
            AzureMarketplaceImage azureMarketplaceImage = azureMarketplaceImageProviderService.get(image);
            AzureImageTermStatus termsStatus = getTermsStatus(ac, azureMarketplaceImage);
            if (!isAutomaticTermsSignerAccepted(cloudStack)) {
                switch (termsStatus) {
                    case NOT_ACCEPTED -> handleTermsNotAccepted(imageUri, onlyMarketplaceImagesEnabled, vhdAvailable);
                    case NON_READABLE -> handleTermsNonReadable(imageUri, onlyMarketplaceImagesEnabled, vhdAvailable);
                    default -> LOGGER.debug("Image terms are already accepted for image {}, there is nothing to do here", imageUri);
                }

            } else {
                LOGGER.debug("The Terms and Conditions are accepted for Azure Marketplace image {} or the automatic signer is enabled.", imageUri);
            }

        } else {
            handleUnknownFormat(imageUri);
        }
    }

    private void handleTermsNonReadable(String imageUri, boolean onlyMarketplaceImagesEnabled, boolean vhdAvailable) {
        if (onlyMarketplaceImagesEnabled && !vhdAvailable) {
            String warningMessage = String.format("Cloudera Management Console does not have sufficient permissions to read if "
                    + "Terms and Conditions are accepted for the Azure Marketplace image %s. "
                    + "Please either enable automatic consent or ensure that the terms are already accepted!", imageUri);
            LOGGER.info(warningMessage);
            throw new CloudPlatformValidationWarningException(warningMessage);
        } else {
            LOGGER.debug("Terms for %s are non readable but we proceed without warn assuming at least VHD would work.");
        }
    }

    private void handleTermsNotAccepted(String imageUri, boolean onlyMarketplaceImagesEnabled, boolean vhdAvailable) {
        if (onlyMarketplaceImagesEnabled && !vhdAvailable) {
            String errorMessage = String.format("Your image %s seems to be an Azure Marketplace image, "
                    + "however its Terms and Conditions are not accepted! "
                    + "Please either enable automatic consent or accept the terms manually and initiate the provisioning or upgrade again. " +
                    "On how to accept the Terms and Conditions of the image please refer to azure documentation " +
                    "at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.", imageUri);
            LOGGER.info(errorMessage);

            throw new CloudConnectorException(errorMessage);
        } else {
            String warningMessage = String.format("Your image %s seems to be an Azure Marketplace image, "
                    + "however its Terms and Conditions are not accepted! We will use VHD images for the deployment."
                    + "If you would like to use Marketplace images instead, please either enable automatic consent "
                    + "or accept the terms manually and initiate the provisioning or upgrade again. " +
                    "On how to accept the Terms and Conditions of the image please refer to azure documentation " +
                    "at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.", imageUri);
            LOGGER.info(warningMessage);
            throw new CloudPlatformValidationWarningException(warningMessage);
        }
    }

    private void validateMarketplacePermitted(String imageUri, boolean azureMarketplaceImagesEnabled) {
        checkEntitlement("Checking presence of Azure Marketplace entitlement for Marketplace image {}",
                imageUri,
                !azureMarketplaceImagesEnabled,
                "Your image %s seems to be an Azure Marketplace image! "
                        + "If you would like to use it please open Cloudera support ticket to enable this capability!");
        LOGGER.debug("Checking if Terms and Conditions for your Azure Marketplace image {} are accepted", imageUri);
    }

    private void validateVhdPermitted(String imageUri, boolean azureOnlyMarketplaceImagesEnabled) {
        checkEntitlement("Checking presence of 'Only Azure Marketplace Images Enabled' entitlement for VHD image {}",
                imageUri,
                azureOnlyMarketplaceImagesEnabled,
                "Your image %s seems to be a VHD image, but only Azure Marketplace images allowed in your account! ");
        LOGGER.debug("Image {} seems to be a valid VHD image", imageUri);
    }

    private void checkEntitlement(String logMessage, String imageUri, boolean entitlement, String error) {
        LOGGER.debug(logMessage, imageUri);
        if (entitlement) {
            String errorMessage = String.format(error + "If you would like to use it please open Cloudera support ticket to enable this capability!", imageUri);
            LOGGER.warn(errorMessage);
            throw new CloudConnectorException(errorMessage);
        }
    }

    private boolean isAutomaticTermsSignerAccepted(CloudStack stack) {
        return Boolean.parseBoolean(stack.getParameters().get(ACCEPTANCE_POLICY_PARAMETER));
    }

    public boolean isMarketplaceImageFormat(Image image) {
        return isMarketplaceImageFormat(image.getImageName());
    }

    public boolean hasSourceImagePlan(Image image) {
        String planUrn = image.getPackageVersions().get(ImagePackageVersion.SOURCE_IMAGE.getKey());
        if (StringUtils.isNotBlank(planUrn)) {
            if (isMarketplaceImageFormat(planUrn)) {
                LOGGER.debug("Plan with URN {} is a valid marketplace image plan", planUrn);
                return true;
            } else {
                handleUnknownFormat(planUrn);
            }
        }
        return false;
    }

    public boolean isMarketplaceImageFormat(String imageUri) {
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
        return isVhdImageFormat(image.getImageName());
    }

    public boolean isVhdImageFormat(String imageUri) {
        Matcher matcher = VHD_PATTERN.matcher(imageUri);
        if (matcher.matches()) {
            LOGGER.debug("Image with name {} is a valid VHD based image", imageUri);
            return true;
        } else {
            LOGGER.debug("Image with name {} is not a valid VHD based image", imageUri);
            return false;
        }
    }

    private AzureImageTermStatus getTermsStatus(AuthenticatedContext ac, AzureMarketplaceImage azureMarketplaceImage) {
        AzureClient azureClient = ac.getParameter(AzureClient.class);
        return azureImageTermsSignerService.getImageTermStatus(azureClient.getCurrentSubscription().subscriptionId(), azureMarketplaceImage, azureClient);
    }

    private void handleUnknownFormat(String imageUri) {
        String errorMessage = String.format("Your image name %s is invalid. Please check the desired format in the documentation!", imageUri);
        LOGGER.warn(errorMessage);
        throw new CloudConnectorException(errorMessage);
    }
}
