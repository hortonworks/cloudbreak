package com.sequenceiq.cloudbreak.cloud.openstack;

    import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClusterProxyService;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;

@ExtendWith(MockitoExtension.class)
class OpenStackAuthenticatorTest {

    private static final String ACCOUNT_ID = "account-123";

    private static final String CREDENTIAL_NAME = "my-openstack-cred";

    private static final String REMOTE_ENV_CRN = "crn:cdp:environments:us-west-1:account-123:environment:some-env-id";

    @Mock
    private OpenStackClient openStackClient;

    @Mock
    private OpenStackClusterProxyService clusterProxyService;

    @Mock
    private KeystoneCredentialView keystoneCredentialView;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private OpenStackAuthenticator underTest;

    @Test
    void authenticateShouldRegisterWhenRemoteEnvCrnSetAndNotRegistered() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = createCloudCredential();

        when(openStackClient.createKeystoneCredential(cloudCredential)).thenReturn(keystoneCredentialView);
        stubValidCredentialFields();
        when(keystoneCredentialView.getRemoteEnvironmentCrn()).thenReturn(REMOTE_ENV_CRN);
        when(keystoneCredentialView.getName()).thenReturn(CREDENTIAL_NAME);
        when(clusterProxyService.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME)).thenReturn(false);
        when(openStackClient.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);

        underTest.authenticate(cloudContext, cloudCredential);

        verify(clusterProxyService).registerServices(keystoneCredentialView, ACCOUNT_ID);
        verify(openStackClient).createAuthenticatedContext(cloudContext, cloudCredential);
    }

    @Test
    void authenticateShouldSkipRegistrationWhenAlreadyRegistered() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = createCloudCredential();

        when(openStackClient.createKeystoneCredential(cloudCredential)).thenReturn(keystoneCredentialView);
        stubValidCredentialFields();
        when(keystoneCredentialView.getRemoteEnvironmentCrn()).thenReturn(REMOTE_ENV_CRN);
        when(keystoneCredentialView.getName()).thenReturn(CREDENTIAL_NAME);
        when(clusterProxyService.isRegistered(ACCOUNT_ID, CREDENTIAL_NAME)).thenReturn(true);
        when(openStackClient.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);

        underTest.authenticate(cloudContext, cloudCredential);

        verify(clusterProxyService, never()).registerServices(any(), anyString());
        verify(openStackClient).createAuthenticatedContext(cloudContext, cloudCredential);
    }

    @Test
    void authenticateShouldSkipRegistrationWhenNoRemoteEnvCrn() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = createCloudCredential();

        when(openStackClient.createKeystoneCredential(cloudCredential)).thenReturn(keystoneCredentialView);
        stubValidCredentialFields();
        when(keystoneCredentialView.getRemoteEnvironmentCrn()).thenReturn(null);
        when(openStackClient.createAuthenticatedContext(cloudContext, cloudCredential)).thenReturn(authenticatedContext);

        underTest.authenticate(cloudContext, cloudCredential);

        verify(clusterProxyService, never()).isRegistered(anyString(), anyString());
        verify(clusterProxyService, never()).registerServices(any(), anyString());
    }

    @Test
    void authenticateShouldThrowWhenUsernameBlank() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = createCloudCredential();

        when(openStackClient.createKeystoneCredential(cloudCredential)).thenReturn(keystoneCredentialView);
        when(keystoneCredentialView.getUserName()).thenReturn("");

        CloudConnectorException ex = assertThrows(CloudConnectorException.class,
                () -> underTest.authenticate(cloudContext, cloudCredential));
        assertTrue(ex.getMessage().contains("username"));
    }

    @Test
    void authenticateShouldThrowWhenPasswordNull() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = createCloudCredential();

        when(openStackClient.createKeystoneCredential(cloudCredential)).thenReturn(keystoneCredentialView);
        when(keystoneCredentialView.getUserName()).thenReturn("admin");
        when(keystoneCredentialView.getPassword()).thenReturn(null);

        CloudConnectorException ex = assertThrows(CloudConnectorException.class,
                () -> underTest.authenticate(cloudContext, cloudCredential));
        assertTrue(ex.getMessage().contains("password"));
    }

    @Test
    void authenticateShouldThrowWhenEndpointBlank() {
        CloudContext cloudContext = createCloudContext();
        CloudCredential cloudCredential = createCloudCredential();

        when(openStackClient.createKeystoneCredential(cloudCredential)).thenReturn(keystoneCredentialView);
        when(keystoneCredentialView.getUserName()).thenReturn("admin");
        when(keystoneCredentialView.getPassword()).thenReturn("secret");
        when(keystoneCredentialView.getEndpoint()).thenReturn("");

        CloudConnectorException ex = assertThrows(CloudConnectorException.class,
                () -> underTest.authenticate(cloudContext, cloudCredential));
        assertTrue(ex.getMessage().contains("endpoint"));
    }

    private CloudContext createCloudContext() {
        return CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("OPENSTACK")
                .withVariant("OPENSTACK")
                .withAccountId(ACCOUNT_ID)
                .build();
    }

    private CloudCredential createCloudCredential() {
        return new CloudCredential("cred-id", CREDENTIAL_NAME, null, ACCOUNT_ID);
    }

    private void stubValidCredentialFields() {
        when(keystoneCredentialView.getUserName()).thenReturn("admin");
        when(keystoneCredentialView.getPassword()).thenReturn("secret");
        when(keystoneCredentialView.getEndpoint()).thenReturn("http://keystone:5000/v3");
    }
}
