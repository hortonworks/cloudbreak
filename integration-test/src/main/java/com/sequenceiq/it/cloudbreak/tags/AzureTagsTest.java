package com.sequenceiq.it.cloudbreak.tags;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;


public class AzureTagsTest extends AbstractTagTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.tags.AzureTagsTest.class);

    @Value("${integrationtest.azurermcredential.subscriptionId}")
    private String defaultSubscriptionId;

    @Value("${integrationtest.azurermcredential.secretKey}")
    private String defaultSecretKey;

    @Value("${integrationtest.azurermcredential.accessKey}")
    private String defaultAccesKey;

    @Value("${integrationtest.azurermcredential.tenantId}")
    private String defaultTenantId;

    @Parameters({"subscriptionId", "secretKey", "accesKey", "tenantId"})
    @BeforeMethod
    public void checkAzureTags(@Optional ("") String subscriptionId, @Optional ("") String secretKey, @Optional ("") String accesKey,
            @Optional ("") String tenantId) throws Exception {
        subscriptionId = StringUtils.hasLength(subscriptionId) ? subscriptionId : defaultSubscriptionId;
        secretKey = StringUtils.hasLength(secretKey) ? secretKey : defaultSecretKey;
        accesKey = StringUtils.hasLength(accesKey) ? accesKey : defaultAccesKey;
        tenantId = StringUtils.hasLength(tenantId) ? tenantId : defaultTenantId;

        Map<String, String> cpd = getCloudProviderParams();
        cpd.put("cloudProvider", "AZURE");
        cpd.put("subscriptionId", subscriptionId);
        cpd.put("secretKey", secretKey);
        cpd.put("accesKey", accesKey);
        cpd.put("tenantId", tenantId);
    }
}
