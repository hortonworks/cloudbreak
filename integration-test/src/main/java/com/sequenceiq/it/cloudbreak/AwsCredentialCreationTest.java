package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.it.util.ResourceUtil;

public class AwsCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.awscredential.name}")
    private String defaultName;
    @Value("${integrationtest.awscredential.roleArn}")
    private String defaultRoleArn;
    @Value("${integrationtest.awscredential.publicKeyFile}")
    private String defaultPublicKeyFile;

    @Test
    @Parameters({ "credentialName", "roleArn", "publicKeyFile" })
    public void testAwsCredentialCreation(@Optional("") String credentialName, @Optional("") String roleArn,
            @Optional("") String publicKeyFile) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        roleArn = StringUtils.hasLength(roleArn) ? roleArn : defaultRoleArn;
        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        // TODO publicInAccount
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName);
        credentialRequest.setPublicKey(publicKey);
        credentialRequest.setDescription("Aws Rm credential for integartiontest");
        Map<String, Object> map = new HashMap<>();
        map.put("roleArn", roleArn);
        credentialRequest.setParameters(map);
        credentialRequest.setCloudPlatform("AWS");
        // WHEN
        // TODO publicInAccount
        String id = getCredentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
