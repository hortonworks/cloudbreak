package com.sequenceiq.datalake.service.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.datalake.service.validation.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class CloudStorageValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1:user:2";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private SecretService secretService;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private CloudStorageValidator underTest;

    @Test
    public void validateEnvironmentRequestCloudStorageValidationDisabled() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.DISABLED);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(new CloudStorageRequest(), environment, validationResultBuilder));
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationNoEntitlement() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        when(entitlementService.cloudStorageValidationEnabled(any())).thenReturn(false);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(new CloudStorageRequest(), environment, validationResultBuilder));
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationMissingEntitlement() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.validate(new CloudStorageRequest(), environment, validationResultBuilder)));
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidation() {
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(secretService.getByResponse(any())).thenReturn("secret");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc", false));
        when(entitlementService.cloudStorageValidationEnabled(any())).thenReturn(true);
        when(cloudProviderServicesV4Endopint.validateObjectStorage(any())).thenReturn(new ObjectStorageValidateResponse());
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(new CloudStorageRequest(), environment, validationResultBuilder));
        assertFalse(validationResultBuilder.build().hasError());
    }
}
