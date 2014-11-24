package com.sequenceiq.cloudbreak.it;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import com.jayway.restassured.response.Response;
import com.sequenceiq.ambari.client.AmbariClient;

public class AwsIntegrationTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIntegrationTest.class);

    @Value("${cb.it.aws.public.key}")
    private String awsSshPublicKey;

    @Value("${cb.it.aws.rolearn}")
    private String roleArn;

    @Override protected void decorateModel() {
        getTestContext().put("roleArn", roleArn);
        getTestContext().put("sshPublicKey", awsSshPublicKey);
    }

    @Test
    public void createAwsCluster() {

        // Create credential
        createResource(ResourceType.AWS_CREDENTIAL, "credentialId");

        // Create blueprint
        createResource(ResourceType.AWS_BLUEPRINT, "blueprintId");

        // Create template
        createResource(ResourceType.AWS_TEMPLATE, "templateId");

        // Create Stack
        createResource(ResourceType.STACK, "stackId");

        // Create cluster
        LOGGER.info("Creating cluster for integration testing...");
        Response clusterCreationResponse = IntegrationTestUtil.createEntityRequest(getAccessToken(),
                getJsonMessage(ResourceType.CLUSTER, getTestContext()))
                .pathParam("stackId", getTestContext().get("stackId"))
                .post(ResourceType.CLUSTER.restResource());
        clusterCreationResponse.then().statusCode(HttpStatus.CREATED.value());

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

}
