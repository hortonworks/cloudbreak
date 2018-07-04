package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.filesystem.GcsCloudStorageParameters;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.HiveRdsConfigForGcp;
import com.sequenceiq.it.cloudbreak.newway.LdapConfig;
import com.sequenceiq.it.cloudbreak.newway.RangerRdsConfigForGcp;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProvider;
import com.sequenceiq.it.cloudbreak.newway.cloud.CloudProviderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;

import static com.sequenceiq.it.cloudbreak.newway.cloud.GcpCloudProvider.GCP;
import static java.util.Collections.singletonList;

public class SharedServiceGcpTest extends SharedServiceTestRoot {

    public SharedServiceGcpTest() {
        this(LoggerFactory.getLogger(SharedServiceGcpTest.class), GCP);
    }

    private SharedServiceGcpTest(@Nonnull Logger logger, String implementation) {
        super(logger, implementation);
    }

    @Override
    @Test(dataProvider = "providerAndBlueprintAndClusterNameForDatalake")
    public void testADatalakeClusterCreation(CloudProvider cloudProvider, String clusterName, String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(RangerRdsConfigForGcp.isCreatedWithParameters(getTestParameter()));
        given(HiveRdsConfigForGcp.isCreatedWithParameters(getTestParameter()));
        given(LdapConfig.isCreatedWithParameters(getTestParameter()));
        given(Cluster.request()
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName))
                        .withCloudStorage(getGcpCloudStorageRequestForDatalake())
                        .withRdsConfigNames(new HashSet<>(Arrays.asList(getTestParameter().get("NN_GCP_DB_RANGER_CONFIG_NAME"),
                                getTestParameter().get("NN_GCP_DB_HIVE_CONFIG_NAME"))))
                        .withLdapConfigName(getTestParameter().get("NN_LDAP")),
                "a datalake cluster request");
        InstanceGroupV2Request hg = cloudProvider.hostgroup("master", InstanceGroupType.GATEWAY, 1);
        given(cloudProvider.aValidStackRequest().withInstanceGroups(singletonList(hg))
                .withName(clusterName));
        when(Stack.post());
        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(),
                "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Override
    @Test(dataProvider = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public void testClusterAttachedToDatalakeCluster(CloudProvider cloudProvider, String clusterName, String datalakeClusterName,
                String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(Cluster.request()
                        .withSharedService(datalakeClusterName)
                        .withAmbariRequest(cloudProvider.ambariRequestWithBlueprintName(blueprintName))
                        .withCloudStorage(getCloudStorageForAttachedCluster(
                                getTestParameter().get("NN_GCP_BUCKET_NAME"),
                                new GcsCloudStorageParameters()))
                        .withRdsConfigNames(new HashSet<>(Arrays.asList(
                                getTestParameter().get("NN_GCP_DB_RANGER_CONFIG_NAME"),
                                getTestParameter().get("NN_GCP_DB_HIVE_CONFIG_NAME"))))
                        .withLdapConfigName(getTestParameter().get("NN_LDAP")),
                "an attached cluster request");
        given(cloudProvider.aValidStackRequest().withName(clusterName));

        when(Stack.post());

        then(Stack.waitAndCheckClusterAndStackAvailabilityStatus(), "wait and check availability");
        then(Stack.checkClusterHasAmbariRunning(
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PORT),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_USER),
                getTestParameter().get(CloudProviderHelper.DEFAULT_AMBARI_PASSWORD)),
                "check ambari is running and components available");
    }

    @Override
    @Test(alwaysRun = true, dataProvider = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public void testTerminateAttachedCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName,
                String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(datalakeClusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Override
    @Test(alwaysRun = true, dataProvider = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public void testTerminateDatalakeCluster(CloudProvider cloudProvider, String attachedClusterName, String datalakeClusterName,
                String blueprintName) throws Exception {
        given(CloudbreakClient.isCreated());
        given(cloudProvider.aValidCredential());
        given(cloudProvider.aValidStackIsCreated()
                .withName(attachedClusterName), "a stack is created");
        when(Stack.delete());
        then(Stack.waitAndCheckClusterDeleted(), "stack has been deleted");
    }

    @Override
    @DataProvider(name = "providerAndBlueprintAndClusterNameForDatalake")
    public Object[][] providerClusterNameForDatalake() {
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(GCP, getTestParameter());
        return new Object[][]{
                {cloudProvider, getDatalakeClusterName(), getDatalakeBlueprintName()}
        };
    }

    @Override
    @DataProvider(name = "providerAndBlueprintAndClusterNameForAttachedCluster")
    public Object[][] providerClusterNameAndBlueprintForAttachedCluster() {
        CloudProvider cloudProvider = CloudProviderHelper.providerFactory(GCP, getTestParameter());
        return new Object[][]{
                {cloudProvider, getAttachedClusterName(), getDatalakeClusterName(), getAttachedClusterBlueprintName()}
        };
    }

    private CloudStorageRequest getGcpCloudStorageRequestForDatalake() {
        CloudStorageRequest request = new CloudStorageRequest();
        request.setGcs(new GcsCloudStorageParameters());
        request.setS3(null);
        request.setAdls(null);
        request.setWasb(null);
        request.setLocations(defaultDatalakeStorageLocations(getTestParameter().get("NN_GCP_BUCKET_NAME")));
        return request;
    }
}
