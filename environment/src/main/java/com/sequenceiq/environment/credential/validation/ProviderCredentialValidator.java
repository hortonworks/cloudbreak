package com.sequenceiq.environment.credential.validation;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;

public interface ProviderCredentialValidator {

    String supportedProvider();

    ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResult.ValidationResultBuilder resultBuilder);
}
