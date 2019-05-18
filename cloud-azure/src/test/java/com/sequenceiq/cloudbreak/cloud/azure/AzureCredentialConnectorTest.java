package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.cloud.azure.client.CBRefreshTokenClientProvider;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialSender;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

public class AzureCredentialConnectorTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String DEPLOYMENT_ADDRESS = "https://mydeployment.com";

    private static final String PLATFORM = "AWS";

    private static final CloudContext TEST_CLOUD_CONTEXT = new CloudContext(1L, "test", PLATFORM, USER_ID, WORKSPACE_ID);

    @InjectMocks
    private AzureCredentialConnector underTest;

    @Mock
    private AzureInteractiveLogin azureInteractiveLogin;

    @Mock
    private CredentialSender credentialSender;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private AzureCredentialAppCreationCommand appCreationCommand;

    @Mock
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPrerequisitesReturnsTheExpectedValue() {
        String expected = "someAppCreationCommandValue";
        when(appCreationCommand.generate(anyString())).thenReturn(expected);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(TEST_CLOUD_CONTEXT, "2", DEPLOYMENT_ADDRESS);

        assertEquals(PLATFORM, result.getCloudPlatform());
        assertEquals(expected, new String(Base64.decodeBase64(result.getAzure().getAppCreationCommand())));
    }

    @Test
    public void testGetPrerequisitesAwsIsNotImplemented() {
        String expected = "someAppCreationCommandValue";
        when(appCreationCommand.generate(anyString())).thenReturn(expected);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(TEST_CLOUD_CONTEXT, "2", DEPLOYMENT_ADDRESS);

        assertNull(result.getAws());
    }

    @Test
    public void testInteractiveLoginIsEnabled() {
        when(azureInteractiveLogin.login(any(CloudContext.class), any(ExtendedCloudCredential.class),
                any(CredentialNotifier.class))).thenReturn(Maps.newHashMap());
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(null, null, null,
                null, null, USER_ID, WORKSPACE_ID);
        underTest.interactiveLogin(TEST_CLOUD_CONTEXT, extendedCloudCredential, credentialSender);
        verify(azureInteractiveLogin, times(1)).login(any(CloudContext.class), any(ExtendedCloudCredential.class),
                any(CredentialNotifier.class));
    }

    @Test
    public void testInitCodeGrantFlow() {
        String accessKey = "someAccessKey";
        String tenantId = "1";
        String secretKey = "someExtremelySecretKey";
        String redirectUrl = "some.url.com";
        when(cloudCredential.getParameter("accessKey", String.class)).thenReturn(accessKey);
        when(cloudCredential.getParameter("tenantId", String.class)).thenReturn(tenantId);
        when(cloudCredential.getParameter("secretKey", String.class)).thenReturn(secretKey);
        when(cloudCredential.getParameter("deploymentAddress", String.class)).thenReturn(DEPLOYMENT_ADDRESS);
        when(appCreationCommand.getRedirectURL(String.valueOf(WORKSPACE_ID), DEPLOYMENT_ADDRESS)).thenReturn(redirectUrl);

        Map<String, String> result = underTest.initCodeGrantFlow(TEST_CLOUD_CONTEXT, cloudCredential);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("appLoginUrl"));
        assertTrue(result.containsKey("appReplyUrl"));
        assertTrue(result.containsKey("codeGrantFlowState"));
        assertTrue(result.get("appLoginUrl").contains(result.get("codeGrantFlowState")));
        assertEquals(redirectUrl, result.get("appReplyUrl"));
        verify(appCreationCommand, times(1)).getRedirectURL(String.valueOf(WORKSPACE_ID), DEPLOYMENT_ADDRESS);
    }

}
