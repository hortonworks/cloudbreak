package com.sequenceiq.environment.environment.validation.validators;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.util.DocumentationLinkProvider;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@Component
public class ManagedIdentityRoleValidator {

    public static final String USER_MANAGED_IDENTITY = "^/subscriptions/[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}/"
            + "(resourceGroups|resourcegroups)/[-\\w._()]+/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[A-Za-z0-9-_]*$";

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedIdentityRoleValidator.class);

    public ValidationResult validateEncryptionRole(String encryptionRole) {
        ValidationResult.ValidationResultBuilder validationResultBuilder = ValidationResult.builder();

        if (encryptionRole != null) {
            String error = null;
            if (StringUtils.isBlank(encryptionRole)) {
                error = "If specified, the managed identity resource ID may not be empty or whitespace only." + getDocLink();
            } else if (!encryptionRole.matches(USER_MANAGED_IDENTITY)) {
                error = "Must be a full valid managed identity resource ID in the format of /subscriptions/[your-subscription-id]/resourceGroups/" +
                        "[your-resource-group]/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[name-of-your-identity]." +
                        getDocLink();
            }
            if (error != null) {
                LOGGER.debug(error);
                validationResultBuilder.error(error);
            }
        }
        return validationResultBuilder.build();
    }

    private String getDocLink() {
        String docReferenceLink = " Refer to Cloudera documentation at %s for the required setup.";
        return String.format(docReferenceLink, DocumentationLinkProvider.azureCloudStorageSetupLink());
    }

}