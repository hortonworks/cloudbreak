package com.sequenceiq.cloudbreak.controller.validation;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.Validator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.CloudPlatformValidationWarningException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.environment.marketplace.AzureMarketplaceTermsClientService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class RecommendedImageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendedImageValidator.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private AzureMarketplaceTermsClientService azureMarketplaceTermsClientService;

    @Inject
    private RecommendImageService recommendImageService;

    public ValidationResult validateRecommendedImage(Long workspaceId, CloudbreakUser cloudbreakUser, ImageSettingsV4Request imageSettings,
            String environmentCrn, String region, String platform, String blueprintName) {
        if (!CloudPlatform.AZURE.name().equals(platform)) {
            LOGGER.debug("Platform is not Azure. Nothing to validate.");
            return new ValidationResult();
        }
        CloudPlatformVariant cloudPlatform = new CloudPlatformVariant(
                Platform.platform(platform.toUpperCase()),
                Variant.variant(platform.toUpperCase()));
        Image image = recommendImageService.recommendImage(workspaceId, cloudbreakUser, imageSettings, region, platform, blueprintName, cloudPlatform);
        LOGGER.debug("Recommended image to validate: {}", image);

        Boolean accepted = azureMarketplaceTermsClientService.getAccepted(environmentCrn);
        LOGGER.debug("Azure Marketplace automatic terms acceptance policy: {}", accepted);
        Map<String, String> parameters = Map.of(ACCEPTANCE_POLICY_PARAMETER, accepted.toString());
        CloudStack cloudStack = new CloudStack(List.of(), null, image, parameters, Map.of(), null, null, null, null, null, null, null);

        CloudConnector connector = cloudPlatformConnectors.get(cloudPlatform);
        AuthenticatedContext ac = getAuthenticatedContext(workspaceId, environmentCrn, cloudPlatform, connector);
        try {
            for (Validator validator : connector.validators(ValidatorType.IMAGE)) {
                validator.validate(ac, cloudStack);
            }
            LOGGER.debug("Validation finished successfully");
            return new ValidationResult();
        } catch (CloudConnectorException e) {
            ValidationResult result = new ValidationResult();
            result.setErrorMsg(e.getMessage());
            LOGGER.debug("Validation finished with an error: {}", e.getMessage());
            return result;
        } catch (CloudPlatformValidationWarningException e) {
            ValidationResult result = new ValidationResult();
            result.setWarningMsg(e.getMessage());
            LOGGER.debug("Validation finished with a warning: {}", e.getMessage());
            return result;
        }
    }

    private AuthenticatedContext getAuthenticatedContext(Long workspaceId, String environmentCrn, CloudPlatformVariant cloudPlatform, CloudConnector connector) {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withPlatform(cloudPlatform.getPlatform())
                .withVariant(cloudPlatform.getVariant())
                .withWorkspaceId(workspaceId)
                .build();

        CloudCredential cloudCredential = stackUtil.getCloudCredential(environmentCrn);
        return connector.authentication().authenticate(cloudContext, cloudCredential);
    }

    public static class ValidationResult {
        private String errorMsg;

        private String warningMsg;

        public String getErrorMsg() {
            return errorMsg;
        }

        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }

        public String getWarningMsg() {
            return warningMsg;
        }

        public void setWarningMsg(String warningMsg) {
            this.warningMsg = warningMsg;
        }
    }
}
