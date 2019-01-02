package com.sequenceiq.it.cloudbreak;

import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.AzureCloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;

public class AzureAdlsGen2ClusterTest extends CloudbreakTest {

    private static final String[] HOSTGROUPS = {"master"};

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    @Test(priority = 10)
    @Parameters({"clusterName"})
    public void testCreateClusterWithAdlsGen2(String clusterName) throws Exception {
        AzureCloudProvider azureCloudProvider = new AzureCloudProvider(getTestParameter());
        given(CloudbreakClient.created());
        given(azureCloudProvider.aValidCredential());
        given(azureCloudProvider.aValidClusterWithFs());
        given(azureCloudProvider.aValidStackRequest()
                .withName(clusterName), "a stack request");

        when(Stack.postV3(), "post the stack request");
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Test(priority = 20)
    @Parameters({"sshCommand", "sshChecker", "clusterName"})
    public void testTerasort(String sshCommand, String sshChecker, String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(Stack.request().withName(clusterName));
        when(Stack.get());
        then(Stack.checkSshCommand(HOSTGROUPS, defaultPrivateKeyFile, sshCommand, sshChecker), "check terasort is successful");
    }

    @Test(priority = 30)
    @Parameters({"clusterName"})
    public void cleanUpFs(String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(Stack.request().withName(clusterName));
        when(Stack.get());
        then(Stack.checkSshCommand(HOSTGROUPS, defaultPrivateKeyFile, "hdfs dfs -rm -R abfs://e2econtainer@cloudbreakabfs.dfs.core.windows.net/tera",
                "notContains:error"), "cleanup the master node");
    }

    @Test(priority = 40)
    @Parameters({"clusterName"})
    public void testTerminateCluster(String clusterName) throws Exception {
        given(CloudbreakClient.created());
        given(Stack.request().withName(clusterName));
        when(Stack.get());
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }
}