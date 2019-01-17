package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.KeystoneV2Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class OpenStackCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.openstackcredential.name}")
    private String defaultName;

    @Value("${integrationtest.openstackcredential.tenantName}")
    private String defaultTenantName;

    @Value("${integrationtest.openstackcredential.userName}")
    private String defaultUserName;

    @Value("${integrationtest.openstackcredential.password}")
    private String defaultPassword;

    @Value("${integrationtest.openstackcredential.endpoint}")
    private String defaultEndpoint;

    @Test
    @Parameters({ "credentialName", "tenantName", "userName", "password", "endpoint" })
    public void testOpenStackCredentialCreation(@Optional("")String credentialName, @Optional("")String tenantName, @Optional("")String userName,
            @Optional("")String password, @Optional("")String endpoint) {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        tenantName = StringUtils.hasLength(tenantName) ? tenantName : defaultTenantName;
        userName = StringUtils.hasLength(userName) ? userName : defaultUserName;
        password = StringUtils.hasLength(password) ? password : defaultPassword;
        endpoint = StringUtils.hasLength(endpoint) ? endpoint : defaultEndpoint;

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Aws Rm credential for integartiontest");

        OpenstackCredentialV4Parameters credentialParameters = new OpenstackCredentialV4Parameters();
        credentialParameters.setEndpoint(endpoint);
        credentialParameters.setUserName(userName);
        credentialParameters.setPassword(password);
        KeystoneV2Parameters keystoneV2Parameters = new KeystoneV2Parameters();
        keystoneV2Parameters.setTenantName(tenantName);
        credentialParameters.setKeystoneV2Parameters(keystoneV2Parameters);

        credentialRequest.setOpenstack(credentialParameters);
        credentialRequest.setCloudPlatform("OPENSTACK");
        // WHEN
        Long id = getCloudbreakClient().credentialV4Endpoint().post(1L, credentialRequest).getId();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
        getItContext().putContextParam(CloudbreakV2Constants.CREDENTIAL_NAME, credentialName);
    }
}
