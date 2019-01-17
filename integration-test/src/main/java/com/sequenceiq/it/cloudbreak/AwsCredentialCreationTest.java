package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.RoleBasedCredentialParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

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
            @Optional("") String accessKey, @Optional("") String secretKey) {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        roleArn = StringUtils.hasLength(roleArn) ? roleArn : defaultRoleArn;
        accessKey = StringUtils.hasLength(accessKey) ? accessKey : defaultAccessKey;
        secretKey = StringUtils.hasLength(secretKey) ? secretKey : defaultSecretKey;
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Aws credential for integrationtest");

        AwsCredentialV4Parameters credentialParameters = new AwsCredentialV4Parameters();
        if (roleArn != null && !roleArn.isEmpty()) {
            RoleBasedCredentialParameters roleBasedCredentialParameters = new RoleBasedCredentialParameters();
            roleBasedCredentialParameters.setRoleArn(roleArn);
            credentialParameters.setRoleBasedCredentialParameters(roleBasedCredentialParameters);
        } else {
            KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
            keyBasedCredentialParameters.setAccessKey(accessKey);
            keyBasedCredentialParameters.setSecretKey(secretKey);
            credentialParameters.setKeyBasedCredentialParameters(keyBasedCredentialParameters);
        }

        credentialRequest.setAws(credentialParameters);
        credentialRequest.setCloudPlatform("AWS");
        // WHEN
        Long id = getCloudbreakClient().credentialV4Endpoint().post(1L, credentialRequest).getId();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
