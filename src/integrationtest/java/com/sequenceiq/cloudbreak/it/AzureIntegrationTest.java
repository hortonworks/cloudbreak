package com.sequenceiq.cloudbreak.it;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.jayway.restassured.response.Response;
import com.sequenceiq.ambari.client.AmbariClient;

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
        getTestContext().put("credentialId", getAzureCredentialIdByName(azureCredentialName));
    }

    private String getAzureCredentialIdByName(String azureCredentialName) {
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
        return credentialId.toString();
    }

    @Test
    public void createAzureCluster() {

        // Create blueprint
        createResource(ResourceType.AZURE_BLUEPRINT, "blueprintId");

        // Create template
        createResource(ResourceType.AZURE_TEMPLATE, "templateId");

        // Create Stack
        createResource(ResourceType.STACK, "stackId");

        LOGGER.info("Waiting for the cluster to become available ...");
        waitForStackStatus("AVAILABLE", getTestContext().get("stackId"));

        // Do assertions
        LOGGER.info("Validating the cluster ...");
        Response stackResponse = getStack(getTestContext().get("stackId"));

        // retrieving ambari address
        String ambariIp = stackResponse.jsonPath().get("ambariServerIp");

        // connecting to ambari
        AmbariClient ambariClient = new AmbariClient(ambariIp);

        LOGGER.info("Checking the ambari server ...");
        Assert.assertEquals("The Ambari server is not running!", "RUNNING", ambariClient.healthCheck());

        LOGGER.info("Checking the number of nodes ...");
        Integer ambariNodes = ambariClient.getClusterHosts().size();
        Integer stackNodes = stackResponse.jsonPath().get("nodeCount");
        Assert.assertEquals("The number of cluster nodes in the stack differs from the number of nodes registered in ambari", ambariNodes, stackNodes);

        LOGGER.info("Checking service statuses ...");
        checkServiceStatuses(ambariClient.showServiceList());
    }

    @Override public void after() throws URISyntaxException {
        // keep the credential!
        getTestContext().remove("credentialId");
        super.after();
    }
}
