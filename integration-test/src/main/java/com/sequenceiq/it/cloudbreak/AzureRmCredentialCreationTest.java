package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AppBased;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;

public class AzureRmCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
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
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName(credentialName);
        credentialRequest.setDescription("Azure Rm credential for integartiontest");
        AzureCredentialV4Parameters credentialParameters = new AzureCredentialV4Parameters();
        AppBased appBased = new AppBased();
        appBased.setAccessKey(accessKey);
        appBased.setSecretKey(secretKey);
        credentialParameters.setAppBased(appBased);
        credentialParameters.setSubscriptionId(subscriptionId);
        credentialParameters.setTenantId(tenantId);
        credentialRequest.setAzure(credentialParameters);
        credentialRequest.setCloudPlatform("AZURE_RM");
        Long id = getCloudbreakClient().credentialV4Endpoint().post(1L, credentialRequest).getId();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
