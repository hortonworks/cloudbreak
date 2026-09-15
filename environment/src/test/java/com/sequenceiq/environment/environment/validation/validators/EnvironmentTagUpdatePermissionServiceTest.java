package com.sequenceiq.environment.environment.validation.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.EnvironmentTagUpdatePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentTagUpdatePermissionMissingException;

@ExtendWith(MockitoExtension.class)
public class EnvironmentTagUpdatePermissionServiceTest {

    private static final String ENVIRONMENT_NAME = "env-name";

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private EnvironmentTagUpdatePermissionValidator awsValidator;

    @Mock
    private EnvironmentTagUpdatePermissionValidator azureValidator;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Test
    void validateDelegatesToAwsValidatorAndPassesWhenAllowed() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);
        Environment environment = awsEnvironment();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        doNothing().when(awsValidator).validate(cloudCredential);

        underTest.validate(environment);

        verify(awsValidator).validate(cloudCredential);
    }

    @Test
    void validateTranslatesPermissionExceptionToBadRequestWithFailedActionsPayload() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);
        Environment environment = awsEnvironment();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        List<String> failedActions = List.of("ec2:CreateTags : *", "kms:TagResource : *");
        TagUpdatePermissionMissingException cause = new TagUpdatePermissionMissingException(
                "missing action:ec2:CreateTags", failedActions, null);
        doThrow(cause).when(awsValidator).validate(cloudCredential);

        EnvironmentTagUpdatePermissionMissingException thrown = assertThrows(EnvironmentTagUpdatePermissionMissingException.class,
                () -> underTest.validate(environment));

        assertEquals("missing action:ec2:CreateTags", thrown.getMessage());
        assertSame(cause, thrown.getCause());
        assertEquals(failedActions, thrown.getFailedActions());
    }

    @Test
    void validateDelegatesToTheValidatorOfTheEnvironmentsPlatform() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        when(azureValidator.supportedPlatform()).thenReturn("AZURE");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator, azureValidator), credentialToCloudCredentialConverter);
        Environment environment = azureEnvironment();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);

        underTest.validate(environment);

        verify(azureValidator).validate(cloudCredential);
        verify(awsValidator, never()).validate(any());
    }

    @Test
    void validateIsNoOpWhenNoValidatorRegisteredForPlatform() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        when(azureValidator.supportedPlatform()).thenReturn("AZURE");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator, azureValidator), credentialToCloudCredentialConverter);
        Environment environment = environmentOnPlatform("MOCK");

        underTest.validate(environment);

        verifyNoInteractions(credentialToCloudCredentialConverter);
        verify(awsValidator, never()).validate(any());
        verify(azureValidator, never()).validate(any());
    }

    @Test
    void findMissingPermissionsReportsGrantedWhenTheValidatorPasses() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        doNothing().when(awsValidator).validate(cloudCredential);

        TagUpdatePermissionResult result = underTest.findMissingPermissions(awsEnvironment());

        assertFalse(result.hasError());
        assertTrue(result.verifiable());
        assertNull(result.message());
        assertEquals(List.of(), result.failedActions());
    }

    @Test
    void findMissingPermissionsReturnsTheFailedActionsInsteadOfThrowing() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        List<String> failedActions = List.of("ec2:CreateTags : *");
        doThrow(new TagUpdatePermissionMissingException("missing action:ec2:CreateTags", failedActions, null))
                .when(awsValidator).validate(cloudCredential);

        TagUpdatePermissionResult result = underTest.findMissingPermissions(awsEnvironment());

        assertTrue(result.hasError());
        assertTrue(result.verifiable());
        assertEquals("missing action:ec2:CreateTags", result.message());
        assertEquals(failedActions, result.failedActions());
    }

    @Test
    void findMissingPermissionsMarksAFailureWithoutNamedActionsAsUnverifiable() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        doThrow(new TagUpdatePermissionMissingException("could not simulate the policy", List.of(), null))
                .when(awsValidator).validate(cloudCredential);

        TagUpdatePermissionResult result = underTest.findMissingPermissions(awsEnvironment());

        assertTrue(result.hasError());
        assertFalse(result.verifiable(), "a check that could not run must not be reported as verified");
        assertEquals("could not simulate the policy", result.message());
    }

    @Test
    void findMissingPermissionsReportsGrantedWhenNoValidatorRegisteredForPlatform() {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);

        TagUpdatePermissionResult result = underTest.findMissingPermissions(environmentOnPlatform("MOCK"));

        assertFalse(result.hasError());
        verifyNoInteractions(credentialToCloudCredentialConverter);
    }

    @Test
    void validateAndFindMissingPermissionsAgreeOnTheSameEnvironment() throws Exception {
        when(awsValidator.supportedPlatform()).thenReturn("AWS");
        EnvironmentTagUpdatePermissionService underTest = new EnvironmentTagUpdatePermissionService(
                List.of(awsValidator), credentialToCloudCredentialConverter);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        List<String> failedActions = List.of("ec2:CreateTags : *");
        doThrow(new TagUpdatePermissionMissingException("missing action:ec2:CreateTags", failedActions, null))
                .when(awsValidator).validate(cloudCredential);

        TagUpdatePermissionResult probed = underTest.findMissingPermissions(awsEnvironment());
        EnvironmentTagUpdatePermissionMissingException thrown = assertThrows(EnvironmentTagUpdatePermissionMissingException.class,
                () -> underTest.validate(awsEnvironment()));

        assertEquals(probed.message(), thrown.getMessage());
        assertEquals(probed.failedActions(), thrown.getFailedActions());
    }

    private Environment awsEnvironment() {
        return environmentOnPlatform("AWS");
    }

    private Environment azureEnvironment() {
        return environmentOnPlatform("AZURE");
    }

    private Environment environmentOnPlatform(String cloudPlatform) {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        environment.setCloudPlatform(cloudPlatform);
        environment.setCredential(credential);
        return environment;
    }
}
