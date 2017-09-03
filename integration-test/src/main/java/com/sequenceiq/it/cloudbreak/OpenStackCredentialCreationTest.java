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
            @Optional("")String password, @Optional("")String endpoint) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        tenantName = StringUtils.hasLength(tenantName) ? tenantName : defaultTenantName;
        userName = StringUtils.hasLength(userName) ? userName : defaultUserName;
        password = StringUtils.hasLength(password) ? password : defaultPassword;
        endpoint = StringUtils.hasLength(endpoint) ? endpoint : defaultEndpoint;

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Aws Rm credential for integartiontest");
        Map<String, Object> map = new HashMap<>();
        map.put("tenantName", tenantName);
        map.put("userName", userName);
        map.put("password", password);
        map.put("endpoint", endpoint);
        map.put("keystoneVersion", "cb-keystone-v2");
        map.put("selector", "cb-keystone-v2");

        credentialRequest.setParameters(map);
        credentialRequest.setCloudPlatform("OPENSTACK");
        // WHEN
        String id = getCloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
