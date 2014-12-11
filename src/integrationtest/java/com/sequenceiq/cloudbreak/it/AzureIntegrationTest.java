package com.sequenceiq.cloudbreak.it;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.jayway.restassured.response.Response;

public class AzureIntegrationTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureIntegrationTest.class);

    @Value("${cb.it.azure.subscription.id}")
    private String subscriptionId;

    @Value("${cb.it.azure.jks.password}")
    private String jksPassword;

    @Value("${cb.it.azure.public.key}")
    private String publicKey;

    @Value("${cb.it.azure.credential.name:azure}")
    private String azureCredentialName;

    @Override protected void decorateModel() {
        getTestContext().put("subscriptionId", subscriptionId);
        getTestContext().put("jksPassword", jksPassword);
        getTestContext().put("publicKey", publicKey);

    }

    protected void createCredential() {
        Integer credentialId = null;
        Response response = IntegrationTestUtil.getRequest(getAccessToken()).get("user/credentials");
        List<Map<String, String>> credentials = response.jsonPath().get();

        for (Map credentialEntryMap : credentials) {
            if (azureCredentialName.equals(credentialEntryMap.get("name"))) {
                credentialId = (Integer) credentialEntryMap.get("id");
            }
        }
        LOGGER.info("credential id for {} is {}", azureCredentialName, credentialId);
        Assert.assertNotNull("Credential not found!", credentialId);

        getTestContext().put("credentialId", credentialId.toString());

    }

    @Test
    public void createAzureCluster() {
        super.integrationTestFlow();
    }

    @Override public void after() {
        // keep the credential!
        getTestContext().remove("credentialId");
        super.after();
    }

    @Override protected CloudProvider provider() {
        return CloudProvider.AZURE;
    }
}
