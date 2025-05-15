package com.sequenceiq.environment.environment.validation.cloudstorage;

import static com.sequenceiq.environment.environment.validation.validators.ManagedIdentityRoleValidator.USER_MANAGED_IDENTITY;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;

public abstract class EnvironmentStorageConfigurationValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentLogStorageConfigurationValidator.class);

    protected void validateGcsConfig(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder, String serviceAccountEmail) {
        if (StringUtils.isNotBlank(serviceAccountEmail)) {
            if (!serviceAccountEmail.contains(".iam.gserviceaccount.com")) {
                String error = "Must be a full valid Google Service Account in the format of " +
                        "[service-account-name]@[project-name].iam.gserviceaccount.com." +
                        getDocLink(environment.getCloudPlatform());
                LOGGER.debug(error);
                resultBuilder.error(error);
            }
        } else {
            String error = "Google Service Account must be specified in the requested Environment. " + getDocLink(environment.getCloudPlatform());
            LOGGER.debug(error);
            resultBuilder.error(error);
        }
    }

    protected void validateAdlsGen2Config(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder, String managedIdentity) {
        if (StringUtils.isNotBlank(managedIdentity)) {
            if (!managedIdentity.matches(USER_MANAGED_IDENTITY)) {
                String error = "Must be a full valid managed identity resource ID in the format of /subscriptions/[your-subscription-id]/resourceGroups/" +
                        "[your-resource-group]/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[name-of-your-identity]. " +
                        getDocLink(environment.getCloudPlatform());
                LOGGER.debug(error);
                resultBuilder.error(error);
            }
        } else {
            String error = "Managed Identity must be specified in the requested Environment. " + getDocLink(environment.getCloudPlatform());
            LOGGER.debug(error);
            resultBuilder.error(error);
        }
    }

    protected void validateS3Config(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder, String instanceProfile) {
        if (StringUtils.isNotBlank(instanceProfile)) {
            if (!instanceProfile.startsWith("arn:aws:iam::") && !instanceProfile.startsWith("arn:aws-us-gov:iam::")
                    || !instanceProfile.contains(":instance-profile/")) {
                String error = "Must be a full valid Amazon instance profile in the format of " +
                        "arn:(aws|aws-us-gov):iam::[account-id]:instance-profile/[role-name]." +
                        getDocLink(environment.getCloudPlatform());
                LOGGER.debug(error);
                resultBuilder.error(error);
            }
        } else {
            String error = "Instance Profile must be specified in the requested Environment. " + getDocLink(environment.getCloudPlatform());
            LOGGER.debug(error);
            resultBuilder.error(error);
        }
    }

    private String getDocLink(String cloudPlatform) {
        String docReferenceLink = " Refer to Cloudera documentation at %s for the required setup.";
        if (cloudPlatform.equals(CloudConstants.AWS)) {
            return String.format(docReferenceLink, DocumentationLinkProvider.awsCloudStorageSetupLink());
        } else if (cloudPlatform.equals(CloudConstants.AZURE)) {
            return String.format(docReferenceLink, DocumentationLinkProvider.azureCloudStorageSetupLink());
        } else if (cloudPlatform.equals(CloudConstants.GCP)) {
            return String.format(docReferenceLink, DocumentationLinkProvider.googleCloudStorageSetupLink());
        }
        return "";
    }
}
