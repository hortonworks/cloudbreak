package com.sequenceiq.it.cloudbreak.azure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;

public class AzureCredentialCreationTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.azurermcredential.name}")
    private String defaultName;

    @Value("${integrationtest.azurermcredential.subscriptionId}")
    private String defaultSubscriptionId;

    @Value("${integrationtest.azurermcredential.secretKey}")
    private String defaultSecretKey;

    @Value("${integrationtest.azurermcredential.accessKey}")
    private String defaultAccesKey;

    @Value("${integrationtest.azurermcredential.tenantId}")
    private String defaultTenantId;

    @Test
    @Parameters({ "credentialName", "subscriptionId", "secretKey", "accessKey", "tenantId" })
    public void testAzureRMCredentialCreation(@Optional("itazurermcreden") String credentialName, @Optional("") String subscriptionId,
            @Optional("") String secretKey, @Optional("") String accessKey, @Optional("") String tenantId) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        subscriptionId = StringUtils.hasLength(subscriptionId) ? subscriptionId : defaultSubscriptionId;
        secretKey = StringUtils.hasLength(secretKey) ? secretKey : defaultSecretKey;
        tenantId = StringUtils.hasLength(tenantId) ? tenantId : defaultTenantId;
        accessKey = StringUtils.hasLength(accessKey) ? accessKey : defaultAccesKey;

        // WHEN
        // TODO publicInAccount
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Azure credential for integartion test");
        Map<String, Object> map = new HashMap<>();
        map.put("subscriptionId", subscriptionId);
        map.put("tenantId", tenantId);
        map.put("accessKey", accessKey);
        map.put("secretKey", secretKey);
        credentialRequest.setParameters(map);
        credentialRequest.setCloudPlatform("AZURE");
        String id = getCloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
