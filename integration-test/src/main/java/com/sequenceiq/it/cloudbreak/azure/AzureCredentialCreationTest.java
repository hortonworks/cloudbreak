package com.sequenceiq.it.cloudbreak.azure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AppBased;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
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
            @Optional("") String secretKey, @Optional("") String accessKey, @Optional("") String tenantId) {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        subscriptionId = StringUtils.hasLength(subscriptionId) ? subscriptionId : defaultSubscriptionId;
        secretKey = StringUtils.hasLength(secretKey) ? secretKey : defaultSecretKey;
        tenantId = StringUtils.hasLength(tenantId) ? tenantId : defaultTenantId;
        accessKey = StringUtils.hasLength(accessKey) ? accessKey : defaultAccesKey;

        // WHEN
        var credentialRequest = new CredentialV4Request();
        var credentialParameters = new AzureCredentialV4Parameters();
        var appBased = new AppBased();
        appBased.setAccessKey(accessKey);
        appBased.setSecretKey(secretKey);
        credentialRequest.setDescription("Azure credential for integartion test");
        credentialRequest.setName(credentialName);
        credentialRequest.setAzure(credentialParameters);
        credentialRequest.setCloudPlatform("AZURE");
        credentialParameters.setAppBased(appBased);
        credentialParameters.setTenantId(tenantId);
        credentialParameters.setSubscriptionId(subscriptionId);
        Long id = getCloudbreakClient().credentialV4Endpoint().post(1L, credentialRequest).getId();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
