package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.util.ResourceUtil;

public class AzureRmCredentialCreationTest extends AbstractCloudbreakIntegrationTest {
    @Value("${integrationtest.azurermcredential.name}")
    private String defaultName;
    @Value("${integrationtest.azurermcredential.subscriptionId}")
    private String defaultSubscriptionId;
    @Value("${integrationtest.azurermcredential.secretKey}")
    private String defaultSecretKey;
    @Value("${integrationtest.azurermcredential.accesKey}")
    private String defaultAccesKey;
    @Value("${integrationtest.azurermcredential.tenantId}")
    private String defaultTenantId;
    @Value("${integrationtest.azurermcredential.publicKeyFile}")
    private String defaultPublicKeyFile;

    @Test
    @Parameters({ "credentialName", "subscriptionId", "secretKey", "accesKey", "tenantId", "publicKeyFile" })
    public void testAzureTemplateCreation(@Optional("itazurermcreden") String credentialName, @Optional("") String subscriptionId,
            @Optional("") String secretKey, @Optional("") String accesKey, @Optional("") String tenantId,
            @Optional("") String publicKeyFile) throws Exception {
        // GIVEN
        credentialName = StringUtils.hasLength(credentialName) ? credentialName : defaultName;
        subscriptionId = StringUtils.hasLength(subscriptionId) ? subscriptionId : defaultSubscriptionId;
        secretKey = StringUtils.hasLength(secretKey) ? secretKey : defaultSecretKey;
        tenantId = StringUtils.hasLength(tenantId) ? tenantId : defaultTenantId;
        accesKey = StringUtils.hasLength(accesKey) ? accesKey : defaultAccesKey;

        publicKeyFile = StringUtils.hasLength(publicKeyFile) ? publicKeyFile : defaultPublicKeyFile;
        String publicKey = ResourceUtil.readStringFromResource(applicationContext, publicKeyFile).replaceAll("\n", "");
        // WHEN
        // TODO publicInAccount
        String id = getClient().postAzureRmCredential(credentialName, "Azure Rm credential for integartiontest", subscriptionId, tenantId, accesKey, secretKey,
                publicKey, false);
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, id, true);
    }
}
