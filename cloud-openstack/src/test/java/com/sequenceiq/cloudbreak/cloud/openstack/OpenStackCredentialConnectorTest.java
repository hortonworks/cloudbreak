package com.sequenceiq.cloudbreak.cloud.openstack;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus;
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClient;
import com.sequenceiq.cloudbreak.cloud.openstack.client.OpenStackClusterProxyService;
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

@ExtendWith(MockitoExtension.class)
class OpenStackCredentialConnectorTest {

    private static final String ACCOUNT_ID = "account-123";

    private static final String CREDENTIAL_NAME = "my-openstack-cred";

    private static final String REMOTE_ENV_CRN = "crn:cdp:environments:us-west-1:account-123:environment:some-env-id";

    @Mock
    private OpenStackClient openStackClient;

    @Mock
    private OpenStackClusterProxyService clusterProxyService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private KeystoneCredentialView keystoneCredentialView;

    @InjectMocks
    private OpenStackCredentialConnector underTest;

    @Test
    void verifyShouldReturnVerified() {
        CloudCredential cloudCredential = new CloudCredential("id", CREDENTIAL_NAME, null, ACCOUNT_ID);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);

        CloudCredentialStatus result = underTest.verify(authenticatedContext, null);

        assertEquals(CredentialStatus.VERIFIED, result.getStatus());
    }

    @Test
    void deleteShouldDeregisterWhenRemoteEnvCrnSet() {
        CloudCredential cloudCredential = new CloudCredential("id", CREDENTIAL_NAME, null, ACCOUNT_ID);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getAccountId()).thenReturn(ACCOUNT_ID);
        when(openStackClient.createKeystoneCredential(authenticatedContext)).thenReturn(keystoneCredentialView);
        when(keystoneCredentialView.getRemoteEnvironmentCrn()).thenReturn(REMOTE_ENV_CRN);
        when(keystoneCredentialView.getName()).thenReturn(CREDENTIAL_NAME);

        CloudCredentialStatus result = underTest.delete(authenticatedContext);

        assertEquals(CredentialStatus.DELETED, result.getStatus());
        verify(clusterProxyService).deregisterServices(ACCOUNT_ID, CREDENTIAL_NAME);
    }

    @Test
    void deleteShouldNotDeregisterWhenNoRemoteEnvCrn() {
        CloudCredential cloudCredential = new CloudCredential("id", CREDENTIAL_NAME, null, ACCOUNT_ID);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(openStackClient.createKeystoneCredential(authenticatedContext)).thenReturn(keystoneCredentialView);
        when(keystoneCredentialView.getRemoteEnvironmentCrn()).thenReturn(null);

        CloudCredentialStatus result = underTest.delete(authenticatedContext);

        assertEquals(CredentialStatus.DELETED, result.getStatus());
        verify(clusterProxyService, never()).deregisterServices(anyString(), anyString());
    }

    @Test
    void deleteShouldReturnDeletedWhenDeregistrationFails() {
        CloudCredential cloudCredential = new CloudCredential("id", CREDENTIAL_NAME, null, ACCOUNT_ID);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getAccountId()).thenReturn(ACCOUNT_ID);
        when(openStackClient.createKeystoneCredential(authenticatedContext)).thenReturn(keystoneCredentialView);
        when(keystoneCredentialView.getRemoteEnvironmentCrn()).thenReturn(REMOTE_ENV_CRN);
        when(keystoneCredentialView.getName()).thenReturn(CREDENTIAL_NAME);
        org.mockito.Mockito.doThrow(new ClusterProxyException("connection failed", new RuntimeException()))
                .when(clusterProxyService).deregisterServices(ACCOUNT_ID, CREDENTIAL_NAME);

        CloudCredentialStatus result = underTest.delete(authenticatedContext);

        assertEquals(CredentialStatus.DELETED, result.getStatus());
    }
}
