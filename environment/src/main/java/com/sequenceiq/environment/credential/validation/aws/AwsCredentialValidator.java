package com.sequenceiq.environment.credential.validation.aws;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.ProviderCredentialValidator;

@Component
public class AwsCredentialValidator implements ProviderCredentialValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialValidator.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    public String supportedProvider() {
        return CloudPlatform.AWS.name();
    }

    @Override
    public ValidationResult validateCreate(CredentialRequest credentialRequest, ValidationResultBuilder resultBuilder) {
        return resultBuilder.build();
    }

    @Override
    public ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
        validateKeyBasedRoleBasedChange(original, newCred, resultBuilder);
        validateDefaultRegionUpdate(newCred, resultBuilder);
        return resultBuilder.build();
    }

    private void validateKeyBasedRoleBasedChange(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
        try {
            JsonNode originalKeyBased = JsonUtil.readTree(original.getAttributes()).get("aws").get("keyBased");
            JsonNode originalRoleBased = JsonUtil.readTree(original.getAttributes()).get("aws").get("roleBased");
            JsonNode newKeyBased = JsonUtil.readTree(newCred.getAttributes()).get("aws").get("keyBased");
            JsonNode newRoleBased = JsonUtil.readTree(newCred.getAttributes()).get("aws").get("roleBased");
            resultBuilder.ifError(() -> isNotNull(originalKeyBased) && isNotNull(newRoleBased),
                    "Cannot change AWS credential from key based to role based.");
            resultBuilder.ifError(() -> isNotNull(originalRoleBased) && isNotNull(newKeyBased),
                    "Cannot change AWS credential from role based to key based.");
        } catch (IOException ioe) {
            throw new IllegalStateException("Unexpected error during JSON parsing.", ioe);
        } catch (NullPointerException npe) {
            resultBuilder.error("Missing attributes from the JSON!");
            LOGGER.warn("Missing attributes from the JSON!", npe);
        }
    }

    private void validateDefaultRegionUpdate(Credential newCred, ValidationResultBuilder resultBuilder) {
        CredentialAttributes credentialAttributes = JsonUtil.readValueUnchecked(newCred.getAttributes(), CredentialAttributes.class);
        Optional.ofNullable(credentialAttributes)
                .map(CredentialAttributes::getAws)
                .map(AwsCredentialAttributes::getDefaultRegion)
                .ifPresent(defaultRegion -> {
                    CloudRegions cdpRegions = cloudParameterService.getCdpRegions(CloudPlatform.AWS.name(), AwsConstants.AWS_DEFAULT_VARIANT.value());
                    if (!cdpRegions.getRegionNames().contains(defaultRegion)) {
                        resultBuilder.error("The specified default region '" + defaultRegion + "' is not supported on AWS by CDP."
                                + "Please provide a valid region name!");
                    }
                });
    }

    private boolean isNotNull(JsonNode jsonNode) {
        return jsonNode != null && !jsonNode.isNull();
    }
}
