package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.microsoft.azure.Azure;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.rest.credentials.ServiceClientCredentials;

public class AzureDeleteVpcTest extends AbstractCloudbreakIntegrationTest {

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

    @AfterSuite
    @Parameters({ "resourceGroupName", "vpcName" })
    public void deleteNetwork(@Optional("it-vpc-resource-group") String resourceGroupName, @Optional("it-vpc") String vpcName) throws Exception {
        springTestContextPrepareTestInstance();
        ServiceClientCredentials serviceClientCredentials = new ApplicationTokenCredentials(defaultAccesKey, defaultTenantId, defaultSecretKey, null);
        Azure azure = Azure.authenticate(serviceClientCredentials).withSubscription(defaultSubscriptionId);

        azure.networks().delete(resourceGroupName, vpcName);
    }
}
