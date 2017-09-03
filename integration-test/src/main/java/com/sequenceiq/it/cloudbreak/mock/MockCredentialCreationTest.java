package com.sequenceiq.it.cloudbreak.mock;

import java.util.HashMap;
import java.util.Map;
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

public class MockCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.mockcredential.name}")
    private String defaultName;

    @Test
    @Parameters({ "credentialName" })
    public void testMockCredentialCreation(@Optional("") String credentialName) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName + UUID.randomUUID());
        credentialRequest.setDescription("Mock Rm credential for integrationtest");
        Map<String, Object> map = new HashMap<>();
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");

        credentialRequest.setParameters(map);
        credentialRequest.setCloudPlatform("MOCK");
        // WHEN
        String id = getCloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
