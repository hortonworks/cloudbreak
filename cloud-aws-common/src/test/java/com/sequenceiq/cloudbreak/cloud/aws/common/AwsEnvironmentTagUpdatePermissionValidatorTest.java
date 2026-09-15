package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.exception.AwsPermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.aws.common.validator.AwsEnvironmentTagUpdatePermissionValidator;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.iam.model.IamException;
import software.amazon.awssdk.services.sts.model.StsException;

@ExtendWith(MockitoExtension.class)
public class AwsEnvironmentTagUpdatePermissionValidatorTest {

    private static final String PUBLIC_POLICY = "public-base64-policy";

    private static final String GOV_POLICY = "gov-base64-policy";

    @Mock
    private AwsPlatformParameters awsPlatformParameters;

    @Mock
    private AwsCredentialVerifier awsCredentialVerifier;

    @Mock
    private AwsCredentialViewProvider credentialViewProvider;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AwsCredentialView awsCredentialView;

    @InjectMocks
    private AwsEnvironmentTagUpdatePermissionValidator underTest;

    @Test
    void validatePassesWhenVerifierReturnsNormally() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(false);
        doNothing().when(awsCredentialVerifier).validateAws(eq(awsCredentialView), eq(PUBLIC_POLICY));

        underTest.validate(cloudCredential);

        verify(awsCredentialVerifier).validateAws(awsCredentialView, PUBLIC_POLICY);
    }

    @Test
    void validateUsesGovPolicyForGovCloudCredential() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(true);
        doNothing().when(awsCredentialVerifier).validateAws(eq(awsCredentialView), eq(GOV_POLICY));

        underTest.validate(cloudCredential);

        verify(awsCredentialVerifier).validateAws(awsCredentialView, GOV_POLICY);
    }

    @Test
    void validateThrowsWhenVerifierReportsMissingPermission() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(false);
        List<String> failedActions = List.of("ec2:CreateTags : *", "kms:TagResource : *");
        AwsPermissionMissingException cause = new AwsPermissionMissingException("missing action:ec2:CreateTags", failedActions);
        doThrow(cause).when(awsCredentialVerifier).validateAws(any(AwsCredentialView.class), eq(PUBLIC_POLICY));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals("missing action:ec2:CreateTags", thrown.getMessage());
        assertSame(cause, thrown.getCause());
        assertEquals(failedActions, thrown.getFailedActions());
    }

    @Test
    void validateReportsSimulatePrincipalPolicyAsMissingWhenTheSimulationIsDenied() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(false);
        when(awsCredentialView.getName()).thenReturn("cred-1");
        IamException cause = (IamException) IamException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("not authorized to perform: iam:SimulatePrincipalPolicy")
                        .build())
                .message("not authorized to perform: iam:SimulatePrincipalPolicy")
                .build();
        doThrow(cause).when(awsCredentialVerifier).validateAws(any(AwsCredentialView.class), eq(PUBLIC_POLICY));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of("iam:SimulatePrincipalPolicy : *"), thrown.getFailedActions());
        assertSame(cause, thrown.getCause());
        assertTrue(thrown.getMessage().contains("cred-1"), "the message must name the credential");
        assertTrue(thrown.getMessage().contains("iam:SimulatePrincipalPolicy"), "the message must name the missing prerequisite");
    }

    @Test
    void validateWrapsStsAccessDeniedFromCallerIdentityAsAValidationError() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(false);
        when(awsCredentialView.getName()).thenReturn("cred-1");
        StsException cause = (StsException) StsException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("not authorized to perform: sts:GetCallerIdentity")
                        .build())
                .message("not authorized to perform: sts:GetCallerIdentity")
                .build();
        doThrow(cause).when(awsCredentialVerifier).validateAws(any(AwsCredentialView.class), eq(PUBLIC_POLICY));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertSame(cause, thrown.getCause());
        assertEquals(List.of("iam:SimulatePrincipalPolicy : *"), thrown.getFailedActions());
    }

    @Test
    void validateWrapsNonAccessDeniedIamErrorsWithoutClaimingAnActionIsMissing() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(false);
        when(awsCredentialView.getName()).thenReturn("cred-1");
        IamException cause = (IamException) IamException.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("Throttling").errorMessage("Rate exceeded").build())
                .message("Rate exceeded")
                .build();
        doThrow(cause).when(awsCredentialVerifier).validateAws(any(AwsCredentialView.class), eq(PUBLIC_POLICY));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertSame(cause, thrown.getCause());
        assertTrue(thrown.getFailedActions().isEmpty(), "a throttling error must not be reported as a missing action");
        assertTrue(thrown.getMessage().contains("Rate exceeded"));
    }

    @Test
    void validateWrapsIamExceptionWithoutErrorDetails() throws Exception {
        stubCommonMocks();
        when(awsCredentialView.isGovernmentCloudEnabled()).thenReturn(false);
        when(awsCredentialView.getName()).thenReturn("cred-1");
        IamException cause = (IamException) IamException.builder().message("no error details at all").build();
        doThrow(cause).when(awsCredentialVerifier).validateAws(any(AwsCredentialView.class), eq(PUBLIC_POLICY));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertSame(cause, thrown.getCause());
        assertTrue(thrown.getFailedActions().isEmpty());
    }

    private void stubCommonMocks() {
        when(credentialViewProvider.createAwsCredentialView(cloudCredential)).thenReturn(awsCredentialView);
        when(awsPlatformParameters.getCdpTagUpdatePolicyJson()).thenReturn(Map.of(
                PolicyType.PUBLIC, PUBLIC_POLICY,
                PolicyType.GOV, GOV_POLICY));
    }

    @Test
    void tagUpdatePolicyJsonIsPresentOnClasspath() {
        assertNotNull(getClass().getResource("/definitions/cdp/aws-cdp-tag-update-policy.json"),
                "AWS tag-update policy JSON must exist on the classpath");
    }

    @Test
    void supportedPlatformIsAws() {
        assertEquals("AWS", underTest.supportedPlatform());
    }
}
