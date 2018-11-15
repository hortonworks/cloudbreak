package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialSender;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class AzureCredentialConnectorTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private AzureCredentialConnector underTest;

    @Mock
    private AzureInteractiveLogin azureInteractiveLogin;

    @Mock
    private CredentialSender credentialSender;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInteractiveLoginIsEnabled() {
        when(azureInteractiveLogin.login(any(CloudContext.class), any(ExtendedCloudCredential.class),
                any(CredentialNotifier.class))).thenReturn(Maps.newHashMap());
        CloudContext cloudContext = new CloudContext(1L, "test", "test", USER_ID, WORKSPACE_ID);
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(null, null, null,
                null, null, USER_ID, WORKSPACE_ID);
        underTest.interactiveLogin(cloudContext, extendedCloudCredential, credentialSender);
        verify(azureInteractiveLogin, times(1)).login(any(CloudContext.class), any(ExtendedCloudCredential.class),
                any(CredentialNotifier.class));
    }
}
