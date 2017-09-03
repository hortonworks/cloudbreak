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

public class AwsCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.awscredential.name}")
    private String defaultName;

    @Value("${integrationtest.awscredential.roleArn:}")
    private String defaultRoleArn;

    @Value("${integrationtest.awscredential.accessKey:}")
    private String defaultAccessKey;

    @Value("${integrationtest.awscredential.secretKey:}")
    private String defaultSecretKey;

    @Test
    @Parameters({ "credentialName", "roleArn", "accessKey", "secretKey" })
    public void testAwsCredentialCreation(@Optional("") String credentialName, @Optional("") String roleArn,
            @Optional("") String accessKey, @Optional("") String secretKey) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        roleArn = StringUtils.hasLength(roleArn) ? roleArn : defaultRoleArn;
        accessKey = StringUtils.hasLength(accessKey) ? accessKey : defaultAccessKey;
        secretKey = StringUtils.hasLength(secretKey) ? secretKey : defaultSecretKey;
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Aws credential for integrationtest");
        Map<String, Object> map = new HashMap<>();
        if (roleArn != null && !"".equals(roleArn)) {
            map.put("selector", "role-based");
            map.put("roleArn", roleArn);
        } else {
            map.put("selector", "key-based");
            map.put("accessKey", accessKey);
            map.put("secretKey", secretKey);
        }
        credentialRequest.setParameters(map);
        credentialRequest.setCloudPlatform("AWS");
        // WHEN
        String id = getCloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
