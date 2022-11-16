package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.azure.client.AuthenticationContextProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.CBRefreshTokenClientProvider;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialNotifier;
import com.sequenceiq.cloudbreak.cloud.credential.CredentialSender;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.common.model.CredentialType;

public class AzureCredentialConnectorTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String DEPLOYMENT_ADDRESS = "https://mydeployment.com";

    private static final String PLATFORM = "AWS";

    private static final CloudContext TEST_CLOUD_CONTEXT = CloudContext.Builder.builder()
            .withId(1L)
            .withName("test")
            .withCrn("crn")
            .withPlatform(PLATFORM)
            .withWorkspaceId(WORKSPACE_ID)
            .build();

    @InjectMocks
    private AzureCredentialConnector underTest;

    @Mock
    private AzureInteractiveLogin azureInteractiveLogin;

    @Mock
    private AzureCredentialAppCreationCommand appCreationCommand;

    @Mock
    private AuthenticationContextProvider authenticationContextProvider;

    @Mock
    private CBRefreshTokenClientProvider cbRefreshTokenClientProvider;

    @Mock
    private AzurePlatformParameters azurePlatformParameters;

    @Mock
    private CredentialSender credentialSender;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetPrerequisitesReturnsTheExpectedValue() {
        String expectedCommand = "someAppCreationCommandValue";
        String expectedRoleDef = "roleDefJson";
        when(appCreationCommand.generateEnvironmentCredentialCommand(anyString())).thenReturn(expectedCommand);
        when(azurePlatformParameters.getRoleDefJson()).thenReturn(expectedRoleDef);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(TEST_CLOUD_CONTEXT, "2", "3", DEPLOYMENT_ADDRESS,
                CredentialType.ENVIRONMENT);

        assertEquals(PLATFORM, result.getCloudPlatform());
        assertEquals(expectedCommand, new String(Base64.decodeBase64(result.getAzure().getAppCreationCommand())));
        assertEquals(expectedRoleDef, result.getAzure().getRoleDefitionJson());
    }

    @Test
    public void testGetPrerequisitesOnlyAzureIsImplemented() {
        String expected = "someAppCreationCommandValue";
        when(appCreationCommand.generateEnvironmentCredentialCommand(anyString())).thenReturn(expected);

        CredentialPrerequisitesResponse result = underTest.getPrerequisites(TEST_CLOUD_CONTEXT, "2", "3", DEPLOYMENT_ADDRESS,
                CredentialType.ENVIRONMENT);

        assertNull(result.getAws());
        assertNull(result.getGcp());
        assertNotNull(result.getAzure());
    }

    @Test
    public void testInteractiveLoginIsEnabled() {
        when(azureInteractiveLogin.login(any(CloudContext.class), any(ExtendedCloudCredential.class),
                any(CredentialNotifier.class))).thenReturn(Maps.newHashMap());
        CloudCredential cloudCredential = new CloudCredential("anId", "aName", "account");
        ExtendedCloudCredential extendedCloudCredential = new ExtendedCloudCredential(cloudCredential, null,
                null, USER_ID, "accountId", new ArrayList<>());
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

        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> azureParams = new HashMap<>();
        Map<String, String> codeGrantFlowParams = new HashMap<>();
        codeGrantFlowParams.put("accessKey", accessKey);
        codeGrantFlowParams.put("tenantId", tenantId);
        codeGrantFlowParams.put("secretKey", secretKey);
        codeGrantFlowParams.put("deploymentAddress", DEPLOYMENT_ADDRESS);
        azureParams.put("codeGrantFlowBased", codeGrantFlowParams);
        parameters.put("azure", azureParams);

        CloudCredential cloudCredential = new CloudCredential("anId", "aName", parameters, "acc", false);
        when(appCreationCommand.getRedirectURL(DEPLOYMENT_ADDRESS)).thenReturn(redirectUrl);

        Map<String, String> result = underTest.initCodeGrantFlow(TEST_CLOUD_CONTEXT, cloudCredential);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.containsKey("appLoginUrl"));
        assertTrue(result.containsKey("appReplyUrl"));
        assertTrue(result.containsKey("codeGrantFlowState"));
        assertTrue(result.get("appLoginUrl").contains(result.get("codeGrantFlowState")));
        assertEquals(redirectUrl, result.get("appReplyUrl"));
        verify(appCreationCommand, times(1)).getRedirectURL(DEPLOYMENT_ADDRESS);
    }

}
