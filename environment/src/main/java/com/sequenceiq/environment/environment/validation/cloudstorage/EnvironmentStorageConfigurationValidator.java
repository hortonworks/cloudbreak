package com.sequenceiq.environment.environment.validation.cloudstorage;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;

public abstract class EnvironmentStorageConfigurationValidator {

    protected void validateGcsConfig(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder, String serviceAccountEmail) {
        if (StringUtils.isNotBlank(serviceAccountEmail)) {
            if (!serviceAccountEmail.contains(".iam.gserviceaccount.com")) {
                resultBuilder.error("Must be a full valid google service account in the format of " +
                        "[service-account-name]@[project-name].iam.gserviceaccount.com." +
                        getDocLink(environment.getCloudPlatform()));
            }
        } else {
            resultBuilder.error("Google Service Account must be specified in the requested Environment. " +
                    getDocLink(environment.getCloudPlatform()));
        }
    }

    protected void validateAdlsGen2Config(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder, String managedIdentity) {
        if (StringUtils.isNotBlank(managedIdentity)) {
            if (!managedIdentity.matches("^/subscriptions/[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}/"
                    + "(resourceGroups|resourcegroups)/[-\\w._()]+/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[A-Za-z0-9-_]*$")) {
                resultBuilder.error("Must be a full valid managed identity resource ID in the format of /subscriptions/[your-subscription-id]/resourceGroups/" +
                        "[your-resource-group]/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[name-of-your-identity]. " +
                        getDocLink(environment.getCloudPlatform()));
            }
        } else {
            resultBuilder.error("Managed Identity must be specified in the requested Environment. " +
                    getDocLink(environment.getCloudPlatform()));
        }
    }

    protected void validateS3Config(Environment environment, ValidationResult.ValidationResultBuilder resultBuilder, String instanceProfile) {
        if (StringUtils.isNotBlank(instanceProfile)) {
            if ((!instanceProfile.startsWith("arn:aws:iam::") && !instanceProfile.startsWith("arn:aws-us-gov:iam::"))
                    || !(instanceProfile.contains(":instance-profile/"))) {
                resultBuilder.error("Must be a full valid Amazon instance profile in the format of " +
                        "arn:(aws|aws-us-gov):iam::[account-id]:instance-profile/[role-name]." +
                        getDocLink(environment.getCloudPlatform()));
            }
        } else {
            resultBuilder.error("Instance Profile must be specified in the requested Environment. " +
                    getDocLink(environment.getCloudPlatform()));
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
