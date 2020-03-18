package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.SdkBaseException;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialViewProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;

public class AwsCredentialConnectorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AwsSessionCredentialClient credentialClient;

    @Mock
    private AwsCredentialVerifier awsCredentialVerifier;

    @Mock
    private CloudCredential credential;

    @Mock
    private AwsCredentialViewProvider awsCredentialViewProvider;

    @Mock
    private AwsCredentialView credentialView;

    @InjectMocks
    private AwsCredentialConnector underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(credential.isVerifyPermissions()).thenReturn(true);
        when(authenticatedContext.getCloudCredential()).thenReturn(credential);
        when(awsCredentialViewProvider.createAwsCredentialView(credential)).thenReturn(credentialView);
    }

    @Test
    public void testInteractiveLoginIsProhibitedOnAws() {
        expectedException.expect(UnsupportedOperationException.class);
        underTest.interactiveLogin(null, null, null);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationThrowsSdkBaseExceptionThenFailedStatusShouldReturn() throws AwsPermissionMissingException {
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        String exceptionMessageComesFromSdk = "SomethingTerribleHappened!";
        Exception sdkException = new SdkBaseException(exceptionMessageComesFromSdk);

        doThrow(sdkException).when(awsCredentialVerifier).validateAws(credentialView);
        CloudCredentialStatus result = underTest.verify(authenticatedContext);

        assertNotNull(result);
        assertEquals(CredentialStatus.FAILED, result.getStatus());
        assertEquals(exceptionMessageComesFromSdk, result.getStatusReason());
        assertEquals(sdkException, result.getException());

        verify(awsCredentialVerifier, times(1)).validateAws(any());
        verify(awsCredentialVerifier, times(1)).validateAws(credentialView);
    }

    @Test
    public void testVerifyIfRoleBasedCredentialVerificationGoesFineThenVerifiedStatusShouldReturn() throws AwsPermissionMissingException {
        String roleArn = "someRoleArn";
        when(credentialView.getRoleArn()).thenReturn(roleArn);

        CloudCredentialStatus result = underTest.verify(authenticatedContext);

        assertNotNull(result);
        assertEquals(CredentialStatus.VERIFIED, result.getStatus());
        assertNull(result.getException());

        verify(awsCredentialVerifier, times(1)).validateAws(any());
        verify(awsCredentialVerifier, times(1)).validateAws(credentialView);
    }

}
