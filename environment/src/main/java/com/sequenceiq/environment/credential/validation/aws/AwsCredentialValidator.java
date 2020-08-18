package com.sequenceiq.environment.credential.validation.aws;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.ProviderCredentialValidator;

@Component
public class AwsCredentialValidator implements ProviderCredentialValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCredentialValidator.class);

    @Override
    public String supportedProvider() {
        return CloudPlatform.AWS.name();
    }

    @Override
    public ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
        return validateKeyBasedRoleBasedChange(original, newCred, resultBuilder);
    }

    private ValidationResult validateKeyBasedRoleBasedChange(Credential original, Credential newCred, ValidationResultBuilder resultBuilder) {
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
        return resultBuilder.build();
    }

    private boolean isNotNull(JsonNode jsonNode) {
        return jsonNode != null && !jsonNode.isNull();
    }
}
