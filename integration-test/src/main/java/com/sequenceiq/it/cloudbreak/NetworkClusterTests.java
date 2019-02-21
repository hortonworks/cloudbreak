package com.sequenceiq.it.cloudbreak;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import com.sequenceiq.it.cloudbreak.newway.cloud.OpenstackCloudProvider;

public class NetworkClusterTests extends CloudbreakTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkClusterTests.class);

    private CloudProvider cloudProvider;

    public NetworkClusterTests() {
    }

    public NetworkClusterTests(CloudProvider cp, TestParameter tp) {
        cloudProvider = cp;
        setTestParameter(tp);
    }

    @BeforeTest(alwaysRun = true)
    @Parameters("provider")
    public void beforeTest(@Optional(OpenstackCloudProvider.OPENSTACK) String provider) {
        LOGGER.info("before cluster test set provider: " + provider);
        if (cloudProvider == null) {
            cloudProvider = CloudProviderHelper.providerFactory(provider, getTestParameter());
        } else {
            LOGGER.info("cloud provider already set - running from factory test");
        }
    }

    @AfterTest(alwaysRun = true, dependsOnMethods = { "cleanUpClusters" })
    public void cleanUpCredentials() throws Exception {
        LOGGER.info("Delete credential: {}", cloudProvider.getCredentialName());
        try {
            given(CloudbreakClient.created());
            given(cloudProvider.aValidCredential(), cloudProvider.getCredentialName() + " credential is created.");
            when(Credential.delete());
        } catch (ForbiddenException | NotFoundException e) {
            logCredentialCleanupFailure(e);
        }
    }

    @AfterTest(alwaysRun = true)
    public void cleanUpClusters() throws Exception {
        String clusterName = cloudProvider.getClusterName();

        LOGGER.info("Delete cluster: {}", clusterName);
        try {
            given(CloudbreakClient.created());
            given(cloudProvider.aValidCredential(), cloudProvider.getCredentialName() + " credential is created.");
            given(Stack.request().withName(clusterName), clusterName + " stack is created.");
            given(cloudProvider.aValidStackCreated());
            when(Stack.delete());
            then(Stack.waitAndCheckClusterDeleted(), clusterName + " stack has been deleted.");
        } catch (ForbiddenException | NotFoundException e) {
            LOGGER.info("Terminate cluster exception message ::: " + e.getMessage());
        }
    }

    @Test(groups = { "aws", "gcp", "openstack" })
    public void testCreateNewCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request().withName(cloudProvider.getClusterName()).withAmbariRequest(cloudProvider
                        .ambariRequestWithBlueprintName(cloudProvider.getBlueprintName())), cloudProvider.getPlatform() + " cluster request ");
//        given(cloudProvider.aValidStackRequest().withEnvironmentSettings(cloudProvider.getEnvironmentSettings()).withNetwork(cloudProvider.existingSubnet()),
//                " stack request ");
        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER), getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(groups = { "azure" })
    public void testCreateNewAzureCluster() throws Exception {
        given(CloudbreakClient.created());
        given(cloudProvider.aValidCredential());
        given(Cluster.request().withName(cloudProvider.getClusterName()).withAmbariRequest(cloudProvider
                        .ambariRequestWithBlueprintName(cloudProvider.getBlueprintName())), cloudProvider.getPlatform() + " cluster request ");
//        given(cloudProvider.aValidStackRequest().withEnvironmentSettings(cloudProvider.getEnvironmentSettings()).withNetwork(cloudProvider.existingSubnet()),
//                " stack request ");
        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER), getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    private void logCredentialCleanupFailure(ClientErrorException e) {
        try (Response response = e.getResponse()) {
            String exceptionMessage = response.readEntity(String.class);
            String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
            LOGGER.info("Delete credential exception message ::: " + errorMessage);
        } catch (ProcessingException | IllegalStateException e2) {
            LOGGER.info("Failed to log credential deletion exception message due to the following problem: {}", e2.getMessage());
        }
    }
}
