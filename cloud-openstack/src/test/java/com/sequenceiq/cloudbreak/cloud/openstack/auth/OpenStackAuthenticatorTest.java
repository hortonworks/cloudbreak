package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEYSTONE_V3;
import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEYSTONE_V3_DEFAULT_SCOPE;
import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEYSTONE_V3_PROJECT_SCOPE;
import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEY_KEYSTONE_AUTH_SCOPE;
import static com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView.CB_KEY_KEYSTONE_VERSION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

/**
 * Created by gpapp on 6/8/18.
 */

@RunWith(MockitoJUnitRunner.class)
public class OpenStackAuthenticatorTest {

    @Mock
    private OpenStackClient openStackClient;

    @InjectMocks
    private OpenStackAuthenticator underTest = new OpenStackAuthenticator();

    private final CloudContext cloudContext = new CloudContext(1L, "", "", "");

    private final CloudCredential cloudCredential = new CloudCredential(1L, "credName");

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = CloudConnectorException.class)
    public void testKeystoneV3WithDefaultScopeAndVerification() {
        cloudCredential.putParameter(CB_KEY_KEYSTONE_VERSION, CB_KEYSTONE_V3);
        cloudCredential.putParameter(CB_KEY_KEYSTONE_AUTH_SCOPE, CB_KEYSTONE_V3_DEFAULT_SCOPE);
        doCallRealMethod().when(openStackClient).createKeystoneCredential(cloudCredential);

        try {
            underTest.authenticate(cloudContext, cloudCredential, true);

        } catch (CloudConnectorException e) {
            assertEquals("Creation of Openstack Keystone credentials with default scope is not supported", e.getMessage());
            verify(openStackClient, never()).createAuthenticatedContext(cloudContext, cloudCredential);
            throw e;
        }
    }

    @Test
    public void testKeystoneV3WithDefaultScopeAndNotVerification() {
        cloudCredential.putParameter(CB_KEY_KEYSTONE_VERSION, CB_KEYSTONE_V3);
        cloudCredential.putParameter(CB_KEY_KEYSTONE_AUTH_SCOPE, CB_KEYSTONE_V3_DEFAULT_SCOPE);
        doCallRealMethod().when(openStackClient).createKeystoneCredential(cloudCredential);

        underTest.authenticate(cloudContext, cloudCredential, false);

        verify(openStackClient).createAuthenticatedContext(cloudContext, cloudCredential);
    }

    @Test
    public void testOpenStackV3WithProjectScopeAndVerification() {
        cloudCredential.putParameter(CB_KEY_KEYSTONE_VERSION, CB_KEYSTONE_V3);
        cloudCredential.putParameter(CB_KEY_KEYSTONE_AUTH_SCOPE, CB_KEYSTONE_V3_PROJECT_SCOPE);
        doCallRealMethod().when(openStackClient).createKeystoneCredential(cloudCredential);

        underTest.authenticate(cloudContext, cloudCredential, true);

        verify(openStackClient).createAuthenticatedContext(cloudContext, cloudCredential);
    }

    @Test
    public void testOpenStackV3WithProjectScopeAndNotVerification() {
        cloudCredential.putParameter(CB_KEY_KEYSTONE_VERSION, CB_KEYSTONE_V3);
        cloudCredential.putParameter(CB_KEY_KEYSTONE_AUTH_SCOPE, CB_KEYSTONE_V3_PROJECT_SCOPE);
        doCallRealMethod().when(openStackClient).createKeystoneCredential(cloudCredential);

        underTest.authenticate(cloudContext, cloudCredential, false);

        verify(openStackClient).createAuthenticatedContext(cloudContext, cloudCredential);
    }
}
