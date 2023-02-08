package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.common.model.CredentialType;

@ExtendWith(MockitoExtension.class)
public class AzureCredentialConnectorTest {

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
    private AzureCredentialAppCreationCommand appCreationCommand;

    @Mock
    private AzurePlatformParameters azurePlatformParameters;

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
}
