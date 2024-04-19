package com.sequenceiq.datalake.service.validation.cloudstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.BackupOperationType;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateRequest;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.datalake.service.validation.converter.CredentialResponseToCloudCredentialConverter;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
public class CloudStorageValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1:user:2";

    private static final String CUSTOM_BACKUP_LOCATION = "/location/to/custom/backup";

    private static final String BACKUP_LOCATION = "/location/to/backup";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private CredentialResponseToCloudCredentialConverter credentialResponseToCloudCredentialConverter;

    @Mock
    private CloudProviderServicesV4Endopint cloudProviderServicesV4Endpoint;

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
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialResponseToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc"));
        when(entitlementService.cloudStorageValidationEnabled(any())).thenReturn(true);
        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(new ObjectStorageValidateResponse());
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(new CloudStorageRequest(), environment, validationResultBuilder));
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateBackupLocation() {
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(environment.getBackupLocation()).thenReturn(BACKUP_LOCATION);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialResponseToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc"));

        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(new ObjectStorageValidateResponse());
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateBackupLocation(new CloudStorageRequest(), BackupOperationType.ANY,
                environment, null, "7.2.16", validationResultBuilder));
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateCustomBackupLocation() {
        ArgumentCaptor<ObjectStorageValidateRequest> captor = ArgumentCaptor.forClass(ObjectStorageValidateRequest.class);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialResponseToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc"));

        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(new ObjectStorageValidateResponse());
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateBackupLocation(new CloudStorageRequest(), BackupOperationType.ANY, environment,
                CUSTOM_BACKUP_LOCATION, "7.2.16", validationResultBuilder));
        verify(cloudProviderServicesV4Endpoint, times(1)).validateObjectStorage(captor.capture());
        assertEquals(CUSTOM_BACKUP_LOCATION, captor.getValue().getBackupLocationBase());
        assertFalse(validationResultBuilder.build().hasError());
    }

    @Test
    public void validateBackupLocationOnError() {
        ArgumentCaptor<ObjectStorageValidateRequest> captor = ArgumentCaptor.forClass(ObjectStorageValidateRequest.class);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(environment.getCloudPlatform()).thenReturn("AWS");
        when(environment.getBackupLocation()).thenReturn(BACKUP_LOCATION);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialResponseToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc"));

        ObjectStorageValidateResponse objectStorageValidateResponse = new ObjectStorageValidateResponse();
        objectStorageValidateResponse.setStatus(ResponseStatus.ERROR);
        objectStorageValidateResponse.setError("dummy failure");
        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(objectStorageValidateResponse);
        ValidationResultBuilder validationResultBuilderforFailure = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateBackupLocation(new CloudStorageRequest(), BackupOperationType.ANY, environment, null,
                "7.2.16", validationResultBuilderforFailure));
        verify(cloudProviderServicesV4Endpoint, times(1)).validateObjectStorage(captor.capture());
        assertTrue(captor.getValue().getCloudStorageRequest().getLocations().isEmpty());
        assertEquals("id", captor.getValue().getCredential().getId());
        assertEquals("acc", captor.getValue().getCredential().getAccountId());
        assertEquals(BACKUP_LOCATION, captor.getValue().getBackupLocationBase());
        assertEquals("AWS", captor.getValue().getCloudPlatform());
        assertTrue(validationResultBuilderforFailure.build().hasError());
        assertEquals("dummy failure", validationResultBuilderforFailure.build().getFormattedErrors());
    }

    @Test
    public void validateCustomBackupLocationOnError() {
        ArgumentCaptor<ObjectStorageValidateRequest> captor = ArgumentCaptor.forClass(ObjectStorageValidateRequest.class);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(environment.getCloudPlatform()).thenReturn("AWS");
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialResponseToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc"));

        ObjectStorageValidateResponse objectStorageValidateResponse = new ObjectStorageValidateResponse();
        objectStorageValidateResponse.setStatus(ResponseStatus.ERROR);
        objectStorageValidateResponse.setError("dummy failure");
        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(objectStorageValidateResponse);
        ValidationResultBuilder validationResultBuilderforFailure = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateBackupLocation(new CloudStorageRequest(), BackupOperationType.ANY,
                environment, CUSTOM_BACKUP_LOCATION, "7.2.16", validationResultBuilderforFailure));
        verify(cloudProviderServicesV4Endpoint, times(1)).validateObjectStorage(captor.capture());
        assertTrue(captor.getValue().getBackupOperationType().name().equals(BackupOperationType.ANY.name()));
        assertTrue(captor.getValue().getCloudStorageRequest().getLocations().isEmpty());
        assertEquals("id", captor.getValue().getCredential().getId());
        assertEquals("acc", captor.getValue().getCredential().getAccountId());
        assertEquals(CUSTOM_BACKUP_LOCATION, captor.getValue().getBackupLocationBase());
        assertEquals("AWS", captor.getValue().getCloudPlatform());
        assertTrue(validationResultBuilderforFailure.build().hasError());
        assertEquals("dummy failure", validationResultBuilderforFailure.build().getFormattedErrors());
    }

    static Stream<Arguments> parameterScenarios() {
        return Stream.of(
                Arguments.of("7.2.15", false),
                Arguments.of("7.2.16", false),
                Arguments.of("7.2.17", true),
                Arguments.of("7.2.18", true)
        );
    }

    @ParameterizedTest(name = "runtime = {0}, skipLogRoleValidationforBackup = {1}")
    @MethodSource("parameterScenarios")
    public void validateSkipLogRoleValidationforBackup(String runtime, boolean skipLogRoleValidationforBackup) {
        ObjectStorageValidateResponse objectStorageValidateResponse = mock(ObjectStorageValidateResponse.class);
        when(objectStorageValidateResponse.getStatus()).thenReturn(ResponseStatus.OK);
        when(environment.getCredential()).thenReturn(new CredentialResponse());
        when(environment.getBackupLocation()).thenReturn(BACKUP_LOCATION);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(credentialResponseToCloudCredentialConverter.convert(any())).thenReturn(
                new CloudCredential("id", "name", Map.of("secretKey", "thisshouldnotappearinlog"), "acc"));

        when(cloudProviderServicesV4Endpoint.validateObjectStorage(any())).thenReturn(objectStorageValidateResponse);
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateBackupLocation(new CloudStorageRequest(), BackupOperationType.ANY,
                environment, null, runtime, validationResultBuilder));

        ArgumentCaptor<ObjectStorageValidateRequest> requestArgumentCaptor = ArgumentCaptor.forClass(ObjectStorageValidateRequest.class);
        verify(cloudProviderServicesV4Endpoint).validateObjectStorage(requestArgumentCaptor.capture());
        ObjectStorageValidateRequest request = requestArgumentCaptor.getValue();
        assertFalse(validationResultBuilder.build().hasError());
        assertEquals(skipLogRoleValidationforBackup, request.getSkipLogRoleValidationforBackup());
    }
}
