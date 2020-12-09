package com.sequenceiq.datalake.service.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.assertj.core.util.Strings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class CloudStorageValidatorTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static String userCrn;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DetailedEnvironmentResponse environment;

    @InjectMocks
    private CloudStorageValidator underTest;

    @BeforeClass
    public static void setUp() {
        userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (Strings.isNullOrEmpty(userCrn)) {
            ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
            userCrn = USER_CRN;
        }
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationDisabled() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.DISABLED);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(cloudStorageRequest, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationNoEntitlement() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        when(entitlementService.cloudStorageValidationEnabled(any())).thenReturn(false);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(cloudStorageRequest, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateEnvironmentRequestCloudStorageValidationMissingEntitlement() {
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        when(environment.getCloudStorageValidation()).thenReturn(CloudStorageValidation.ENABLED);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        underTest.validate(cloudStorageRequest, environment, validationResultBuilder);
        assertFalse(validationResultBuilder.build().hasError());
    }
}
