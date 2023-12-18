package com.sequenceiq.cloudbreak.cloud.azure.image;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureDeploymentMarketplaceError.MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureImageTermsSignerService;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;

@Service
public class AzureMarketplaceValidatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceValidatorService.class);

    private static final String MISSING_WHAT_IF_PERMISSION_ERROR = "Insufficient permission to perform what if analysis, " +
            "please ensure Microsoft.Resources/deployments/whatIf/action is granted!";

    @Inject
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    private AzureImageTermsSignerService azureImageTermsSignerService;

    @Inject
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    public MarketplaceValidationResult validateMarketplaceImage(Image image, PrepareImageType prepareType, String imageFallbackTarget,
            AzureClient client, CloudStack stack, AuthenticatedContext ac) {
        if (azureImageFormatValidator.isMarketplaceImageFormat(image)) {
            AzureMarketplaceImage azureMarketplaceImage = azureMarketplaceImageProviderService.get(image);
            AzureClient azureClient = ac.getParameter(AzureClient.class);
            if (prepareType == PrepareImageType.EXECUTED_DURING_PROVISIONING) {
                return skipValidation(image, "called during provisioning flow and not image change.");
            } else if (entitlementService.azureOnlyMarketplaceImagesEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
                return skipValidation(image, "CDP_AZURE_IMAGE_MARKETPLACE_ONLY entitlement is granted, VHD usage is prohibited.");
            } else if (termsAccepted(azureMarketplaceImage, azureClient)) {
                return skipValidation(image, "as current image terms are already accepted, there is no need to perform validation.");
            } else if (Boolean.parseBoolean(stack.getParameters().get(ACCEPTANCE_POLICY_PARAMETER))) {
                azureImageTermsSignerService.sign(azureClient.getCurrentSubscription().subscriptionId(), azureMarketplaceImage, azureClient);
            }
            String marketplaceEligibilityError = performValidation(client, stack, ac);
            if (StringUtils.isNotBlank(marketplaceEligibilityError)) {
                return handleValidationErrors(imageFallbackTarget, marketplaceEligibilityError);
            } else {
                LOGGER.debug("Marketplace image seems to be usable, no need to copy the VHD");
                return new MarketplaceValidationResult(false, true);
            }
        } else {
            LOGGER.debug("Image {} does not have Marketplace format, nothing to do here.", image.getImageName());
            return new MarketplaceValidationResult(false, false);
        }
    }

    private String performValidation(AzureClient client, CloudStack stack, AuthenticatedContext ac) {
        try {
            Optional<ManagementError> error = azureTemplateDeploymentService.runWhatIfAnalysis(client, stack, ac);
            return error.map(this::getMarketplacePurchaseEligibilityFailedError).orElse(null);
        } catch (ManagementException e) {
            // we need to skip errors related missing whatIf permission seamlessly
            if (azureExceptionHandler.isForbidden(e)) {
                String errorMessage = String.format(MISSING_WHAT_IF_PERMISSION_ERROR + " Message: %s", e.getMessage());
                LOGGER.info(errorMessage);
                return errorMessage;
            }
            throw e;
        }
    }

    private MarketplaceValidationResult handleValidationErrors(String imageFallbackTarget, String marketplaceEligibilityError) {
        LOGGER.info("Proceeding with image copy for image {} as it will be needed eventually", imageFallbackTarget);
        return new MarketplaceValidationResult(true, marketplaceEligibilityError);
    }

    private MarketplaceValidationResult skipValidation(Image image, String message) {
        LOGGER.info("Skipping image copy as target image ({}) is an Azure Marketplace image and {}",
                image.getImageName(), message);
        return new MarketplaceValidationResult(false, true);
    }

    private String getMarketplacePurchaseEligibilityFailedError(ManagementError managementError) {
        return managementError.getCode() != null && managementError.getCode().equals(MARKETPLACE_PURCHASE_ELIGIBILITY_FAILED.getCode()) ?
                collectErrors(managementError) :
                null;
    }

    private String collectErrors(ManagementError managementError) {
        if (CollectionUtils.isNotEmpty(managementError.getDetails())) {
            return String.join(". ", managementError.getDetails().stream().map(ManagementError::getMessage).collect(Collectors.toSet()));
        } else {
            return managementError.getMessage();
        }
    }

    private boolean termsAccepted(AzureMarketplaceImage azureMarketplaceImage, AzureClient azureClient) {
        return azureImageTermsSignerService.getImageTermStatus(azureClient.getCurrentSubscription().subscriptionId(), azureMarketplaceImage, azureClient)
                .getAsBoolean();
    }
}
