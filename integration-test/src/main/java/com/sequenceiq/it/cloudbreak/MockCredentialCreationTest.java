package com.sequenceiq.it.cloudbreak;

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
import com.sequenceiq.it.util.ResourceUtil;

public class MockCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.mockcredential.name}")
    private String defaultName;
    @Value("${integrationtest.mockcredential.publicKeyFile}")
    private String defaultPublicKeyFile;

    @Test
    @Parameters({ "credentialName", "publicKeyFile" })
    public void testMockCredentialCreation(@Optional("") String credentialName, @Optional("") String publicKeyFile) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName + UUID.randomUUID());
        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        credentialRequest.setPublicKey(publicKey);
        credentialRequest.setDescription("Mock Rm credential for integrationtest");
        Map<String, Object> map = new HashMap<>();
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");

        credentialRequest.setParameters(map);
        credentialRequest.setCloudPlatform("MOCK");
        // WHEN
        // TODO publicInAccount
        String id = getCloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
