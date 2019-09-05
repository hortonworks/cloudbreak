package com.sequenceiq.it.cloudbreak.mock;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class MockCredentialV3CreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.mockcredential.name}")
    private String defaultName;

    @Test
    @Parameters("credentialName")
    public void testMockCredentialCreation(@Optional("") String credentialName) {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        credentialName += UUID.randomUUID();
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Mock Rm credential for integrationtest");
        credentialRequest.setCloudPlatform("MOCK");
        // WHEN
        Long workspaceId = getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String id = getCloudbreakClient().credentialV3Endpoint().createInWorkspace(workspaceId, credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
        getItContext().putContextParam(CloudbreakV2Constants.CREDENTIAL_NAME, credentialName);
    }
}

