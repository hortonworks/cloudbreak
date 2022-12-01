package com.sequenceiq.environment.credential.validation;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.domain.Credential;

public interface ProviderCredentialValidator {

    String supportedProvider();

    ValidationResult validateCreate(CredentialRequest credentialRequest, ValidationResult.ValidationResultBuilder resultBuilder);

    ValidationResult validateUpdate(Credential original, Credential newCred, ValidationResult.ValidationResultBuilder resultBuilder);
}
